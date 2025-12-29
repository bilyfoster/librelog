package com.onelpro.librelog.config;

import com.onelpro.librelog.enums.UserRole;
import com.onelpro.librelog.enums.UserStatus;
import com.onelpro.librelog.models.User;
import com.onelpro.librelog.repositories.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Data initializer to create default admin user on application startup.
 */
@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Create admin user if it doesn't exist
        if (!userRepository.existsByUsername("admin")) {
            log.info("Creating default admin user...");
            User admin = User.builder()
                    .username("admin")
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .roles(java.util.Set.of(UserRole.ADMIN))
                    .status(UserStatus.ACTIVE)
                    .createdAt(Instant.now())
                    .build();

            userRepository.save(admin);
            log.info("Default admin user created successfully (username: admin, password: admin123)");
        } else {
            log.debug("Admin user already exists, skipping creation");
        }
    }
}

