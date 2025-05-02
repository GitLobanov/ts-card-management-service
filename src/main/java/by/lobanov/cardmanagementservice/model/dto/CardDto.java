package by.lobanov.cardmanagementservice.model.dto;

import by.lobanov.cardmanagementservice.model.constant.CardStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Schema(description = "DTO для отображения информации о банковской карте")
public class CardDto {

    @Schema(description = "Уникальный идентификатор карты")
    private UUID id;

    @Schema(description = "Маскированный номер карты", example = "**** **** **** 1234")
    private String maskedCardNumber;

    @Schema(description = "Email владельца карты", example = "user@example.com")
    private String ownerEmail;

    @Schema(description = "Срок действия карты (MM/YY)", example = "12/25")
    private String expiryDate;

    @Schema(description = "Статус карты (ACTIVE, BLOCKED, EXPIRED)", example = "ACTIVE")
    private CardStatus status;

    @Schema(description = "Текущий баланс карты", example = "1000.50")
    private BigDecimal balance;
}
