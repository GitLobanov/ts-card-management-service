package by.lobanov.cardmanagementservice.unit;

import by.lobanov.cardmanagementservice.exception.BadRequestException;
import by.lobanov.cardmanagementservice.exception.CardOperationException;
import by.lobanov.cardmanagementservice.exception.InsufficientFundsException;
import by.lobanov.cardmanagementservice.exception.ResourceNotFoundException;
import by.lobanov.cardmanagementservice.mapper.CardMapper;
import by.lobanov.cardmanagementservice.model.constant.CardStatus;
import by.lobanov.cardmanagementservice.model.dto.BalanceDto;
import by.lobanov.cardmanagementservice.model.dto.CardDto;
import by.lobanov.cardmanagementservice.model.dto.request.CreateCardRequest;
import by.lobanov.cardmanagementservice.model.dto.request.TransferRequest;
import by.lobanov.cardmanagementservice.model.entity.Card;
import by.lobanov.cardmanagementservice.model.entity.User;
import by.lobanov.cardmanagementservice.repository.CardRepository;
import by.lobanov.cardmanagementservice.repository.UserRepository;
import by.lobanov.cardmanagementservice.service.CardNumberGeneratorService;
import by.lobanov.cardmanagementservice.service.impl.CardServiceImpl;
import by.lobanov.cardmanagementservice.util.AuthenticationHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceImplTest {

    @Mock
    private CardRepository cardRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuthenticationHelper authenticationHelper;
    @Mock
    private CardNumberGeneratorService cardNumberGeneratorService;
    @Mock
    private CardMapper cardMapper;

    @InjectMocks
    private CardServiceImpl cardService;

    @Captor
    private ArgumentCaptor<Card> cardArgumentCaptor;
    @Captor
    private ArgumentCaptor<List<Card>> cardListArgumentCaptor;
    @Captor
    private ArgumentCaptor<Specification<Card>> specificationArgumentCaptor; // Для проверки спецификации

    // Тестовые данные, инициализируемые перед каждым тестом
    private User testUser;
    private User testAdmin;
    private Card testCard1;
    private Card testCard2;
    private CardDto testCardDto1;
    private CreateCardRequest createCardRequest;
    private TransferRequest transferRequest;
    private Pageable pageable;
    private UUID userId;
    private UUID adminId;
    private UUID cardId1;
    private UUID cardId2;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        adminId = UUID.randomUUID();
        cardId1 = UUID.randomUUID();
        cardId2 = UUID.randomUUID();

        testUser = new User();
        testUser.setId(userId);
        testUser.setEmail("user@example.com");

        testAdmin = new User();
        testAdmin.setId(adminId);
        testAdmin.setEmail("admin@example.com");

        testCard1 = new Card();
        testCard1.setId(cardId1);
        testCard1.setOwner(testUser);
        testCard1.setCardNumber("1111222233334441");
        testCard1.setExpiryDate("12/25");
        testCard1.setStatus(CardStatus.ACTIVE);
        testCard1.setBalance(new BigDecimal("1000.00"));

        testCard2 = new Card();
        testCard2.setId(cardId2);
        testCard2.setOwner(testUser);
        testCard2.setCardNumber("5555666677778882");
        testCard2.setExpiryDate("06/26");
        testCard2.setStatus(CardStatus.ACTIVE);
        testCard2.setBalance(new BigDecimal("500.00"));

        testCardDto1 = new CardDto();
        testCardDto1.setId(cardId1);
        testCardDto1.setMaskedCardNumber("**** **** **** 4441");
        testCardDto1.setOwnerEmail(testUser.getEmail());
        testCardDto1.setStatus(CardStatus.ACTIVE);
        testCardDto1.setBalance(testCard1.getBalance());

        createCardRequest = new CreateCardRequest();
        createCardRequest.setUserId(userId);
        createCardRequest.setExpiryDate("10/27");

        transferRequest = new TransferRequest();
        transferRequest.setFromCardId(cardId1);
        transferRequest.setToCardId(cardId2);
        transferRequest.setAmount(new BigDecimal("100.00"));

        pageable = PageRequest.of(0, 10);
    }

    @Test
    void createCard_whenUserExists_shouldCreateAndSaveCard() {
        
        String generatedNumber = "9999888877776665";
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(cardNumberGeneratorService.generateUniqueCardNumber()).thenReturn(generatedNumber);
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cardMapper.toDto(any(Card.class))).thenReturn(testCardDto1); // Возвращаем любой DTO для проверки вызова

        CardDto resultDto = cardService.createCard(createCardRequest);

        assertNotNull(resultDto);
        verify(cardRepository).save(cardArgumentCaptor.capture());
        Card savedCard = cardArgumentCaptor.getValue();

        assertNotNull(savedCard);
        assertEquals(testUser, savedCard.getOwner());
        assertEquals(generatedNumber, savedCard.getCardNumber());
        assertEquals(createCardRequest.getExpiryDate(), savedCard.getExpiryDate());
        assertEquals(CardStatus.ACTIVE, savedCard.getStatus());
        assertEquals(BigDecimal.ZERO, savedCard.getBalance());

        verify(cardNumberGeneratorService).generateUniqueCardNumber();
        verify(cardMapper).toDto(savedCard); // Проверяем, что маппер вызван с сохраненной картой
    }

    @Test
    void createCard_whenUserNotFound_shouldThrowResourceNotFoundException() {
        
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        
        assertThrows(ResourceNotFoundException.class, () -> {
            cardService.createCard(createCardRequest);
        });

        verify(cardNumberGeneratorService, never()).generateUniqueCardNumber();
        verify(cardRepository, never()).save(any());
        verify(cardMapper, never()).toDto(any());
    }

    @Test
    void getAllCardsFiltered_shouldCallRepositoryFindAllWithSpecAndMapResult() {
        
        List<Card> cards = List.of(testCard1);
        Page<Card> cardPage = new PageImpl<>(cards, pageable, 1);
        when(cardRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(cardPage);
        when(cardMapper.toDto(testCard1)).thenReturn(testCardDto1);

        Page<CardDto> resultPage = cardService.getAllCardsFiltered(CardStatus.ACTIVE, "email@test.com", null, null, pageable);

        assertNotNull(resultPage);
        assertEquals(1, resultPage.getTotalElements());
        assertEquals(testCardDto1, resultPage.getContent().get(0));
        verify(cardRepository).findAll(specificationArgumentCaptor.capture(), eq(pageable));
        assertNotNull(specificationArgumentCaptor.getValue()); // Убеждаемся, что спецификация была передана
    }


    @Test
    void getCardByIdAsAdmin_whenCardExists_shouldReturnDto() {
        when(cardRepository.findById(cardId1)).thenReturn(Optional.of(testCard1));
        when(cardMapper.toDto(testCard1)).thenReturn(testCardDto1);

        CardDto result = cardService.getCardByIdAsAdmin(cardId1);

        assertNotNull(result);
        assertEquals(testCardDto1, result);
        verify(cardRepository).findById(cardId1);
        verify(cardMapper).toDto(testCard1);
    }

    @Test
    void getCardByIdAsAdmin_whenCardNotFound_shouldThrowResourceNotFoundException() {
        when(cardRepository.findById(cardId1)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            cardService.getCardByIdAsAdmin(cardId1);
        });
        verify(cardMapper, never()).toDto(any());
    }

    @Test
    void updateCardStatusAsAdmin_whenValidStatusChange_shouldUpdateAndSave() {
        
        when(cardRepository.findById(cardId1)).thenReturn(Optional.of(testCard1)); // status = ACTIVE
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cardMapper.toDto(any(Card.class))).thenAnswer(invocation -> {
            Card updatedCard = invocation.getArgument(0);
            CardDto dto = new CardDto();
            dto.setStatus(updatedCard.getStatus());
            dto.setId(updatedCard.getId());
            return dto;
        });

        CardDto resultDto = cardService.updateCardStatusAsAdmin(cardId1, CardStatus.BLOCKED);

        
        assertNotNull(resultDto);
        assertEquals(CardStatus.BLOCKED, resultDto.getStatus()); // Проверяем статус в DTO

        verify(cardRepository).save(cardArgumentCaptor.capture());
        Card savedCard = cardArgumentCaptor.getValue();
        assertEquals(CardStatus.BLOCKED, savedCard.getStatus()); // Проверяем статус перед сохранением
        assertEquals(cardId1, savedCard.getId());
        verify(cardMapper).toDto(savedCard);
    }

    @Test
    void updateCardStatusAsAdmin_whenStatusIsSame_shouldNotSaveAndReturnDto() {
        testCard1.setStatus(CardStatus.BLOCKED); // Устанавливаем исходный статус
        when(cardRepository.findById(cardId1)).thenReturn(Optional.of(testCard1));
        when(cardMapper.toDto(testCard1)).thenReturn(testCardDto1); // Маппер вернет DTO существующей карты

        CardDto resultDto = cardService.updateCardStatusAsAdmin(cardId1, CardStatus.BLOCKED); // Пытаемся установить тот же статус

        assertNotNull(resultDto);
        verify(cardRepository, never()).save(any());
        verify(cardMapper).toDto(testCard1);
    }

    @Test
    void updateCardStatusAsAdmin_whenSetExpired_shouldThrowBadRequestException() {
        assertThrows(BadRequestException.class, () -> {
            cardService.updateCardStatusAsAdmin(cardId1, CardStatus.EXPIRED);
        });

        verify(cardRepository, never()).findById(any());
        verify(cardRepository, never()).save(any());
    }

    @Test
    void updateCardStatusAsAdmin_whenActivateExpired_shouldThrowCardOperationException() {
        testCard1.setStatus(CardStatus.EXPIRED);
        when(cardRepository.findById(cardId1)).thenReturn(Optional.of(testCard1));

        assertThrows(CardOperationException.class, () -> {
            cardService.updateCardStatusAsAdmin(cardId1, CardStatus.ACTIVE);
        });
        verify(cardRepository, never()).save(any());
    }


    @Test
    void deleteCardAsAdmin_whenCardExists_shouldCallDelete() {
        when(cardRepository.existsById(cardId1)).thenReturn(true);
        doNothing().when(cardRepository).deleteById(cardId1);

        cardService.deleteCardAsAdmin(cardId1);

        
        verify(cardRepository).existsById(cardId1);
        verify(cardRepository).deleteById(cardId1); // Проверяем вызов deleteById
    }

    @Test
    void deleteCardAsAdmin_whenCardNotExists_shouldThrowResourceNotFoundException() {
        when(cardRepository.existsById(cardId1)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> {
            cardService.deleteCardAsAdmin(cardId1);
        });

        verify(cardRepository).existsById(cardId1);
        verify(cardRepository, never()).deleteById(any()); // Убеждаемся, что delete не вызывался
    }

    @Test
    void getCurrentUserCardsFiltered_shouldCallRepoWithUserSpecAndMap() {
        
        List<Card> cards = List.of(testCard1);
        Page<Card> cardPage = new PageImpl<>(cards, pageable, 1);
        when(authenticationHelper.getCurrentUser()).thenReturn(testUser);
        when(cardRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(cardPage);
        when(cardMapper.toDto(testCard1)).thenReturn(testCardDto1);

        Page<CardDto> resultPage = cardService.getCurrentUserCardsFiltered(CardStatus.ACTIVE, null, null, pageable);

        assertNotNull(resultPage);
        assertEquals(1, resultPage.getContent().size());
        verify(cardRepository).findAll(specificationArgumentCaptor.capture(), eq(pageable));
        assertNotNull(specificationArgumentCaptor.getValue());
        verify(authenticationHelper).getCurrentUser();
    }

    @Test
    void getCurrentUserCardById_whenCardExistsAndOwned_shouldReturnDto() {
        
        when(authenticationHelper.getCurrentUser()).thenReturn(testUser);
        when(cardRepository.findByIdAndOwner(cardId1, testUser)).thenReturn(Optional.of(testCard1));
        when(cardMapper.toDto(testCard1)).thenReturn(testCardDto1);

        CardDto result = cardService.getCurrentUserCardById(cardId1);

        assertNotNull(result);
        assertEquals(testCardDto1, result);
        verify(authenticationHelper).getCurrentUser();
        verify(cardRepository).findByIdAndOwner(cardId1, testUser);
        verify(cardMapper).toDto(testCard1);
    }

    @Test
    void getCurrentUserCardById_whenCardNotFoundOrNotOwned_shouldThrowResourceNotFoundException() {
        when(authenticationHelper.getCurrentUser()).thenReturn(testUser);
        when(cardRepository.findByIdAndOwner(cardId1, testUser)).thenReturn(Optional.empty());

        
        assertThrows(ResourceNotFoundException.class, () -> {
            cardService.getCurrentUserCardById(cardId1);
        });
        verify(cardMapper, never()).toDto(any());
    }


    @Test
    void requestBlockCard_whenCardIsActive_shouldSetBlockedAndSave() {
        
        testCard1.setStatus(CardStatus.ACTIVE); // Убедимся, что карта активна
        when(authenticationHelper.getCurrentUser()).thenReturn(testUser);
        when(cardRepository.findByIdAndOwner(cardId1, testUser)).thenReturn(Optional.of(testCard1));
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cardMapper.toDto(any(Card.class))).thenAnswer(invocation -> {
            Card updatedCard = invocation.getArgument(0);
            CardDto dto = new CardDto();
            dto.setStatus(updatedCard.getStatus());
            dto.setId(updatedCard.getId());
            return dto;
        });

        
        CardDto resultDto = cardService.requestBlockCard(cardId1);

        assertNotNull(resultDto);
        assertEquals(CardStatus.BLOCKED, resultDto.getStatus()); // Проверяем статус в DTO

        verify(cardRepository).save(cardArgumentCaptor.capture());
        Card savedCard = cardArgumentCaptor.getValue();
        assertEquals(CardStatus.BLOCKED, savedCard.getStatus()); // Проверяем статус перед сохранением
        verify(cardMapper).toDto(savedCard);
    }

    @Test
    void requestBlockCard_whenCardIsNotActive_shouldThrowCardOperationException() {
        
        testCard1.setStatus(CardStatus.EXPIRED); // Не активная карта
        when(authenticationHelper.getCurrentUser()).thenReturn(testUser);
        when(cardRepository.findByIdAndOwner(cardId1, testUser)).thenReturn(Optional.of(testCard1));

        assertThrows(CardOperationException.class, () -> {
            cardService.requestBlockCard(cardId1);
        });
        verify(cardRepository, never()).save(any());
    }

    @Test
    void requestBlockCard_whenCardIsAlreadyBlocked_shouldNotSaveAndReturnDto() {
        
        testCard1.setStatus(CardStatus.BLOCKED); // Уже заблокирована
        when(authenticationHelper.getCurrentUser()).thenReturn(testUser);
        when(cardRepository.findByIdAndOwner(cardId1, testUser)).thenReturn(Optional.of(testCard1));
        when(cardMapper.toDto(testCard1)).thenReturn(testCardDto1); // Мокаем возврат DTO существующей карты

        CardDto resultDto = cardService.requestBlockCard(cardId1);

        assertNotNull(resultDto);
        assertEquals(testCardDto1, resultDto); // Должны вернуть DTO как есть
        verify(cardRepository, never()).save(any()); // Save не должен вызываться
        verify(cardMapper).toDto(testCard1); // Маппер вызывается для существующей карты
    }

    @Test
    void transferFunds_whenValidRequest_shouldUpdateBalancesAndSaveAll() {
        
        BigDecimal transferAmount = new BigDecimal("100.00");
        BigDecimal initialFromBalance = new BigDecimal("1000.00");
        BigDecimal initialToBalance = new BigDecimal("500.00");
        testCard1.setStatus(CardStatus.ACTIVE);
        testCard1.setBalance(initialFromBalance);
        testCard2.setStatus(CardStatus.ACTIVE);
        testCard2.setBalance(initialToBalance);

        when(authenticationHelper.getCurrentUser()).thenReturn(testUser);
        when(cardRepository.findByIdAndOwner(cardId1, testUser)).thenReturn(Optional.of(testCard1));
        when(cardRepository.findByIdAndOwner(cardId2, testUser)).thenReturn(Optional.of(testCard2));
        
        when(cardRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0)); // Просто возвращаем список для проверки

        transferRequest.setAmount(transferAmount);

        cardService.transferFunds(transferRequest);

        
        verify(cardRepository).saveAll(cardListArgumentCaptor.capture());
        List<Card> savedCards = cardListArgumentCaptor.getValue();

        assertNotNull(savedCards);
        assertEquals(2, savedCards.size());

        Card savedFromCard = savedCards.stream().filter(c -> c.getId().equals(cardId1)).findFirst().orElseThrow();
        Card savedToCard = savedCards.stream().filter(c -> c.getId().equals(cardId2)).findFirst().orElseThrow();

        assertEquals(0, initialFromBalance.subtract(transferAmount).compareTo(savedFromCard.getBalance()));
        assertEquals(0, initialToBalance.add(transferAmount).compareTo(savedToCard.getBalance()));
    }

    @Test
    void transferFunds_whenInsufficientFunds_shouldThrowInsufficientFundsException() {
        
        testCard1.setBalance(new BigDecimal("50.00")); // Недостаточно средств
        testCard1.setStatus(CardStatus.ACTIVE);
        testCard2.setStatus(CardStatus.ACTIVE);
        transferRequest.setAmount(new BigDecimal("100.00"));

        when(authenticationHelper.getCurrentUser()).thenReturn(testUser);
        when(cardRepository.findByIdAndOwner(cardId1, testUser)).thenReturn(Optional.of(testCard1));
        when(cardRepository.findByIdAndOwner(cardId2, testUser)).thenReturn(Optional.of(testCard2));
        
        assertThrows(InsufficientFundsException.class, () -> {
            cardService.transferFunds(transferRequest);
        });
        verify(cardRepository, never()).saveAll(anyList()); // Save не должен вызываться
    }

    @Test
    void transferFunds_whenSourceCardNotActive_shouldThrowCardOperationException() {
        
        testCard1.setStatus(CardStatus.BLOCKED); // Не активна
        testCard2.setStatus(CardStatus.ACTIVE);
        transferRequest.setAmount(new BigDecimal("100.00"));

        when(authenticationHelper.getCurrentUser()).thenReturn(testUser);
        when(cardRepository.findByIdAndOwner(cardId1, testUser)).thenReturn(Optional.of(testCard1));
        when(cardRepository.findByIdAndOwner(cardId2, testUser)).thenReturn(Optional.of(testCard2));

        assertThrows(CardOperationException.class, () -> {
            cardService.transferFunds(transferRequest);
        });
        verify(cardRepository, never()).saveAll(anyList());
    }

    @Test
    void transferFunds_whenAmountIsNegative_shouldThrowBadRequestException() {
        
        transferRequest.setAmount(new BigDecimal("-100.00")); // Негативная сумма

        when(authenticationHelper.getCurrentUser()).thenReturn(testUser);

        assertThrows(BadRequestException.class, () -> {
            cardService.transferFunds(transferRequest);
        });
        verify(cardRepository, never()).findByIdAndOwner(any(), any());
        verify(cardRepository, never()).saveAll(anyList());
    }

    @Test
    void transferFunds_whenSameCard_shouldThrowBadRequestException() {
        
        transferRequest.setToCardId(cardId1); // Перевод на ту же карту

        when(authenticationHelper.getCurrentUser()).thenReturn(testUser);

        assertThrows(BadRequestException.class, () -> {
            cardService.transferFunds(transferRequest);
        });
        verify(cardRepository, never()).findByIdAndOwner(any(), any());
        verify(cardRepository, never()).saveAll(anyList());
    }

    @Test
    void getCurrentUserCardBalance_whenCardExists_shouldReturnBalanceDto() {
        BigDecimal expectedBalance = new BigDecimal("1234.56");
        testCard1.setBalance(expectedBalance);
        when(authenticationHelper.getCurrentUser()).thenReturn(testUser);
        when(cardRepository.findByIdAndOwner(cardId1, testUser)).thenReturn(Optional.of(testCard1));

        BalanceDto result = cardService.getCurrentUserCardBalance(cardId1);

        assertNotNull(result);
        assertEquals(0, expectedBalance.compareTo(result.getBalance()));
        verify(authenticationHelper).getCurrentUser();
        verify(cardRepository).findByIdAndOwner(cardId1, testUser);
    }

    @Test
    void getCurrentUserTotalBalance_shouldCallRepoAndReturnBalanceDto() {
        
        BigDecimal expectedTotalBalance = new BigDecimal("9876.54");
        when(authenticationHelper.getCurrentUser()).thenReturn(testUser);
        when(cardRepository.getSumBalanceByOwner(testUser)).thenReturn(expectedTotalBalance);
        
        BalanceDto result = cardService.getCurrentUserTotalBalance();

        assertNotNull(result);
        assertEquals(0, expectedTotalBalance.compareTo(result.getBalance()));
        verify(authenticationHelper).getCurrentUser();
        verify(cardRepository).getSumBalanceByOwner(testUser);
    }
}
