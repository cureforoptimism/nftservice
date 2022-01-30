package com.cureforoptimism.nftservice.domain.nft;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Attribute {
  @Id
  @Getter
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @JsonProperty("trait_type")
  private String traitType;

  public String value;

  @ManyToOne
  @Getter
  @Setter
  @JoinColumn(name = "base_nft_id")
  private BaseNft baseNft;
}
