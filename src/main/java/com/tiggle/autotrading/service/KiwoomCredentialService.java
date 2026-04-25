package com.tiggle.autotrading.service;

import com.tiggle.autotrading.model.KiwoomCredential;
import com.tiggle.autotrading.repository.KiwoomCredentialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class KiwoomCredentialService {

    private final KiwoomCredentialRepository credentialRepository;

    public KiwoomCredentialService(KiwoomCredentialRepository credentialRepository) {
        this.credentialRepository = credentialRepository;
    }

    @Transactional
    public void saveOrUpdate(Long userId, String appkey, String secretkey) {
        KiwoomCredential credential = credentialRepository.findByUserId(userId)
                .orElse(new KiwoomCredential(userId, appkey, secretkey));
        credential.setAppkey(appkey);
        credential.setSecretkey(secretkey);
        credentialRepository.save(credential);
    }

    @Transactional
    public void delete(Long userId) {
        if (!credentialRepository.existsByUserId(userId)) {
            throw new IllegalArgumentException("등록된 키움 인증정보가 없습니다.");
        }
        credentialRepository.deleteByUserId(userId);
    }

    public KiwoomCredential getCredential(Long userId) {
        return credentialRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "키움 API 인증정보가 등록되지 않았습니다. 먼저 /api/v1/kiwoom/credential 에서 appkey/secretkey를 등록하세요."));
    }

    public boolean hasCredential(Long userId) {
        return credentialRepository.existsByUserId(userId);
    }
}
