package by.lobanov.cardmanagementservice.util;

public final class CardMaskingUtil {

    private CardMaskingUtil() {}

    private static final int VISIBLE_DIGITS = 4;
    private static final char MASK_CHAR = '*';
    private static final char SEPARATOR = ' ';

    /**
     * Маскирует номер карты, оставляя видимыми последние 4 цифры.
     * Форматирует результат как "**** **** **** 1234".
     *
     * @param decryptedCardNumber Расшифрованный номер карты.
     * @return Маскированный и отформатированный номер карты, или null если входные данные null.
     *         Возвращает строку с маской, даже если номер короче 4 символов.
     */
    public static String maskCardNumber(String decryptedCardNumber) {
        if (decryptedCardNumber == null) {
            return null;
        }

        String digitsOnly = decryptedCardNumber.replaceAll("\\D", "");
        int length = digitsOnly.length();

        if (length <= VISIBLE_DIGITS) {
            StringBuilder masked = new StringBuilder();
            for (int i = 0; i < 12; i++) {
                masked.append(MASK_CHAR);
                if ((i + 1) % 4 == 0 && i < 11) {
                    masked.append(SEPARATOR);
                }
            }
            masked.append(SEPARATOR);
            masked.append(digitsOnly);
            return masked.toString();

        } else {
            int maskedLength = length - VISIBLE_DIGITS;
            StringBuilder masked = new StringBuilder(length + 3);

            for (int i = 0; i < length; i++) {
                if (i < maskedLength) {
                    masked.append(MASK_CHAR);
                } else {
                    masked.append(digitsOnly.charAt(i));
                }
                if ((i + 1) % 4 == 0 && i < length - 1) {
                    masked.append(SEPARATOR);
                }
            }

            StringBuilder formattedMasked = new StringBuilder();
            int totalMaskedChars = 12; // Обычно 16 цифр, маскируем первые 12
            String visiblePart = digitsOnly.substring(maskedLength);

            for (int i = 0; i < totalMaskedChars; i++) {
                formattedMasked.append(MASK_CHAR);
                if ((i + 1) % 4 == 0 && i < totalMaskedChars -1) {
                    formattedMasked.append(SEPARATOR);
                }
            }
            formattedMasked.append(SEPARATOR);
            formattedMasked.append(visiblePart);

            return formattedMasked.toString();
        }
    }
}
