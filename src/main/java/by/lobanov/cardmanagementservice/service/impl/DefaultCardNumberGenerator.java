package by.lobanov.cardmanagementservice.service.impl;

import by.lobanov.cardmanagementservice.converter.CardNumberConverter;
import by.lobanov.cardmanagementservice.repository.CardRepository;
import by.lobanov.cardmanagementservice.service.CardNumberGeneratorService;
import by.lobanov.cardmanagementservice.util.LuhnUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static by.lobanov.cardmanagementservice.util.ServiceMessagesUtil.COULD_NOT_GENERATE_UNIQUE_CARD_NUMBER;

@Service
@RequiredArgsConstructor
public class DefaultCardNumberGenerator implements CardNumberGeneratorService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultCardNumberGenerator.class);
    private static final int CARD_NUMBER_LENGTH = 16;
    private static final int MAX_GENERATION_ATTEMPTS = 10;

    private final SecureRandom random = new SecureRandom();
    private final LuhnUtil luhnUtil;
    private final CardRepository cardRepository;
    private final CardNumberConverter cardNumberConverter;

    @Override
    public String generateUniqueCardNumber() {
        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            String cardNumber = generateLuhnValidNumber();
            String encryptedCardNumber = cardNumberConverter.convertToDatabaseColumn(cardNumber);

            if (!cardRepository.existsByEncryptedCardNumber(encryptedCardNumber)) {
                logger.info("Generated unique card number on attempt {}", attempt + 1);
                return cardNumber;
            }
            logger.warn("Generated card number collision detected on attempt {}. Retrying...", attempt + 1);
        }
        logger.error("Failed to generate a unique card number after {} attempts.", MAX_GENERATION_ATTEMPTS);
        throw new RuntimeException(COULD_NOT_GENERATE_UNIQUE_CARD_NUMBER);
    }

    private String generateLuhnValidNumber() {
        String prefix = IntStream.range(0, CARD_NUMBER_LENGTH - 1)
                .map(i -> random.nextInt(10))
                .mapToObj(String::valueOf)
                .collect(Collectors.joining());

        String checkDigit = luhnUtil.calculateCheckDigit(prefix);

        return prefix + checkDigit;
    }
}
