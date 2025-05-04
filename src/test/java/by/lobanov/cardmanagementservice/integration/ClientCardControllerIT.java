package by.lobanov.cardmanagementservice.integration;

import by.lobanov.cardmanagementservice.converter.CardNumberConverter;
import by.lobanov.cardmanagementservice.model.constant.CardStatus;
import by.lobanov.cardmanagementservice.model.constant.RoleType;
import by.lobanov.cardmanagementservice.model.dto.CardDto;
import by.lobanov.cardmanagementservice.model.dto.request.LoginRequest;
import by.lobanov.cardmanagementservice.model.dto.request.TransferRequest;
import by.lobanov.cardmanagementservice.model.dto.response.JwtResponse;
import by.lobanov.cardmanagementservice.model.dto.response.PagedResponse;
import by.lobanov.cardmanagementservice.model.entity.Card;
import by.lobanov.cardmanagementservice.model.entity.Role;
import by.lobanov.cardmanagementservice.model.entity.User;
import by.lobanov.cardmanagementservice.repository.CardRepository;
import by.lobanov.cardmanagementservice.repository.RoleRepository;
import by.lobanov.cardmanagementservice.repository.UserRepository;
import by.lobanov.cardmanagementservice.service.CardNumberGeneratorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Fail.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.HttpStatus.OK;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ClientCardControllerIT {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CardRepository cardRepository;
    @Autowired
    private CardNumberConverter cardNumberConverter;
    @Autowired
    private CardNumberGeneratorService cardNumberGeneratorService;

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("test_card_db")
            .withUsername("testuser")
            .withPassword("testpassword");

    @DynamicPropertySource
    static void registerPostgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ObjectMapper objectMapper;

    private String baseUrl;
    private String jwtToken;
    private User testUserInDb;

    private String getBaseUrl() {
        return "http://localhost:" + port + "/api/cards";
    }

    @BeforeAll
    static void beforeAll() {
        postgres.start();
    }

    @BeforeEach
    void setUpEachTest() {
        baseUrl = getBaseUrl();

        String userEmail = "integration_test_user_" + UUID.randomUUID() + "@example.com"; // Уникальный email
        String userPassword = "password123";
        Role userRole = roleRepository.findByName(RoleType.ROLE_USER)
                .orElseGet(() -> roleRepository.save(new Role(RoleType.ROLE_USER))); // Получаем или создаем роль

        User userToCreate = new User();
        userToCreate.setEmail(userEmail);
        userToCreate.setPassword(passwordEncoder.encode(userPassword)); // Хешируем пароль!
        userToCreate.setRoles(Set.of(userRole));
        testUserInDb = userRepository.saveAndFlush(userToCreate); // Сохраняем и получаем ID

        String loginUrl = "http://localhost:" + port + "/api/auth/login";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(userEmail);
        loginRequest.setPassword(userPassword);

        try {
            String requestBody = objectMapper.writeValueAsString(loginRequest);
            HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<JwtResponse> response = restTemplate.postForEntity(loginUrl, requestEntity, JwtResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                this.jwtToken = response.getBody().getToken(); // Сохраняем токен
                assertNotNull(this.jwtToken, "JWT Token should not be null after login");
                System.out.println("Successfully logged in user: " + userEmail + " for test.");
            } else {
                fail("Authentication failed during test setup for user " + userEmail + ". Status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            fail("Exception during authentication setup for user " + userEmail + ": " + e.getMessage(), e);
        }
    }

    @Test
    void getCurrentUserCardsFiltered_shouldReturnCardsWithCorrectFilters() {
        User testUser = new User();
        testUser.setEmail("test_user@example.com");
        testUser.setPassword(passwordEncoder.encode("testpassword"));
        userRepository.save(testUser);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + jwtToken);
        Pageable pageable = PageRequest.of(0, 10);
        CardStatus status = CardStatus.ACTIVE;
        BigDecimal minBalance = BigDecimal.ZERO;

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("page", pageable.getPageNumber())
                .queryParam("size", pageable.getPageSize())
                .queryParam("status", status)
                .queryParam("minBalance", minBalance);

        HttpEntity<?> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<PagedResponse<CardDto>> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                requestEntity,
                new ParameterizedTypeReference<>() {
                }
        );

        assertEquals(OK, response.getStatusCode());
        assertNotNull(response.getBody());

        PagedResponse<CardDto> pagedResponse = response.getBody();
        assertNotNull(pagedResponse.getContent());
        assertTrue(pagedResponse.getContent().size() >= 0);

    }

    @Test
    void getCurrentUserCardById_shouldReturnCardDetails() {
        assertNotNull(jwtToken, "JWT Token must be obtained in setup");

        Card testCard = new Card();
        testCard.setOwner(testUserInDb);
        String rawCardNumber = "4444555566667771";
        String encryptedNumber = cardNumberConverter.convertToDatabaseColumn(rawCardNumber);
        testCard.setCardNumber(encryptedNumber);
        testCard.setExpiryDate("12/26");
        testCard.setStatus(CardStatus.ACTIVE);
        testCard.setBalance(new BigDecimal("555.55"));

        Card savedCard = cardRepository.saveAndFlush(testCard);
        UUID cardIdToFetch = savedCard.getId();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);

        ResponseEntity<CardDto> response;
        try {
            response = restTemplate.exchange(
                    baseUrl + "/" + cardIdToFetch,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    CardDto.class
            );
        } catch (HttpClientErrorException e) {
            fail("API call failed unexpectedly for existing card " + cardIdToFetch + ": " + e.getStatusCode() + " Body: " + e.getResponseBodyAsString(), e);
            return;
        }

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        CardDto cardDto = response.getBody();
        assertEquals(cardIdToFetch, cardDto.getId());
        assertEquals(testUserInDb.getEmail(), cardDto.getOwnerEmail());
        assertEquals(CardStatus.ACTIVE, cardDto.getStatus());
        assertEquals(0, new BigDecimal("555.55").compareTo(cardDto.getBalance()));
        assertNotNull(cardDto.getMaskedCardNumber());
    }

    @Test
    void requestBlockCard_shouldSetCardStatusToPendingBlock() {
        Card cardToBlock = new Card();
        cardToBlock.setOwner(testUserInDb);
        cardToBlock.setCardNumber(cardNumberConverter.convertToDatabaseColumn("3333000033330003"));
        cardToBlock.setExpiryDate("03/25");
        cardToBlock.setStatus(CardStatus.ACTIVE);
        cardToBlock.setBalance(new BigDecimal("50.00"));
        Card savedCard = cardRepository.saveAndFlush(cardToBlock);
        UUID cardId = savedCard.getId();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);

        HttpEntity<Void> requestEntity = new HttpEntity<>(null, headers);

        ResponseEntity<CardDto> response = restTemplate.exchange(
                baseUrl + "/" + cardId + "/block-request",
                HttpMethod.POST,
                requestEntity,
                CardDto.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(CardStatus.BLOCKED, response.getBody().getStatus());

        Card updatedCard = cardRepository.findById(cardId).orElseThrow();
        assertEquals(CardStatus.BLOCKED, updatedCard.getStatus());
    }

    @Test
    void transferFunds_shouldTransferMoneyBetweenCards() throws Exception {
        Card fromCard = new Card();
        fromCard.setOwner(testUserInDb);
        fromCard.setCardNumber(cardNumberConverter.convertToDatabaseColumn("1111000011110001"));
        fromCard.setExpiryDate("01/25");
        fromCard.setStatus(CardStatus.ACTIVE);
        fromCard.setBalance(new BigDecimal("1000.00"));
        Card savedFromCard = cardRepository.saveAndFlush(fromCard);
        UUID fromCardId = savedFromCard.getId();

        Card toCard = new Card();
        toCard.setOwner(testUserInDb); // Тот же владелец
        toCard.setCardNumber(cardNumberConverter.convertToDatabaseColumn("2222000022220002"));
        toCard.setExpiryDate("02/26");
        toCard.setStatus(CardStatus.ACTIVE);
        toCard.setBalance(new BigDecimal("500.00"));
        Card savedToCard = cardRepository.saveAndFlush(toCard);
        UUID toCardId = savedToCard.getId();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        TransferRequest transferRequest = new TransferRequest();
        transferRequest.setFromCardId(fromCardId);
        transferRequest.setToCardId(toCardId);
        transferRequest.setAmount(new BigDecimal("100.00"));

        String requestBody = objectMapper.writeValueAsString(transferRequest);
        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Void> response;
        try {
            response = restTemplate.exchange(
                    baseUrl + "/transfer",
                    HttpMethod.POST,
                    requestEntity,
                    Void.class
            );
        } catch (HttpClientErrorException e) {
            fail("API call for transfer failed unexpectedly: " + e.getStatusCode() + " Body: " + e.getResponseBodyAsString(), e);
            return;
        }

        assertEquals(HttpStatus.OK, response.getStatusCode());

        Card updatedFromCard = cardRepository.findById(fromCardId).orElseThrow();
        Card updatedToCard = cardRepository.findById(toCardId).orElseThrow();

        assertEquals(0, new BigDecimal("900.00").compareTo(updatedFromCard.getBalance()));
        assertEquals(0, new BigDecimal("600.00").compareTo(updatedToCard.getBalance()));
    }
}