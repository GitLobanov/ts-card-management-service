package by.lobanov.cardmanagementservice.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.UUID;

@Data
@Schema(description = "Запрос на создание новой банковской карты (только Администратор)")
public class CreateCardRequest {

    @NotNull(message = "ID пользователя не может быть пустым")
    @Schema(description = "ID пользователя, для которого создается карта", example = "c1c299bf-8b3f-4ed6-a161-ff25dccb3d61")
    private UUID userId;

    @NotBlank(message = "Срок действия не может быть пустым")
    @Pattern(regexp = "^(0[1-9]|1[0-2])/([0-9]{2})$", message = "Неверный формат срока действия (ожидается MM/YY)")
    // TODO: Добавить валидацию, что дата не в прошлом
    @Schema(description = "Срок действия карты (MM/YY)", example = "12/25")
    private String expiryDate;
}