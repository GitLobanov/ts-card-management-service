package by.lobanov.cardmanagementservice.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.validator.routines.checkdigit.CheckDigit;
import org.apache.commons.validator.routines.checkdigit.LuhnCheckDigit;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public final class LuhnUtil {

    private LuhnUtil (){}

    private final CheckDigit luhnCheckDigit = LuhnCheckDigit.LUHN_CHECK_DIGIT;

    /**
     * Проверяет, является ли номер карты валидным согласно алгоритму Луна.
     *
     * @param cardNumber Номер карты (строка цифр).
     * @return true, если номер валиден, иначе false.
     */
    public boolean isValid(String cardNumber) {
        if (cardNumber == null || cardNumber.trim().isEmpty()) {
            return false;
        }
        // Удаляем нецифровые символы на всякий случай
        String digitsOnly = cardNumber.replaceAll("\\D", "");
        boolean isValid = luhnCheckDigit.isValid(digitsOnly);
        if (!isValid) {
            log.trace("Luhn check failed for card number (digits only): {}", digitsOnly);
        }
        return isValid;
    }

    /**
     * Вычисляет контрольную цифру Луна для заданного номера без контрольной цифры.
     *
     * @param numberWithoutCheckDigit Номер карты без последней (контрольной) цифры.
     * @return Строка с вычисленной контрольной цифрой.
     * @throws IllegalArgumentException если не удалось вычислить контрольную цифру.
     */
    public String calculateCheckDigit(String numberWithoutCheckDigit) {
        if (numberWithoutCheckDigit == null || numberWithoutCheckDigit.trim().isEmpty()) {
            throw new IllegalArgumentException("Input number cannot be null or empty");
        }
        String digitsOnly = numberWithoutCheckDigit.replaceAll("\\D", "");
        try {
            return luhnCheckDigit.calculate(digitsOnly);
        } catch (Exception e) { // Ловим CheckDigitException из commons-validator
            log.error("Failed to calculate Luhn check digit for: {}", digitsOnly, e);
            throw new IllegalArgumentException("Failed to calculate check digit for input: " + numberWithoutCheckDigit, e);
        }
    }
}
