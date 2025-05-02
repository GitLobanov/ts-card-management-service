package by.lobanov.cardmanagementservice.controller.impl;

import by.lobanov.cardmanagementservice.controller.AdminCardController;
import by.lobanov.cardmanagementservice.model.constant.CardStatus;
import by.lobanov.cardmanagementservice.model.dto.CardDto;
import by.lobanov.cardmanagementservice.model.dto.request.CreateCardRequest;
import by.lobanov.cardmanagementservice.model.dto.request.UpdateCardStatusRequest;
import by.lobanov.cardmanagementservice.model.dto.response.PagedResponse;
import by.lobanov.cardmanagementservice.service.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AdminCardControllerImpl implements AdminCardController {

    private final CardService cardService;

    @Override
    public ResponseEntity<CardDto> createCard(CreateCardRequest request) {
        CardDto createdCard = cardService.createCard(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCard);
    }

    @Override
    public ResponseEntity<PagedResponse<CardDto>> getAllCardsFiltered(
            CardStatus status,
            String ownerEmail,
            BigDecimal minBalance,
            BigDecimal maxBalance,
            Pageable pageable) {

        Page<CardDto> cardsPage = cardService.getAllCardsFiltered(status, ownerEmail, minBalance, maxBalance, pageable);
        PagedResponse<CardDto> response = PagedResponse.fromPage(cardsPage);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<CardDto> getCardById(UUID id) {
        CardDto card = cardService.getCardByIdAsAdmin(id);
        return ResponseEntity.ok(card);
    }

    @Override
    public ResponseEntity<CardDto> updateCardStatus(UUID id, UpdateCardStatusRequest request) {
        if (request.getStatus() == CardStatus.EXPIRED) {
            return ResponseEntity.badRequest().build();
        }
        CardDto updatedCard = cardService.updateCardStatusAsAdmin(id, request.getStatus());
        return ResponseEntity.ok(updatedCard);
    }

    @Override
    public ResponseEntity<Void> deleteCard(UUID id) {
        cardService.deleteCardAsAdmin(id);
        return ResponseEntity.noContent().build();
    }
}
