package by.lobanov.cardmanagementservice.controller;

import by.lobanov.cardmanagementservice.model.dto.request.LoginRequest;
import by.lobanov.cardmanagementservice.model.dto.request.RegisterRequest;
import by.lobanov.cardmanagementservice.model.dto.request.TokenRefreshRequest;
import by.lobanov.cardmanagementservice.model.dto.response.JwtResponse;
import by.lobanov.cardmanagementservice.model.dto.response.TokenRefreshResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication API", description = "API для аутентификации пользователей")
public interface AuthController {

    @PostMapping("/login")
    @Operation(summary = "Аутентификация пользователя", description = "Принимает email и пароль, возвращает JWT токен при успехе.")
    @ApiResponse(responseCode = "200", description = "Успешная аутентификация",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = JwtResponse.class)))
    @ApiResponse(responseCode = "400", description = "Невалидные входные данные")
    @ApiResponse(responseCode = "401", description = "Неверные учетные данные")
    public ResponseEntity<JwtResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest);

    @PostMapping("/refresh")
    @Operation(summary = "Обновить Access Token", description = "Принимает валидный Refresh Token и возвращает новую пару Access и Refresh токенов.")
    @ApiResponse(responseCode = "200", description = "Токены успешно обновлены", content = @Content(schema = @Schema(implementation = TokenRefreshResponse.class)))
    @ApiResponse(responseCode = "400", description = "Неверный запрос (нет токена)")
    @ApiResponse(responseCode = "403", description = "Ошибка Refresh Token (истек, не найден, отозван)")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody TokenRefreshRequest request);

    @PostMapping("/logout")
    @Operation(summary = "Выход пользователя", description = "Удаляет Refresh Token пользователя, делая текущую сессию невалидной для обновления.")
    @ApiResponse(responseCode = "200", description = "Выход выполнен успешно")
    @ApiResponse(responseCode = "401", description = "Неавторизован (требуется Access Token для выхода)") // Пользователь должен быть аутентифицирован для выхода
    @SecurityRequirement(name = "bearerAuth") // Требует Access Token
    public ResponseEntity<?> logoutUser(Authentication authentication);

    @PostMapping("/register")
    @Operation(summary = "Регистрация нового пользователя", description = "Создает нового пользователя с ролью USER.")
    @ApiResponse(responseCode = "201", description = "Пользователь успешно зарегистрирован")
    @ApiResponse(responseCode = "400", description = "Неверный запрос (ошибка валидации или email уже занят)")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest);
}
