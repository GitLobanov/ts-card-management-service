package by.lobanov.cardmanagementservice.controller;

import by.lobanov.cardmanagementservice.model.dto.UserDto;
import by.lobanov.cardmanagementservice.model.dto.request.CreateUserRequest;
import by.lobanov.cardmanagementservice.model.dto.request.UpdateUserRequest;
import by.lobanov.cardmanagementservice.model.dto.response.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

@Tag(name = "Admin User Management API", description = "API для управления пользователями (только Администратор)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/admin/users")
public interface AdminUserController {

    @GetMapping
    @Operation(summary = "Получить всех пользователей", description = "Возвращает постраничный список всех пользователей.")
    @Parameters({
            @Parameter(name = "page", description = "Номер страницы (начиная с 0)", in = ParameterIn.QUERY, schema = @Schema(type = "integer", defaultValue = "0")),
            @Parameter(name = "size", description = "Количество элементов на странице", in = ParameterIn.QUERY, schema = @Schema(type = "integer", defaultValue = "10")),
            @Parameter(name = "sort", description = "Сортировка (например, 'email,asc')", in = ParameterIn.QUERY, schema = @Schema(type = "string", defaultValue = "id,asc"))
    })
    @ApiResponse(responseCode = "200", description = "Список пользователей получен",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PagedResponse.class))) // Используем PagedResponse<UserDto>
    @ApiResponse(responseCode = "401", description = "Неавторизован")
    @ApiResponse(responseCode = "403", description = "Доступ запрещен (не админ)")
    ResponseEntity<PagedResponse<UserDto>> getAllUsers(@ParameterObject Pageable pageable);

    @GetMapping("/{id}")
    @Operation(summary = "Получить пользователя по ID", description = "Возвращает детали конкретного пользователя.")
    @ApiResponse(responseCode = "200", description = "Пользователь найден", content = @Content(schema = @Schema(implementation = UserDto.class)))
    @ApiResponse(responseCode = "401", description = "Неавторизован")
    @ApiResponse(responseCode = "403", description = "Доступ запрещен")
    @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    ResponseEntity<UserDto> getUserById(@PathVariable UUID id);

    @PostMapping
    @Operation(summary = "Создать нового пользователя", description = "Создает пользователя с указанными email, паролем и ролями.")
    @ApiResponse(responseCode = "201", description = "Пользователь успешно создан", content = @Content(schema = @Schema(implementation = UserDto.class)))
    @ApiResponse(responseCode = "400", description = "Неверный запрос (ошибка валидации, email занят, роль не найдена)")
    @ApiResponse(responseCode = "401", description = "Неавторизован")
    @ApiResponse(responseCode = "403", description = "Доступ запрещен")
    ResponseEntity<UserDto> createUser(@Valid @RequestBody CreateUserRequest request);

    @PutMapping("/{id}")
    @Operation(summary = "Обновить пользователя", description = "Обновляет пароль (если передан) и/или роли пользователя.")
    @ApiResponse(responseCode = "200", description = "Пользователь успешно обновлен", content = @Content(schema = @Schema(implementation = UserDto.class)))
    @ApiResponse(responseCode = "400", description = "Неверный запрос (ошибка валидации, роль не найдена)")
    @ApiResponse(responseCode = "401", description = "Неавторизован")
    @ApiResponse(responseCode = "403", description = "Доступ запрещен")
    @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    ResponseEntity<UserDto> updateUser(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request);

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить пользователя", description = "Удаляет пользователя из системы. Нельзя удалить себя или защищенных пользователей.")
    @ApiResponse(responseCode = "204", description = "Пользователь успешно удален")
    @ApiResponse(responseCode = "400", description = "Нельзя удалить себя или защищенного пользователя")
    @ApiResponse(responseCode = "401", description = "Неавторизован")
    @ApiResponse(responseCode = "403", description = "Доступ запрещен")
    @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    ResponseEntity<Void> deleteUser(@PathVariable UUID id);
}
