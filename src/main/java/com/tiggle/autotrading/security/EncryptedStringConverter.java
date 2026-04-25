package com.tiggle.autotrading.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

/**
 * JPA AttributeConverter — DB 저장 시 AES-256-GCM 자동 암호화/복호화.
 * Spring Boot + Hibernate BeanContainer를 통해 AesEncryptor를 주입받습니다.
 */
@Converter
@Component
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private final AesEncryptor aesEncryptor;

    public EncryptedStringConverter(AesEncryptor aesEncryptor) {
        this.aesEncryptor = aesEncryptor;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isBlank()) return attribute;
        return aesEncryptor.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return dbData;
        return aesEncryptor.decrypt(dbData);
    }
}
