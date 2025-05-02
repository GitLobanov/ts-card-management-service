package by.lobanov.cardmanagementservice.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
@Schema(description = "Запрос на обновление пользователя (только Администратор)")
public class UpdateUserRequest {

    @Size(min = 6, max = 100, message = "Пароль должен содержать от 6 до 100 символов")
    @Schema(description = "Новый пароль пользователя (опционально, минимум 6 символов). Если не указан, пароль не меняется.",
            example = "newsecurepass456", nullable = true)
    private String password;

    @NotEmpty(message = "Должна быть указана хотя бы одна роль")
    @Schema(description = "Новый набор ролей для пользователя (заменяет старый набор)",
            example = "[\"ROLE_USER\", \"ROLE_ADMIN\"]")
    private Set<String> roles;
}
