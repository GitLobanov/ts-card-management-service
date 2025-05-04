package by.lobanov.cardmanagementservice.mapper;

import by.lobanov.cardmanagementservice.model.dto.CardDto;
import by.lobanov.cardmanagementservice.model.entity.Card;
import by.lobanov.cardmanagementservice.util.CardMaskingUtil;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING,
        imports = {CardMaskingUtil.class})
public interface CardMapper {

    @Mapping(source = "ownerEmail", target = "owner.email")
    @Mapping(target = "cardNumber", ignore = true)
    Card toEntity(CardDto cardDto);

    @Mapping(source = "owner.email", target = "ownerEmail")
    @Mapping(target = "maskedCardNumber",
        expression = "java(CardMaskingUtil.maskCardNumber(card.getCardNumber()))")
    CardDto toDto(Card card);
}