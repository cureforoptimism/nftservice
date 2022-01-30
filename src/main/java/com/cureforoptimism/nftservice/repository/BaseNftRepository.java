package com.cureforoptimism.nftservice.repository;

import com.cureforoptimism.nftservice.domain.nft.BaseNft;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BaseNftRepository extends JpaRepository<BaseNft, Long> {
  boolean existsByTokenIdAndToken_ContractId(Long tokenId, String contractId);

  Set<BaseNft> findByToken_ContractId(String contractId);
}
