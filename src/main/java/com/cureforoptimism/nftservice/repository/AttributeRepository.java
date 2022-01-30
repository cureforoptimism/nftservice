package com.cureforoptimism.nftservice.repository;

import com.cureforoptimism.nftservice.domain.nft.Attribute;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttributeRepository extends JpaRepository<Attribute, Long> {}
