package com.cureforoptimism.nftservice.controller;

import com.cureforoptimism.nftservice.domain.dto.NftDto;
import com.cureforoptimism.nftservice.domain.mapper.NftMapper;
import com.cureforoptimism.nftservice.domain.nft.Attribute;
import com.cureforoptimism.nftservice.domain.nft.BaseNft;
import com.cureforoptimism.nftservice.domain.nft.Chain;
import com.cureforoptimism.nftservice.domain.nft.Token;
import com.cureforoptimism.nftservice.repository.BaseNftRepository;
import com.cureforoptimism.nftservice.repository.TokenRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.ws.rs.core.MediaType;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.web3j.contracts.eip721.generated.ERC721;
import org.web3j.contracts.eip721.generated.ERC721Enumerable;
import org.web3j.contracts.eip721.generated.ERC721Metadata;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Keys;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.DefaultGasProvider;

@RestController
@RequestMapping("/api/nft")
@AllArgsConstructor
@Slf4j
public class NftToken {
  private final TokenRepository tokenRepository;
  private final BaseNftRepository baseNftRepository;

  @RequestMapping(method = RequestMethod.POST)
  @ResponseStatus(HttpStatus.OK)
  public void addToken(@RequestParam("contractId") final String contractId) {
    CompletableFuture.runAsync(() -> initializeFromContract(contractId));
  }

  @RequestMapping(method = RequestMethod.GET)
  public ResponseEntity<Set<Token>> getToken() {
    Set<Token> tokens = new HashSet<>(tokenRepository.findAll());

    return ResponseEntity.ok(tokens);
  }

  @GetMapping("/{id}")
  public ResponseEntity<Set<NftDto>> getTokensByContractAddress(
      @PathVariable("id") String contractAddress) {

    final var mapper = Mappers.getMapper(NftMapper.class);

    final var tokens =
        baseNftRepository.findByToken_ContractId(contractAddress).stream()
            .map(mapper::toDto)
            .collect(Collectors.toSet());

    return ResponseEntity.ok(tokens);
  }

  @Async
  public CompletableFuture<Void> initializeFromContract(String contractId) {
    Web3j web3j = Web3j.build(new HttpService("https://arb1.arbitrum.io/rpc"));

    ERC721 erc721 = null;
    ERC721Enumerable erc721Enumerable;
    ERC721Metadata erc721Metadata;
    HttpClient httpClient = HttpClient.newHttpClient();
    final ObjectMapper objMapper = new ObjectMapper();
    ContractGasProvider contractGasProvider = new DefaultGasProvider();
    try {
      Credentials dummyCredentials = Credentials.create(Keys.createEcKeyPair());
      erc721 = ERC721.load(contractId, web3j, dummyCredentials, contractGasProvider);

      erc721Enumerable =
          ERC721Enumerable.load(contractId, web3j, dummyCredentials, contractGasProvider);

      erc721Metadata =
          ERC721Metadata.load(contractId, web3j, dummyCredentials, contractGasProvider);
    } catch (InvalidAlgorithmParameterException
        | NoSuchAlgorithmException
        | NoSuchProviderException ex) {
      log.error("unable to create dummy credentials", ex);
      return CompletableFuture.completedFuture(null);
    }

    BigInteger totalSupply;
    String contractName;
    try {
      totalSupply = erc721Enumerable.totalSupply().send();
      contractName = erc721Metadata.name().send();

      log.info("(" + contractName + ") - Total supply: " + totalSupply);
    } catch (Exception ex) {
      log.error("Unable to retrieve totalSupply/contactName", ex);
      return CompletableFuture.completedFuture(null);
    }

    Token token =
        tokenRepository.save(
            Token.builder()
                .name(contractName)
                .contractId(contractId)
                .chain(Chain.ARBITRUM)
                .build());

    Base64.Decoder decoder = Base64.getDecoder();

    boolean isBase64 = false;
    List<Integer> missingTokenId = new ArrayList<>();

    for (int x = 0; x < totalSupply.intValue(); x++) {
      if (baseNftRepository.existsByTokenIdAndToken_ContractId((long) x, contractId)) {
        // TODO: We may want to force metadata refreshes via an arg at some point...
        continue;
      }

      String tokenUriValue;

      try {
        tokenUriValue = erc721Metadata.tokenURI(BigInteger.valueOf(x)).send();
      } catch (Exception ex) {
        // Bail; record what we've got
        log.info("(" + contractName + ") Unable to retrieve tokenURI for " + x, ex);
        missingTokenId.add(x);
        continue;
      }

      if (tokenUriValue.startsWith("data:")) {
        // Trim data:
        tokenUriValue = tokenUriValue.substring(5);

        if (tokenUriValue.startsWith(MediaType.APPLICATION_JSON)) {
          tokenUriValue = tokenUriValue.substring(tokenUriValue.indexOf(";") + 1);
        } else {
          log.error("Unsupported data encoding in tokenURI");
          return CompletableFuture.completedFuture(null);
        }

        // Handle encoding (assume media type is stripped)
        if (tokenUriValue.startsWith("base64")) {
          tokenUriValue = tokenUriValue.substring(tokenUriValue.indexOf(",") + 1);

          isBase64 = true;
        }
      }

      if (isBase64) {
        tokenUriValue = new String(decoder.decode(tokenUriValue));
      } else {
        try {
          HttpRequest request = HttpRequest.newBuilder().GET().uri(new URI(tokenUriValue)).build();

          tokenUriValue = httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
        } catch (URISyntaxException | InterruptedException | IOException ex) {
          log.error("Exception on URI: " + tokenUriValue, ex);
          missingTokenId.add(x);
          continue;
        }
      }

      // Transform JSON
      BaseNft baseNft;
      try {
        baseNft =
            objMapper.readValue(tokenUriValue.getBytes(StandardCharsets.UTF_8), BaseNft.class);
      } catch (IOException ex) {
        log.error("Unable to parse JSON", ex);
        missingTokenId.add(x);
        continue;
      }

      for (Attribute attribute : baseNft.getAttributes()) {
        attribute.setBaseNft(baseNft);
      }

      baseNft.setTokenId(Integer.toUnsignedLong(x));
      baseNft.setToken(token);

      baseNftRepository.save(baseNft);

      if (x % 100 == 0) {
        log.info("(" + contractName + ") - Initialized " + x + " / " + totalSupply);
      }
    }

    log.info("(" + contractName + ") Import complete.");
    if (!missingTokenId.isEmpty()) {
      log.info(
          "Missing tokens: "
              + missingTokenId.stream().map(Object::toString).collect(Collectors.joining(", ")));
    }

    return CompletableFuture.completedFuture(null);
  }
}
