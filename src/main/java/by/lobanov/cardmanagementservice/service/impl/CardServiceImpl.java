package by.lobanov.cardmanagementservice.service.impl;

import by.lobanov.cardmanagementservice.exception.BadRequestException;
import by.lobanov.cardmanagementservice.exception.CardOperationException;
import by.lobanov.cardmanagementservice.exception.InsufficientFundsException;
import by.lobanov.cardmanagementservice.exception.ResourceNotFoundException;
import by.lobanov.cardmanagementservice.model.constant.CardStatus;
import by.lobanov.cardmanagementservice.model.dto.BalanceDto;
import by.lobanov.cardmanagementservice.model.dto.CardDto;
import by.lobanov.cardmanagementservice.model.dto.request.CreateCardRequest;
import by.lobanov.cardmanagementservice.model.dto.request.TransferRequest;
import by.lobanov.cardmanagementservice.model.entity.Card;
import by.lobanov.cardmanagementservice.model.entity.User;
import by.lobanov.cardmanagementservice.repository.CardRepository;
import by.lobanov.cardmanagementservice.repository.UserRepository;
import by.lobanov.cardmanagementservice.repository.specification.CardSpecification;
import by.lobanov.cardmanagementservice.service.CardNumberGeneratorService;
import by.lobanov.cardmanagementservice.service.CardService;
import by.lobanov.cardmanagementservice.util.AuthenticationHelper;
import by.lobanov.cardmanagementservice.util.CardMaskingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static by.lobanov.cardmanagementservice.util.ServiceMessagesUtil.ADMIN_CANNOT_MANUALLY_SET_STATUS_EXPIRED;
import static by.lobanov.cardmanagementservice.util.ServiceMessagesUtil.CANNOT_ACTIVATE_EXPIRED_CARD_ID;
import static by.lobanov.cardmanagementservice.util.ServiceMessagesUtil.CARD_NOT_FOUND;

@Service
@Slf4j
@RequiredArgsConstructor
public class CardServiceImpl implements CardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final AuthenticationHelper authenticationHelper;
    private final CardNumberGeneratorService cardNumberGeneratorService;

    @Override
    @Transactional
    public CardDto createCard(CreateCardRequest request) {
        log.info("Admin request to create card for user ID: {}", request.getUserId());
        User owner = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));

        Card card = new Card();
        card.setOwner(owner);
        String generatedCardNumber = cardNumberGeneratorService.generateUniqueCardNumber();
        card.setCardNumber(generatedCardNumber);
        card.setExpiryDate(request.getExpiryDate());
        card.setStatus(CardStatus.ACTIVE);
        card.setBalance(BigDecimal.ZERO);
        Card savedCard = cardRepository.save(card);
        log.info("Card created successfully with ID: {} for user ID: {}", savedCard.getId(), owner.getId());
        return mapToCardDto(savedCard);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CardDto> getAllCardsFiltered(CardStatus status, String ownerEmail, BigDecimal minBalance, BigDecimal maxBalance, Pageable pageable) {
        log.debug("Admin request to get filtered cards. Filters: status={}, ownerEmail={}, minBalance={}, maxBalance={}. Pageable: {}",
                status, ownerEmail, minBalance, maxBalance, pageable);
        Specification<Card> spec = CardSpecification.filterBy(status, ownerEmail, minBalance, maxBalance);
        Page<Card> cardPage = cardRepository.findAll(spec, pageable);
        return cardPage.map(this::mapToCardDto);
    }

    @Override
    @Transactional(readOnly = true)
    public CardDto getCardByIdAsAdmin(UUID id) {
        log.debug("Admin request to get card by ID: {}", id);
        Card card = findCardByIdOrThrow(id);
        return mapToCardDto(card);
    }

    @Override
    @Transactional
    public CardDto updateCardStatusAsAdmin(UUID id, CardStatus newStatus) {
        log.info("Admin request to update status for card ID: {} to {}", id, newStatus);
        if (newStatus == CardStatus.EXPIRED) {
            throw new BadRequestException(ADMIN_CANNOT_MANUALLY_SET_STATUS_EXPIRED);
        }
        Card card = findCardByIdOrThrow(id);

        if (card.getStatus() == newStatus) {
            log.warn("Card ID: {} already has status {}. No update performed.", id, newStatus);
            return mapToCardDto(card);
        }

        if (card.getStatus() == CardStatus.EXPIRED && newStatus == CardStatus.ACTIVE) {
            throw new CardOperationException(CANNOT_ACTIVATE_EXPIRED_CARD_ID + id);
        }


        card.setStatus(newStatus);
        Card updatedCard = cardRepository.save(card);
        log.info("Card ID: {} status updated to {} by admin", updatedCard.getId(), newStatus);
        return mapToCardDto(updatedCard);
    }

    @Override
    @Transactional
    public void deleteCardAsAdmin(UUID id) {
        log.warn("Admin request to delete card ID: {}", id);
        if (!cardRepository.existsById(id)) {
            throw new ResourceNotFoundException(CARD_NOT_FOUND + id);
        }
        cardRepository.deleteById(id);
        log.info("Card ID: {} deleted successfully by admin", id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CardDto> getCurrentUserCardsFiltered(CardStatus status, BigDecimal minBalance, BigDecimal maxBalance, Pageable pageable) {
        User currentUser = authenticationHelper.getCurrentUser();
        log.debug("User {} request to get own filtered cards. Filters: status={}, minBalance={}, maxBalance={}. Pageable: {}",
                currentUser.getEmail(), status, minBalance, maxBalance, pageable);
        Specification<Card> spec = CardSpecification.filterBy(status, null, minBalance, maxBalance, currentUser);
        Page<Card> cardPage = cardRepository.findAll(spec, pageable);
        return cardPage.map(this::mapToCardDto);
    }

    @Override
    @Transactional(readOnly = true)
    public CardDto getCurrentUserCardById(UUID id) {
        User currentUser = authenticationHelper.getCurrentUser();
        log.debug("User {} request to get own card by ID: {}", currentUser.getEmail(), id);
        Card card = findCardByIdAndOwnerOrThrow(id, currentUser);
        return mapToCardDto(card);
    }

    @Override
    @Transactional
    public CardDto requestBlockCard(UUID id) {
        User currentUser = authenticationHelper.getCurrentUser();
        log.info("User {} request to block own card ID: {}", currentUser.getEmail(), id);
        Card card = findCardByIdAndOwnerOrThrow(id, currentUser);

        if (card.getStatus() == CardStatus.BLOCKED) {
            log.warn("User {} requested to block card ID: {}, but it's already blocked.", currentUser.getEmail(), id);
            return mapToCardDto(card);
        }

        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new CardOperationException("Cannot block card ID: " + id + " because its status is not ACTIVE (current: " + card.getStatus() + ")");
        }

        card.setStatus(CardStatus.BLOCKED);
        Card updatedCard = cardRepository.save(card);
        log.info("Card ID: {} blocked successfully upon user {} request", updatedCard.getId(), currentUser.getEmail());
        return mapToCardDto(updatedCard);
    }

    @Override
    @Transactional
    public void transferFunds(TransferRequest request) {
        User currentUser = authenticationHelper.getCurrentUser();
        UUID fromCardId = request.getFromCardId();
        UUID toCardId = request.getToCardId();
        BigDecimal amount = request.getAmount();

        log.info("User {} request to transfer {} from card ID: {} to card ID: {}",
                currentUser.getEmail(), amount, fromCardId, toCardId);

        Objects.requireNonNull(fromCardId, "From card ID cannot be null");
        Objects.requireNonNull(toCardId, "To card ID cannot be null");
        Objects.requireNonNull(amount, "Amount cannot be null");

        if (fromCardId.equals(toCardId)) {
            throw new BadRequestException("Cannot transfer funds to the same card.");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Transfer amount must be positive.");
        }

        Card fromCard = findCardByIdAndOwnerOrThrow(fromCardId, currentUser);
        Card toCard = findCardByIdAndOwnerOrThrow(toCardId, currentUser);


        if (fromCard.getStatus() != CardStatus.ACTIVE) {
            throw new CardOperationException("Source card ID: " + fromCardId + " is not active.");
        }
        if (toCard.getStatus() != CardStatus.ACTIVE) {
            throw new CardOperationException("Destination card ID: " + toCardId + " is not active.");
        }

        if (fromCard.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds on source card ID: " + fromCardId);
        }

        fromCard.setBalance(fromCard.getBalance().subtract(amount));
        toCard.setBalance(toCard.getBalance().add(amount));

        cardRepository.saveAll(List.of(fromCard, toCard));

        log.info("Transfer successful for user {}: {} transferred from card {} to card {}",
                currentUser.getEmail(), amount, fromCardId, toCardId);
    }

    @Override
    @Transactional(readOnly = true)
    public BalanceDto getCurrentUserCardBalance(UUID id) {
        User currentUser = authenticationHelper.getCurrentUser();
        log.debug("User {} request for balance of card ID: {}", currentUser.getEmail(), id);
        Card card = findCardByIdAndOwnerOrThrow(id, currentUser);
        return new BalanceDto(card.getBalance());
    }

    @Override
    @Transactional(readOnly = true)
    public BalanceDto getCurrentUserTotalBalance() {
        User currentUser = authenticationHelper.getCurrentUser();
        BigDecimal totalBalance = cardRepository.getSumBalanceByOwner(currentUser);
        return new BalanceDto(totalBalance);
    }

    private Card findCardByIdOrThrow(UUID id) {
        return cardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + id));
    }

    private Card findCardByIdAndOwnerOrThrow(UUID id, User owner) {
        return cardRepository.findByIdAndOwner(id, owner)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + id + " for owner " + owner.getEmail()));
    }

    private CardDto mapToCardDto(Card card) {
        CardDto dto = new CardDto();
        dto.setId(card.getId());
        dto.setMaskedCardNumber(CardMaskingUtil.maskCardNumber(card.getCardNumber()));
        dto.setOwnerEmail(card.getOwner().getEmail());
        dto.setExpiryDate(card.getExpiryDate());
        dto.setStatus(card.getStatus());
        dto.setBalance(card.getBalance());
        return dto;
    }
}
