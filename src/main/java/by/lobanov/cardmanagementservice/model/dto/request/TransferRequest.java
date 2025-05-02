package by.lobanov.cardmanagementservice.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Schema(description = "Запрос на перевод средств между картами одного пользователя")
public class TransferRequest {

    @NotNull(message = "ID карты-источника не может быть пустым")
    @Schema(description = "ID карты, с которой осуществляется перевод", example = "c1c299bf-8b3f-4ed6-a161-ff25dccb3d61")
    private UUID fromCardId;

    @NotNull(message = "ID карты-получателя не может быть пустым")
    @Schema(description = "ID карты, на которую осуществляется перевод", example = "c1c299bf-8b3f-4ed6-a161-ff25dccb3d62")
    private UUID toCardId;

    @NotNull(message = "Сумма перевода не может быть пустой")
    @DecimalMin(value = "0.01", message = "Сумма перевода должна быть положительной")
    @Schema(description = "Сумма перевода", example = "100.00")
    private BigDecimal amount;
}
