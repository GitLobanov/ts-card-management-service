package by.lobanov.cardmanagementservice.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Set;
import java.util.UUID;

@Data
@Schema(description = "DTO для отображения информации о пользователе")
public class UserDto {

    @Schema(description = "Уникальный идентификатор пользователя", example = "c1c299bf-8b3f-4ed6-a161-ff25dccb3d61")
    private UUID id;

    @Schema(description = "Email пользователя", example = "user@example.com")
    private String email;

    @Schema(description = "Набор ролей пользователя (строковое представление)", example = "[\"ROLE_USER\", \"ROLE_ADMIN\"]")
    private Set<String> roles; // Используем String для простоты в DTO
}