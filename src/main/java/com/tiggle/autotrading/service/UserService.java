package com.tiggle.autotrading.service;

import com.tiggle.autotrading.model.User;
import com.tiggle.autotrading.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private static final int MAX_FAIL_COUNT = 5;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public boolean register(String email, String userName, String mobile, String userType, String password) {
        if (isBlank(email) || isBlank(password)) {
            throw new IllegalArgumentException("이메일과 비밀번호는 필수입니다.");
        }
        if (isBlank(userName)) {
            throw new IllegalArgumentException("사용자 이름은 필수입니다.");
        }
        if (userRepository.existsByEmail(email)) {
            return false;
        }

        String hashedPassword = passwordEncoder.encode(password);
        User user = new User(email, hashedPassword, "", userName, userType != null ? userType : "USER");
        if (mobile != null) user.setMobile(mobile);

        userRepository.save(user);
        return true;
    }

    @Transactional
    public boolean register(String email, String password, String userName, String userType) {
        return register(email, userName, null, userType, password);
    }

    @Transactional
    public boolean register(String email, String password) {
        return register(email, password, email, "USER");
    }

    @Transactional
    public Optional<User> authenticate(String email, String password, String ipAddress) {
        if (isBlank(email) || isBlank(password)) {
            throw new IllegalArgumentException("이메일과 비밀번호는 필수입니다.");
        }

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }

        User user = userOpt.get();

        if (user.isLocked()) {
            return Optional.empty();
        }

        boolean passwordMatches = verifyPassword(user, password);

        if (!passwordMatches) {
            user.incrementFailCount();
            if (user.getFailCount() >= MAX_FAIL_COUNT) {
                user.lockAccount();
            }
            userRepository.save(user);
            return Optional.empty();
        }

        user.resetFailCount();
        if (ipAddress != null) user.updateRecentIp(ipAddress);
        userRepository.save(user);

        return Optional.of(user);
    }

    @Transactional
    public Optional<User> authenticate(String email, String password) {
        return authenticate(email, password, null);
    }

    private boolean verifyPassword(User user, String rawPassword) {
        String salt = user.getSalt();
        if (salt != null && !salt.isEmpty()) {
            // 레거시 SHA-256+Salt 검증 후 BCrypt 마이그레이션
            String hashedPassword = hashWithSalt(rawPassword, salt);
            boolean matches = user.getPassword().equals(hashedPassword);
            if (matches) {
                user.changePassword(passwordEncoder.encode(rawPassword));
            }
            return matches;
        }
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional
    public User updateUser(Long id, String userName, String mobile, String userType, Short usedBool) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + id));

        if (userName != null && !userName.isBlank()) user.setUserName(userName);
        if (mobile != null) user.setMobile(mobile);
        if (userType != null) user.setUserType(userType);
        if (usedBool != null) user.setUsedBool(usedBool);

        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다: " + id);
        }
        userRepository.deleteById(id);
    }

    @Transactional
    public void changePassword(Long id, String newPassword) {
        if (isBlank(newPassword)) {
            throw new IllegalArgumentException("새 비밀번호는 필수입니다.");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + id));

        validatePasswordHistory(user, newPassword);

        user.changePassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public User unlockAccount(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + id));
        user.resetFailCount();
        return userRepository.save(user);
    }

    public boolean isEmailExists(String email) {
        if (isBlank(email)) {
            throw new IllegalArgumentException("이메일은 필수입니다.");
        }
        return userRepository.existsByEmail(email);
    }

    @Transactional
    public String resetPassword(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + id));

        String tempPassword = generateRandomPassword(10);
        user.changePassword(passwordEncoder.encode(tempPassword));
        userRepository.save(user);
        return tempPassword;
    }

    private void validatePasswordHistory(User user, String newPassword) {
        if (matchesStoredPassword(newPassword, user.getPassword(), user.getSalt())) {
            throw new IllegalArgumentException("현재 사용 중인 비밀번호와 동일합니다.");
        }
        if (user.getPreviousPassword() != null && !user.getPreviousPassword().isEmpty()) {
            if (matchesStoredPassword(newPassword, user.getPreviousPassword(), null)) {
                throw new IllegalArgumentException("이전에 사용한 비밀번호는 재사용할 수 없습니다.");
            }
        }
    }

    private boolean matchesStoredPassword(String rawPassword, String storedPassword, String salt) {
        if (storedPassword == null || storedPassword.isEmpty()) return false;
        if (storedPassword.startsWith("$2")) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }
        if (salt != null && !salt.isEmpty()) {
            return storedPassword.equals(hashWithSalt(rawPassword, salt));
        }
        return false;
    }

    @Deprecated
    private String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    @Deprecated
    private String hashWithSalt(String password, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest((password + salt).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashed) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available.", e);
        }
    }

    private String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < length; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        return password.toString();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
