package com.tiggle.autotrading.config;

import com.tiggle.autotrading.repository.UserRepository;
import com.tiggle.autotrading.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("local")
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${init.admin.email:admin@tiggle.com}")
    private String adminEmail;

    @Value("${init.admin.password:Admin1234!}")
    private String adminPassword;

    @Value("${init.admin.name:관리자}")
    private String adminName;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmail(adminEmail)) {
            log.info("[DataInitializer] Admin account already exists: {}", adminEmail);
            return;
        }

        User admin = new User(
                adminEmail,
                passwordEncoder.encode(adminPassword),
                "",
                adminName,
                "ADMIN"
        );
        admin.setUsedBool((short) 1);

        userRepository.save(admin);
        log.info("[DataInitializer] Admin account created: {}", adminEmail);
    }
}
