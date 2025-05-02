package by.lobanov.cardmanagementservice.controller.impl;

import by.lobanov.cardmanagementservice.controller.ClientCardController;
import by.lobanov.cardmanagementservice.model.constant.CardStatus;
import by.lobanov.cardmanagementservice.model.dto.BalanceDto;
import by.lobanov.cardmanagementservice.model.dto.CardDto;
import by.lobanov.cardmanagementservice.model.dto.request.TransferRequest;
import by.lobanov.cardmanagementservice.model.dto.response.PagedResponse;
import by.lobanov.cardmanagementservice.service.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ClientCardControllerImpl implements ClientCardController {

    private final CardService cardService;

    @Override
    public ResponseEntity<PagedResponse<CardDto>> getCurrentUserCardsFiltered(
            @RequestParam(required = false) CardStatus status,
            @RequestParam(required = false) BigDecimal minBalance,
            @RequestParam(required = false) BigDecimal maxBalance,
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {

        Page<CardDto> cardsPage = cardService.getCurrentUserCardsFiltered(status, minBalance, maxBalance, pageable);
        PagedResponse<CardDto> response = PagedResponse.fromPage(cardsPage);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<CardDto> getCurrentUserCardById(@PathVariable UUID id) {
        CardDto card = cardService.getCurrentUserCardById(id);
        return ResponseEntity.ok(card);
    }

    @Override
    public ResponseEntity<CardDto> requestBlockCard(@PathVariable UUID id) {
        CardDto updatedCard = cardService.requestBlockCard(id);
        return ResponseEntity.ok(updatedCard);
    }

    @Override
    public ResponseEntity<Void> transferFunds(@Valid @RequestBody TransferRequest request) {
        cardService.transferFunds(request);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<BalanceDto> getCardBalance(@PathVariable UUID id) {
        BalanceDto balance = cardService.getCurrentUserCardBalance(id);
        return ResponseEntity.ok(balance);
    }

    @Override
    public ResponseEntity<BalanceDto> getTotalBalance() {
        BalanceDto totalBalance = cardService.getCurrentUserTotalBalance();
        return ResponseEntity.ok(totalBalance);
    }
}