package com.cureforoptimism.nftservice.domain.nft;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseNft {
  @Id
  @Getter
  @Setter
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @Setter public Long tokenId;
  public String name;

  @Lob public String image;

  @Getter
  @OneToMany(mappedBy = "baseNft")
  public List<Attribute> attributes;

  @ManyToOne
  @Getter
  @Setter
  @JoinColumn(name = "contract_id")
  private Token token;
}
