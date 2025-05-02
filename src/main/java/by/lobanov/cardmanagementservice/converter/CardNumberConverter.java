package by.lobanov.cardmanagementservice.converter;

import by.lobanov.cardmanagementservice.service.EncryptionService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import static by.lobanov.cardmanagementservice.util.ServiceMessagesUtil.ENCRYPTION_HAS_NOT_BEEN_INJECTED_INTO_CARD_NUMBER_CONVERTER;

@Converter
@Component
public class CardNumberConverter implements AttributeConverter<String, String> {

    private static EncryptionService encryptionService;

    @Autowired
    public void setEncryptionService(@Lazy EncryptionService service) {
        CardNumberConverter.encryptionService = service;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || encryptionService == null) {
            return null;
        }
        return encryptionService.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        if (encryptionService == null) {
            throw new IllegalStateException(ENCRYPTION_HAS_NOT_BEEN_INJECTED_INTO_CARD_NUMBER_CONVERTER);
        }
        return encryptionService.decrypt(dbData);
    }
}
