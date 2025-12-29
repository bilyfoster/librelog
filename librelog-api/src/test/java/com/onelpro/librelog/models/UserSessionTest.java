package com.onelpro.librelog.models;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserSessionTest {

	@Test
	void builder_When_AllFieldsProvided_Expect_EntityCreated() {
		UUID id = UUID.randomUUID();
		User user = User.builder().id(UUID.randomUUID()).build();
		Station station = Station.builder().id(UUID.randomUUID()).build();
		LocalDateTime loginTime = LocalDateTime.now();
		LocalDateTime lastActivity = LocalDateTime.now();
		LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);

		UserSession session = UserSession.builder()
				.id(id)
				.user(user)
				.sessionToken("hashed_token_12345")
				.loginTimestamp(loginTime)
				.lastActivityTimestamp(lastActivity)
				.ipAddress("192.168.1.1")
				.userAgent("Mozilla/5.0")
				.currentStation(station)
				.currentResourceId(UUID.randomUUID())
				.isActive(true)
				.expiresAt(expiresAt)
				.build();

		assertNotNull(session);
		assertEquals(id, session.getId());
		assertEquals(user, session.getUser());
		assertEquals("hashed_token_12345", session.getSessionToken());
		assertEquals(loginTime, session.getLoginTimestamp());
		assertEquals(lastActivity, session.getLastActivityTimestamp());
		assertEquals("192.168.1.1", session.getIpAddress());
		assertEquals("Mozilla/5.0", session.getUserAgent());
		assertEquals(station, session.getCurrentStation());
		assertTrue(session.getIsActive());
		assertEquals(expiresAt, session.getExpiresAt());
	}

	@Test
	void noArgsConstructor_When_Called_Expect_EntityCreated() {
		UserSession session = new UserSession();
		assertNotNull(session);
	}

	@Test
	void setters_When_Called_Expect_ValuesSet() {
		UserSession session = new UserSession();
		UUID id = UUID.randomUUID();
		Boolean isActive = false;

		session.setId(id);
		session.setIsActive(isActive);

		assertEquals(id, session.getId());
		assertEquals(isActive, session.getIsActive());
	}
}

