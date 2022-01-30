package com.cureforoptimism.nftservice.domain.mapper;

import com.cureforoptimism.nftservice.domain.dto.NftDto;
import com.cureforoptimism.nftservice.domain.dto.TokenDto;
import com.cureforoptimism.nftservice.domain.nft.BaseNft;
import com.cureforoptimism.nftservice.domain.nft.Token;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NftMapper {
  NftDto toDto(BaseNft baseNft);

  TokenDto toDto(Token token);
}
