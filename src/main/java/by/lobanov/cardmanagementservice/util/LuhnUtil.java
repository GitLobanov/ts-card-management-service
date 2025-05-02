package by.lobanov.cardmanagementservice.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.validator.routines.checkdigit.CheckDigit;
import org.apache.commons.validator.routines.checkdigit.LuhnCheckDigit;
import org.springframework.stereotype.Component;

import static by.lobanov.cardmanagementservice.util.ServiceMessagesUtil.FAILED_TO_CALCULATE_DIGIT_FOR_INPUT;
import static by.lobanov.cardmanagementservice.util.ServiceMessagesUtil.INPUT_NUMBER_CANNOT_BE_NULL_OR_EMPTY;

@Component
@Slf4j
@UtilityClass
public class LuhnUtil {

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
            throw new IllegalArgumentException(INPUT_NUMBER_CANNOT_BE_NULL_OR_EMPTY);
        }
        String digitsOnly = numberWithoutCheckDigit.replaceAll("\\D", "");
        try {
            return luhnCheckDigit.calculate(digitsOnly);
        } catch (Exception e) {
            log.error("Failed to calculate Luhn check digit for: {}", digitsOnly, e);
            throw new IllegalArgumentException(FAILED_TO_CALCULATE_DIGIT_FOR_INPUT + numberWithoutCheckDigit, e);
        }
    }
}
