package by.lobanov.cardmanagementservice.controller;

import by.lobanov.cardmanagementservice.model.constant.CardStatus;
import by.lobanov.cardmanagementservice.model.dto.BalanceDto;
import by.lobanov.cardmanagementservice.model.dto.CardDto;
import by.lobanov.cardmanagementservice.model.dto.request.TransferRequest;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/cards")
@Tag(name = "User Card API", description = "API для управления своими картами (только Пользователь)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('USER')")
public interface ClientCardController {

    @GetMapping
    @Operation(summary = "Получить свои карты (с фильтрацией)", description = "Возвращает постраничный список карт текущего пользователя с возможностью фильтрации.")
    @Parameters({
            @Parameter(name = "page", description = "Номер страницы (начиная с 0)", in = ParameterIn.QUERY, schema = @Schema(type = "integer", defaultValue = "0")),
            @Parameter(name = "size", description = "Количество элементов на странице", in = ParameterIn.QUERY, schema = @Schema(type = "integer", defaultValue = "10")),
            @Parameter(name = "sort", description = "Сортировка (например, 'balance,desc')", in = ParameterIn.QUERY, schema = @Schema(type = "string")),
            @Parameter(name = "status", description = "Фильтр по статусу карты (ACTIVE, BLOCKED, EXPIRED)", in = ParameterIn.QUERY, schema = @Schema(implementation = CardStatus.class)),
            @Parameter(name = "minBalance", description = "Фильтр по минимальному балансу", in = ParameterIn.QUERY, schema = @Schema(type = "number", format = "double")),
            @Parameter(name = "maxBalance", description = "Фильтр по максимальному балансу", in = ParameterIn.QUERY, schema = @Schema(type = "number", format = "double"))
    })
    @ApiResponse(responseCode = "200", description = "Список карт пользователя получен")
    public ResponseEntity<PagedResponse<CardDto>> getCurrentUserCardsFiltered(
            @RequestParam(required = false) CardStatus status,
            @RequestParam(required = false) BigDecimal minBalance,
            @RequestParam(required = false) BigDecimal maxBalance,
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable);

    @GetMapping("/{id}")
    @Operation(summary = "Получить свою карту по ID", description = "Возвращает детали конкретной карты, если она принадлежит текущему пользователю.")
    @ApiResponse(responseCode = "200", description = "Карта найдена", content = @Content(schema = @Schema(implementation = CardDto.class)))
    @ApiResponse(responseCode = "401", description = "Неавторизован")
    @ApiResponse(responseCode = "403", description = "Доступ запрещен")
    @ApiResponse(responseCode = "404", description = "Карта не найдена или не принадлежит пользователю")
    public ResponseEntity<CardDto> getCurrentUserCardById(@PathVariable UUID id);

    @PostMapping("/{id}/block-request")
    @Operation(summary = "Запросить блокировку карты", description = "Устанавливает статус BLOCKED для указанной карты пользователя.")
    @ApiResponse(responseCode = "200", description = "Карта успешно заблокирована", content = @Content(schema = @Schema(implementation = CardDto.class)))
    @ApiResponse(responseCode = "401", description = "Неавторизован")
    @ApiResponse(responseCode = "403", description = "Доступ запрещен")
    @ApiResponse(responseCode = "404", description = "Карта не найдена или не принадлежит пользователю")
    @ApiResponse(responseCode = "409", description = "Конфликт операции (карта не активна или уже заблокирована)")
    public ResponseEntity<CardDto> requestBlockCard(@PathVariable UUID id);

    @PostMapping("/transfer")
    @Operation(summary = "Перевести средства", description = "Выполняет перевод средств между двумя картами текущего пользователя.")
    @ApiResponse(responseCode = "200", description = "Перевод выполнен успешно") // Успешный перевод - 200 OK, т.к. нет возвращаемого ресурса
    @ApiResponse(responseCode = "400", description = "Неверный запрос (недостаточно средств, карты не активны, некорректные ID)")
    @ApiResponse(responseCode = "401", description = "Неавторизован")
    @ApiResponse(responseCode = "403", description = "Доступ запрещен")
    @ApiResponse(responseCode = "404", description = "Одна из карт не найдена или не принадлежит пользователю")
    @ApiResponse(responseCode = "409", description = "Конфликт операции (карты не активны)")
    public ResponseEntity<Void> transferFunds(@Valid @RequestBody TransferRequest request);

    @GetMapping("/{id}/balance")
    @Operation(summary = "Получить баланс карты", description = "Возвращает баланс конкретной карты пользователя.")
    @ApiResponse(responseCode = "200", description = "Баланс получен", content = @Content(schema = @Schema(implementation = BalanceDto.class)))
    @ApiResponse(responseCode = "401", description = "Неавторизован")
    @ApiResponse(responseCode = "403", description = "Доступ запрещен")
    @ApiResponse(responseCode = "404", description = "Карта не найдена или не принадлежит пользователю")
    public ResponseEntity<BalanceDto> getCardBalance(@PathVariable UUID id);

    @GetMapping("/balance")
    @Operation(summary = "Получить общий баланс", description = "Возвращает суммарный баланс по всем картам пользователя.")
    @ApiResponse(responseCode = "200", description = "Общий баланс получен", content = @Content(schema = @Schema(implementation = BalanceDto.class)))
    @ApiResponse(responseCode = "401", description = "Неавторизован")
    @ApiResponse(responseCode = "403", description = "Доступ запрещен")
    public ResponseEntity<BalanceDto> getTotalBalance();
}