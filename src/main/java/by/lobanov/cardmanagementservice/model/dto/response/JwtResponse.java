package by.lobanov.cardmanagementservice.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponse {
    private String token;
    private String refreshToken;
    private String type = "Bearer";
    private String email;

    public JwtResponse(String accessToken, String refreshToken, String email) {
        this.token = accessToken;
        this.refreshToken = refreshToken;
        this.email = email;
    }
}
