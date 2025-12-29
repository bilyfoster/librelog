package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.services.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceImplTest {

	private JwtService jwtService;
	private static final String TEST_SECRET = "test-secret-key-minimum-32-characters-long-for-hmac-sha";
	private static final UUID TEST_USER_ID = UUID.randomUUID();
	private static final String TEST_EMAIL = "test@example.com";
	private static final String TEST_ROLE = "USER";

	@BeforeEach
	void setUp() {
		jwtService = new JwtServiceImpl();
		ReflectionTestUtils.setField(jwtService, "secretKey", TEST_SECRET);
		ReflectionTestUtils.setField(jwtService, "expiration", 86400000L);
	}

	@Test
	void generateToken_When_ValidInput_Expect_TokenGenerated() {
		String token = jwtService.generateToken(TEST_USER_ID, TEST_EMAIL, TEST_ROLE);
		assertNotNull(token);
		assertFalse(token.isEmpty());
	}

	@Test
	void extractUserId_When_ValidToken_Expect_CorrectUserId() {
		String token = jwtService.generateToken(TEST_USER_ID, TEST_EMAIL, TEST_ROLE);
		String extractedUserId = jwtService.extractUserId(token);
		assertEquals(TEST_USER_ID.toString(), extractedUserId);
	}

	@Test
	void extractEmail_When_ValidToken_Expect_CorrectEmail() {
		String token = jwtService.generateToken(TEST_USER_ID, TEST_EMAIL, TEST_ROLE);
		String extractedEmail = jwtService.extractEmail(token);
		assertEquals(TEST_EMAIL, extractedEmail);
	}

	@Test
	void extractRole_When_ValidToken_Expect_CorrectRole() {
		String token = jwtService.generateToken(TEST_USER_ID, TEST_EMAIL, TEST_ROLE);
		String extractedRole = jwtService.extractRole(token);
		assertEquals(TEST_ROLE, extractedRole);
	}

	@Test
	void isTokenValid_When_ValidToken_Expect_True() {
		String token = jwtService.generateToken(TEST_USER_ID, TEST_EMAIL, TEST_ROLE);
		assertTrue(jwtService.isTokenValid(token));
	}

	@Test
	void isTokenValid_When_InvalidToken_Expect_False() {
		assertFalse(jwtService.isTokenValid("invalid.token.here"));
	}

}

