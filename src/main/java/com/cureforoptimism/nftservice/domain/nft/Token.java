package com.cureforoptimism.nftservice.domain.nft;

import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Token {
  @Id @Getter public String contractId;

  // See {IERC721Metadata-name}.
  public String name;

  // TODO: Add chain enum
  Chain chain;

  @Getter
  @Setter
  @OneToMany(
      mappedBy = "id",
      cascade = {CascadeType.REMOVE, CascadeType.ALL})
  Set<BaseNft> baseNfts;
}
