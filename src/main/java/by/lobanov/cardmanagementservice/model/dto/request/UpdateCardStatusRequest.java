package by.lobanov.cardmanagementservice.model.dto.request;

import by.lobanov.cardmanagementservice.model.constant.CardStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Запрос на обновление статуса карты")
public class UpdateCardStatusRequest {

    @NotNull(message = "Статус не может быть пустым")
    @Schema(description = "Новый статус карты (ACTIVE, BLOCKED)", example = "BLOCKED")
    private CardStatus status;
}
