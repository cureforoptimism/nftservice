package com.cureforoptimism.nftservice.repository;

import com.cureforoptimism.nftservice.domain.nft.Token;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TokenRepository extends JpaRepository<Token, String> {
  Token findByContractId(String contractId);
}
