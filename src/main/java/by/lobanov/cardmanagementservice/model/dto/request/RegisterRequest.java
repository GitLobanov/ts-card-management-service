package by.lobanov.cardmanagementservice.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Запрос на регистрацию нового пользователя")
public class RegisterRequest {

    @NotBlank(message = "Email не может быть пустым")
    @Email(message = "Неверный формат email")
    @Schema(description = "Email нового пользователя", example = "newuser@example.com")
    private String email;

    @NotBlank(message = "Пароль не может быть пустым")
    @Size(min = 6, max = 100, message = "Пароль должен содержать от 6 до 100 символов")
    @Schema(description = "Пароль нового пользователя (минимум 6 символов)", example = "strongpassword")
    private String password;
}
