package by.lobanov.cardmanagementservice.controller;

import by.lobanov.cardmanagementservice.model.constant.CardStatus;
import by.lobanov.cardmanagementservice.model.dto.CardDto;
import by.lobanov.cardmanagementservice.model.dto.request.CreateCardRequest;
import by.lobanov.cardmanagementservice.model.dto.request.UpdateCardStatusRequest;
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
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.UUID;

@RequestMapping("/api/admin/cards")
@Tag(name = "Admin Card Management API", description = "API для управления картами (только Администратор)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public interface AdminCardController {

    @Operation(summary = "Создать новую карту",
            description = "Создает карту для указанного пользователя. Номер карты генерируется автоматически и валидируется по алгоритму Луна.")
    @ApiResponse(responseCode = "201", description = "Карта успешно создана", content = @Content(schema = @Schema(implementation = CardDto.class)))
    @ApiResponse(responseCode = "400", description = "Неверный запрос (ошибка валидации или пользователь не найден)")
    @ApiResponse(responseCode = "401", description = "Неавторизован")
    @ApiResponse(responseCode = "403", description = "Доступ запрещен (не админ)")
    @PostMapping
    public ResponseEntity<CardDto> createCard(@Valid @RequestBody CreateCardRequest request);

    @Operation(summary = "Получить все карты (с фильтрацией)", description = "Возвращает постраничный список всех карт в системе с возможностью фильтрации.")
    @Parameters({
            @Parameter(name = "page", description = "Номер страницы (начиная с 0)", in = ParameterIn.QUERY, schema = @Schema(type = "integer", defaultValue = "0")),
            @Parameter(name = "size", description = "Количество элементов на странице", in = ParameterIn.QUERY, schema = @Schema(type = "integer", defaultValue = "10")),
            @Parameter(name = "sort", description = "Сортировка (например, 'balance,desc')", in = ParameterIn.QUERY, schema = @Schema(type = "string")),
            @Parameter(name = "status", description = "Фильтр по статусу карты (ACTIVE, BLOCKED, EXPIRED)", in = ParameterIn.QUERY, schema = @Schema(implementation = CardStatus.class)),
            @Parameter(name = "ownerEmail", description = "Фильтр по email владельца", in = ParameterIn.QUERY, schema = @Schema(type = "string")),
            @Parameter(name = "minBalance", description = "Фильтр по минимальному балансу", in = ParameterIn.QUERY, schema = @Schema(type = "number", format = "double")),
            @Parameter(name = "maxBalance", description = "Фильтр по максимальному балансу", in = ParameterIn.QUERY, schema = @Schema(type = "number", format = "double"))
    })
    @ApiResponse(responseCode = "200", description = "Список карт получен")
    @GetMapping
    public ResponseEntity<PagedResponse<CardDto>> getAllCardsFiltered(
            @RequestParam(required = false) CardStatus status,
            @RequestParam(required = false) String ownerEmail,
            @RequestParam(required = false) BigDecimal minBalance,
            @RequestParam(required = false) BigDecimal maxBalance,
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable);

    @GetMapping("/{id}")
    @Operation(summary = "Получить карту по ID", description = "Возвращает детали конкретной карты.")
    @ApiResponse(responseCode = "200", description = "Карта найдена", content = @Content(schema = @Schema(implementation = CardDto.class)))
    @ApiResponse(responseCode = "401", description = "Неавторизован")
    @ApiResponse(responseCode = "403", description = "Доступ запрещен")
    @ApiResponse(responseCode = "404", description = "Карта не найдена")
    public ResponseEntity<CardDto> getCardById(@PathVariable UUID id);

    @PutMapping("/{id}/status")
    @Operation(summary = "Обновить статус карты", description = "Активирует или блокирует карту. Нельзя установить статус EXPIRED.")
    @ApiResponse(responseCode = "200", description = "Статус карты обновлен", content = @Content(schema = @Schema(implementation = CardDto.class)))
    @ApiResponse(responseCode = "400", description = "Неверный запрос (недопустимый статус или карта истекла)")
    @ApiResponse(responseCode = "401", description = "Неавторизован")
    @ApiResponse(responseCode = "403", description = "Доступ запрещен")
    @ApiResponse(responseCode = "404", description = "Карта не найдена")
    @ApiResponse(responseCode = "409", description = "Конфликт операции (например, попытка активировать истекшую карту)")
    public ResponseEntity<CardDto> updateCardStatus(@PathVariable UUID id, @Valid @RequestBody UpdateCardStatusRequest request);

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить карту", description = "Безвозвратно удаляет карту из системы.")
    @ApiResponse(responseCode = "204", description = "Карта успешно удалена")
    @ApiResponse(responseCode = "401", description = "Неавторизован")
    @ApiResponse(responseCode = "403", description = "Доступ запрещен")
    @ApiResponse(responseCode = "404", description = "Карта не найдена")
    public ResponseEntity<Void> deleteCard(@PathVariable UUID id);
}
