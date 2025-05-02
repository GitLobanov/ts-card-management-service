package by.lobanov.cardmanagementservice.service.impl;

import by.lobanov.cardmanagementservice.converter.CardNumberConverter;
import by.lobanov.cardmanagementservice.repository.CardRepository;
import by.lobanov.cardmanagementservice.service.CardNumberGeneratorService;
import by.lobanov.cardmanagementservice.util.LuhnUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class DefaultCardNumberGenerator implements CardNumberGeneratorService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultCardNumberGenerator.class);
    private static final int CARD_NUMBER_LENGTH = 16; // Стандартная длина номера карты
    private static final int MAX_GENERATION_ATTEMPTS = 10; // Ограничение на попытки генерации уникального номера

    private final SecureRandom random = new SecureRandom();
    private final LuhnUtil luhnUtil;
    private final CardRepository cardRepository; // Нужен для проверки уникальности
    private final CardNumberConverter cardNumberConverter; // Нужен для шифрования перед проверкой уникальности

    @Autowired
    public DefaultCardNumberGenerator(LuhnUtil luhnUtil, CardRepository cardRepository, CardNumberConverter cardNumberConverter) {
        this.luhnUtil = luhnUtil;
        this.cardRepository = cardRepository;
        this.cardNumberConverter = cardNumberConverter;
    }

    @Override
    public String generateUniqueCardNumber() {
        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            String cardNumber = generateLuhnValidNumber();
            // Важно: Проверяем уникальность ЗАШИФРОВАННОГО номера, т.к. именно он хранится в БД
            String encryptedCardNumber = cardNumberConverter.convertToDatabaseColumn(cardNumber);

            // Нужен метод в репозитории для проверки существования по зашифрованному номеру
            if (!cardRepository.existsByEncryptedCardNumber(encryptedCardNumber)) {
                logger.info("Generated unique card number on attempt {}", attempt + 1);
                return cardNumber; // Возвращаем НЕшифрованный номер
            }
            logger.warn("Generated card number collision detected on attempt {}. Retrying...", attempt + 1);
        }
        // Если не смогли сгенерировать уникальный номер за N попыток
        logger.error("Failed to generate a unique card number after {} attempts.", MAX_GENERATION_ATTEMPTS);
        throw new RuntimeException("Could not generate a unique card number.");
    }

    private String generateLuhnValidNumber() {
        // 1. Генерируем префикс (первые 15 цифр)
        // Можно добавить логику для БИН (первых 6 цифр), если нужно, но пока просто 15 случайных
        String prefix = IntStream.range(0, CARD_NUMBER_LENGTH - 1)
                .map(i -> random.nextInt(10)) // Генерируем цифру от 0 до 9
                .mapToObj(String::valueOf)
                .collect(Collectors.joining());

        // 2. Вычисляем контрольную цифру Луна
        String checkDigit = luhnUtil.calculateCheckDigit(prefix);

        // 3. Собираем полный номер
        String generatedNumber = prefix + checkDigit;
        logger.debug("Generated Luhn-valid number: {}", generatedNumber); // Логгируем для отладки
        return generatedNumber;
    }
}
