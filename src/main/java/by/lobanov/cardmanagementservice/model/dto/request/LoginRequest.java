package by.lobanov.cardmanagementservice.model.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.ToString;

@Data
@ToString(exclude = {"password"})
public class LoginRequest {

    @NotBlank(message = "Email не может быть пустым")
    @Email(message = "Неверный формат email")
    private String email;

    @NotBlank(message = "Пароль не может быть пустым")
    private String password;
}
