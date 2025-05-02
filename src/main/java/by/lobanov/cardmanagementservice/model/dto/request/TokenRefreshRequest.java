package by.lobanov.cardmanagementservice.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TokenRefreshRequest {
    @NotBlank(message = "Refresh token не может быть пустым")
    private String refreshToken;
}
