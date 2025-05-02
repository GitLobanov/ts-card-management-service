package by.lobanov.cardmanagementservice.service.impl;

import by.lobanov.cardmanagementservice.service.EncryptionService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
@Slf4j
public class AesEncryptionService implements EncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final SecretKey secretKey;

    public AesEncryptionService(@Value("${app.encryption.key}") String encryptionKey) {
        if (encryptionKey == null || encryptionKey.length() != 32) {
            log.error("Invalid encryption key length. Key must be 32 bytes long. Provided length: {}", encryptionKey != null ? encryptionKey.length() : "null");
            throw new IllegalArgumentException("Invalid encryption key length. Key must be 32 bytes long.");
        }
        this.secretKey = new SecretKeySpec(encryptionKey.getBytes(StandardCharsets.UTF_8), "AES");
        log.info("AES Encryption Service initialized successfully.");
    }

    @Override
    public String encrypt(String data) {
        if (data == null) {
            return null;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);

            byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

            byte[] encryptedDataWithIv = new byte[GCM_IV_LENGTH + encryptedBytes.length];
            System.arraycopy(iv, 0, encryptedDataWithIv, 0, GCM_IV_LENGTH);
            System.arraycopy(encryptedBytes, 0, encryptedDataWithIv, GCM_IV_LENGTH, encryptedBytes.length);

            return Base64.getEncoder().encodeToString(encryptedDataWithIv);
        } catch (Exception e) {
            log.error("Encryption failed", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }

    @Override
    public String decrypt(String encryptedData) {
        if (encryptedData == null) {
            return null;
        }
        try {
            byte[] encryptedDataWithIv = Base64.getDecoder().decode(encryptedData);

            if (encryptedDataWithIv.length < GCM_IV_LENGTH) {
                log.error("Invalid encrypted data format (too short).");
                throw new IllegalArgumentException("Invalid encrypted data format.");
            }

            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(encryptedDataWithIv, 0, iv, 0, GCM_IV_LENGTH);

            byte[] encryptedBytes = new byte[encryptedDataWithIv.length - GCM_IV_LENGTH];
            System.arraycopy(encryptedDataWithIv, GCM_IV_LENGTH, encryptedBytes, 0, encryptedBytes.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);

            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Decryption failed", e);
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
