package com.cureforoptimism.nftservice.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ImageCacheService {
  public Optional<byte[]> getImageBytes(String contractAddress, Long tokenId, String imageRef) {
    final Path path = Paths.get("img_cache", contractAddress, tokenId + ".png");
    if (path.toFile().exists()) {
      // Read
      try {
        ByteArrayInputStream bytes = new ByteArrayInputStream(Files.readAllBytes(path));
        return Optional.of(bytes.readAllBytes());
      } catch (IOException e) {
        e.printStackTrace();
      }
    } else {
      // TODO: Don't always construct a new HttpClient
      HttpClient httpClient = HttpClient.newHttpClient();
      HttpRequest request;

      try {
        request = HttpRequest.newBuilder().GET().uri(new URI(imageRef)).build();
      } catch (URISyntaxException ex) {
        log.error("Invalid URI for HTTP request: " + imageRef, ex);
        return Optional.empty();
      }

      try {
        for (int retry = 0; retry <= 5; retry++) {
          final var response = httpClient.send(request, BodyHandlers.ofByteArray());
          if (response.statusCode() == 200) {
            // Write to cache
            log.info("Writing new cached object: " + path + "; try: " + (retry + 1));

            Files.createDirectories(Paths.get("img_cache", contractAddress));
            Files.write(path, response.body());

            return Optional.of(response.body());
          } else {
            Thread.sleep(250);
            log.error("Unable to retrieve image (will retry): " + response.statusCode());
          }
        }
      } catch (IOException | InterruptedException ex) {
        log.warn("Unable to retrieve image: " + imageRef, ex);
      }
    }

    return Optional.empty();
  }
}
