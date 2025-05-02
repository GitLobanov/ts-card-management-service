package by.lobanov.cardmanagementservice.service;

import by.lobanov.cardmanagementservice.model.constant.CardStatus;
import by.lobanov.cardmanagementservice.model.dto.BalanceDto;
import by.lobanov.cardmanagementservice.model.dto.CardDto;
import by.lobanov.cardmanagementservice.model.dto.request.CreateCardRequest;
import by.lobanov.cardmanagementservice.model.dto.request.TransferRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.UUID;

public interface CardService {

    /**
     * Создает новую карту для указанного пользователя. (Только Админ)
     * @param request DTO с данными для создания карты.
     * @return DTO созданной карты.
     */
    CardDto createCard(CreateCardRequest request);

    /**
     * Получает карту по ID. (Только Админ)
     * @param id ID карты.
     * @return DTO карты.
     */
    CardDto getCardByIdAsAdmin(UUID id);

    /**
     * Обновляет статус карты (Активирует/Блокирует). (Только Админ)
     * @param id ID карты.
     * @param newStatus Новый статус (ACTIVE или BLOCKED).
     * @return DTO обновленной карты.
     */
    CardDto updateCardStatusAsAdmin(UUID id, CardStatus newStatus);

    /**
     * Удаляет карту. (Только Админ)
     * @param id ID карты для удаления.
     */
    void deleteCardAsAdmin(UUID id);

    /**
     * Получает карту текущего пользователя по ID.
     * @param id ID карты.
     * @return DTO карты.
     */
    CardDto getCurrentUserCardById(UUID id);

    /**
     * Запрашивает блокировку карты текущего пользователя.
     * @param id ID карты для блокировки.
     * @return DTO заблокированной карты.
     */
    CardDto requestBlockCard(UUID id);

    /**
     * Выполняет перевод средств между двумя картами текущего пользователя.
     * @param request DTO с деталями перевода.
     */
    void transferFunds(TransferRequest request);

    /**
     * Получает баланс конкретной карты текущего пользователя.
     * @param id ID карты.
     * @return DTO с балансом.
     */
    BalanceDto getCurrentUserCardBalance(UUID id);

    /**
     * Получает суммарный баланс по всем картам текущего пользователя.
     * @return DTO с общим балансом.
     */
    BalanceDto getCurrentUserTotalBalance();

    /**
     * Получает страницу всех карт в системе с пагинацией и фильтрацией. (Только Админ)
     * @param status Фильтр по статусу (null для отсутствия фильтра).
     * @param ownerEmail Фильтр по email владельца (null для отсутствия фильтра).
     * @param minBalance Фильтр по минимальному балансу (null для отсутствия фильтра).
     * @param maxBalance Фильтр по максимальному балансу (null для отсутствия фильтра).
     * @param pageable Параметры пагинации и сортировки.
     * @return Страница с DTO карт.
     */
    Page<CardDto> getAllCardsFiltered(CardStatus status, String ownerEmail, BigDecimal minBalance, BigDecimal maxBalance, Pageable pageable);

    /**
     * Получает страницу карт текущего пользователя с пагинацией и фильтрацией.
     * @param status Фильтр по статусу (null для отсутствия фильтра).
     * @param minBalance Фильтр по минимальному балансу (null для отсутствия фильтра).
     * @param maxBalance Фильтр по максимальному балансу (null для отсутствия фильтра).
     * @param pageable Параметры пагинации и сортировки.
     * @return Страница с DTO карт пользователя.
     */
    Page<CardDto> getCurrentUserCardsFiltered(CardStatus status, BigDecimal minBalance, BigDecimal maxBalance, Pageable pageable);
}
