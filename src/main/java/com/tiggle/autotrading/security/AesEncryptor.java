package com.tiggle.autotrading.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES-256-GCM 암호화/복호화.
 * 출력 포맷: Base64(IV[12] || CipherText || GCM_TAG[16])
 */
@Component
public class AesEncryptor {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKeySpec secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public AesEncryptor(@Value("${encryption.aes.key}") String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalStateException(
                    "AES 암호화 키가 설정되지 않았습니다. 환경변수 AES_ENCRYPTION_KEY를 설정하세요. " +
                    "(생성 명령: openssl rand -base64 32)");
        }
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        if (keyBytes.length != 32) {
            throw new IllegalStateException(
                    "AES 키는 32바이트(AES-256)여야 합니다. 현재 키 길이: " + keyBytes.length + "바이트");
        }
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] cipherTextWithTag = cipher.doFinal(plaintext.getBytes());

            // IV를 앞에 붙여 저장 (복호화 시 추출)
            byte[] combined = new byte[IV_LENGTH_BYTES + cipherTextWithTag.length];
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH_BYTES);
            System.arraycopy(cipherTextWithTag, 0, combined, IV_LENGTH_BYTES, cipherTextWithTag.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("암호화 실패", e);
        }
    }

    public String decrypt(String base64Encrypted) {
        try {
            byte[] combined = Base64.getDecoder().decode(base64Encrypted);
            byte[] iv = Arrays.copyOfRange(combined, 0, IV_LENGTH_BYTES);
            byte[] cipherTextWithTag = Arrays.copyOfRange(combined, IV_LENGTH_BYTES, combined.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] plaintext = cipher.doFinal(cipherTextWithTag);

            return new String(plaintext);
        } catch (Exception e) {
            throw new IllegalStateException("복호화 실패", e);
        }
    }
}
