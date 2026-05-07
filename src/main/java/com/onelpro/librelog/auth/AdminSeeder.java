package com.onelpro.librelog.auth;

import com.onelpro.librelog.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminSeeder implements CommandLineRunner {

    private final AppUserRepository users;
    private final AppProperties props;
    private final PasswordEncoder encoder;

    @Override
    public void run(String... args) {
        var seed = props.getAdmin();
        if (seed.getSeedEmail() == null || seed.getSeedEmail().isBlank()) return;
        if (users.existsByEmail(seed.getSeedEmail().toLowerCase())) return;

        AppUser admin = AppUser.builder()
                .email(seed.getSeedEmail().toLowerCase())
                .passwordHash(encoder.encode(seed.getSeedPassword()))
                .name(seed.getSeedName())
                .role("ADMIN")
                .active(true)
                .build();
        users.save(admin);
        log.info("Seeded admin user {} (please change the password on first login)", admin.getEmail());
    }
}
