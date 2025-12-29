package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.models.User;
import com.onelpro.librelog.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthSecurityServiceImplTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserRepository userRepository;

    private AuthSecurityServiceImpl authSecurityService;

    @BeforeEach
    void setUp() {
        authSecurityService = new AuthSecurityServiceImpl(passwordEncoder, userRepository);
    }

    @Test
    void hashPassword_When_ValidPassword_Expect_HashedPassword() {
        String rawPassword = "testPassword123!";
        String hashedPassword = "$2a$10$hashed";
        
        when(passwordEncoder.encode(rawPassword)).thenReturn(hashedPassword);
        
        String result = authSecurityService.hashPassword(rawPassword);
        assertEquals(hashedPassword, result);
        verify(passwordEncoder).encode(rawPassword);
    }

    @Test
    void hashPassword_When_NullPassword_Expect_Exception() {
        assertThrows(IllegalArgumentException.class, () -> authSecurityService.hashPassword(null));
    }

    @Test
    void hashPassword_When_EmptyPassword_Expect_Exception() {
        assertThrows(IllegalArgumentException.class, () -> authSecurityService.hashPassword(""));
    }

    @Test
    void verifyPassword_When_PasswordsMatch_Expect_True() {
        String rawPassword = "testPassword123!";
        String hashedPassword = "$2a$10$hashed";
        
        when(passwordEncoder.matches(rawPassword, hashedPassword)).thenReturn(true);
        
        boolean result = authSecurityService.verifyPassword(rawPassword, hashedPassword);
        assertTrue(result);
        verify(passwordEncoder).matches(rawPassword, hashedPassword);
    }

    @Test
    void verifyPassword_When_PasswordsDoNotMatch_Expect_False() {
        String rawPassword = "testPassword123!";
        String hashedPassword = "$2a$10$hashed";
        
        when(passwordEncoder.matches(rawPassword, hashedPassword)).thenReturn(false);
        
        boolean result = authSecurityService.verifyPassword(rawPassword, hashedPassword);
        assertFalse(result);
    }

    @Test
    void verifyPassword_When_NullRawPassword_Expect_False() {
        assertFalse(authSecurityService.verifyPassword(null, "hashed"));
    }

    @Test
    void verifyPassword_When_NullHashedPassword_Expect_False() {
        assertFalse(authSecurityService.verifyPassword("raw", null));
    }

    @Test
    void updateLastLogin_When_ValidUser_Expect_LastLoginUpdated() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .passwordHash("hashed")
                .build();
        
        when(userRepository.save(any(User.class))).thenReturn(user);
        
        authSecurityService.updateLastLogin(user);
        
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        
        User savedUser = userCaptor.getValue();
        assertNotNull(savedUser.getLastLogin());
        assertTrue(savedUser.getLastLogin().isBefore(Instant.now().plusSeconds(1)));
    }

    @Test
    void updateLastLogin_When_NullUser_Expect_NoException() {
        assertDoesNotThrow(() -> authSecurityService.updateLastLogin(null));
        verify(userRepository, never()).save(any());
    }
}

