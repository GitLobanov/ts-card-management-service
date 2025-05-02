package by.lobanov.cardmanagementservice.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
@Schema(description = "Запрос на создание нового пользователя (только Администратор)")
public class CreateUserRequest {

    @NotBlank(message = "Email не может быть пустым")
    @Email(message = "Неверный формат email")
    @Schema(description = "Email нового пользователя", example = "newadmin@example.com")
    private String email;

    @NotBlank(message = "Пароль не может быть пустым")
    @Size(min = 6, max = 100, message = "Пароль должен содержать от 6 до 100 символов")
    @Schema(description = "Пароль нового пользователя (минимум 6 символов)", example = "securepass123")
    private String password;

    @NotEmpty(message = "Должна быть указана хотя бы одна роль")
    @Schema(description = "Набор ролей для пользователя (например, [\"ROLE_USER\"] или [\"ROLE_USER\", \"ROLE_ADMIN\"])",
            example = "[\"ROLE_USER\"]")
    private Set<String> roles; // Администратор указывает роли при создании
}
