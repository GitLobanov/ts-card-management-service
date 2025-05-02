package by.lobanov.cardmanagementservice.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO для отображения баланса")
public class BalanceDto {

    @Schema(description = "Баланс", example = "1500.75")
    private BigDecimal balance;
}
