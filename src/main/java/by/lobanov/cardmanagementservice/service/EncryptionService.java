package by.lobanov.cardmanagementservice.service;

/**
 * Абстракция для работы с шифрованием данных
 */
public interface EncryptionService {
    /**
     * Шифрует строку.
     * @param data Исходная строка.
     * @return Зашифрованная строка (вероятно, в Base64).
     * @throws RuntimeException если произошла ошибка шифрования.
     */
    String encrypt(String data);

    /**
     * Дешифрует строку.
     * @param encryptedData Зашифрованная строка (вероятно, в Base64).
     * @return Исходная расшифрованная строка.
     * @throws RuntimeException если произошла ошибка дешифрования.
     */
    String decrypt(String encryptedData);
}
