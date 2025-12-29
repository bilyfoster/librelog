package com.onelpro.librelog.repositories;

import com.onelpro.librelog.config.TestcontainersConfiguration;
import com.onelpro.librelog.enums.UserRole;
import com.onelpro.librelog.enums.UserStatus;
import com.onelpro.librelog.models.User;
import com.onelpro.librelog.models.UserSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Transactional
@DirtiesContext
class UserSessionRepositoryTest {

	@Autowired
	private UserSessionRepository repository;

	@Autowired
	private com.onelpro.librelog.repositories.UserRepository userRepository;

	private User user;
	private LocalDateTime now;

	@BeforeEach
	void setUp() {
		// Create user with unique email
		String uniqueEmail = "test" + UUID.randomUUID() + "@example.com";
		user = User.builder()
				.email(uniqueEmail)
				.password("hashedPassword")
				.status(UserStatus.ACTIVE)
				.role(UserRole.ADMIN)
				.createdAt(LocalDateTime.now())
				.build();
		user = userRepository.save(user);

		now = LocalDateTime.now();
	}

	@Test
	void findByUserId_When_UserHasSessions_Expect_SessionsReturned() {
		// Create session
		UserSession session = UserSession.builder()
				.user(user)
				.sessionToken("hashed_token_123")
				.loginTimestamp(now)
				.lastActivityTimestamp(now)
				.isActive(true)
				.expiresAt(now.plusHours(1))
				.build();
		repository.save(session);

		// Find by user ID
		List<UserSession> sessions = repository.findByUserId(user.getId());

		assertNotNull(sessions);
		assertTrue(sessions.size() >= 1);
		assertTrue(sessions.stream().anyMatch(s -> s.getId().equals(session.getId())));
	}

	@Test
	void findByIsActiveTrue_When_ActiveSessionsExist_Expect_ActiveSessionsReturned() {
		// Create active and inactive sessions
		UserSession activeSession = UserSession.builder()
				.user(user)
				.sessionToken("active_token")
				.loginTimestamp(now)
				.lastActivityTimestamp(now)
				.isActive(true)
				.expiresAt(now.plusHours(1))
				.build();
		UserSession inactiveSession = UserSession.builder()
				.user(user)
				.sessionToken("inactive_token")
				.loginTimestamp(now.minusHours(2))
				.lastActivityTimestamp(now.minusHours(2))
				.isActive(false)
				.expiresAt(now.minusHours(1))
				.build();
		repository.save(activeSession);
		repository.save(inactiveSession);

		// Find active sessions
		List<UserSession> activeSessions = repository.findByIsActiveTrue();

		assertNotNull(activeSessions);
		assertTrue(activeSessions.size() >= 1);
		assertTrue(activeSessions.stream().anyMatch(s -> s.getId().equals(activeSession.getId())));
		assertFalse(activeSessions.stream().anyMatch(s -> s.getId().equals(inactiveSession.getId())));
	}

	@Test
	void findByUserIdAndIsActiveTrue_When_UserHasActiveSessions_Expect_ActiveSessionsReturned() {
		// Create active session
		UserSession session = UserSession.builder()
				.user(user)
				.sessionToken("token_123")
				.loginTimestamp(now)
				.lastActivityTimestamp(now)
				.isActive(true)
				.expiresAt(now.plusHours(1))
				.build();
		repository.save(session);

		// Find by user ID and active
		List<UserSession> sessions = repository.findByUserIdAndIsActiveTrue(user.getId());

		assertNotNull(sessions);
		assertTrue(sessions.size() >= 1);
		assertTrue(sessions.stream().anyMatch(s -> s.getId().equals(session.getId())));
	}

	@Test
	void findBySessionToken_When_TokenExists_Expect_SessionReturned() {
		// Create session
		UserSession session = UserSession.builder()
				.user(user)
				.sessionToken("unique_token_456")
				.loginTimestamp(now)
				.lastActivityTimestamp(now)
				.isActive(true)
				.expiresAt(now.plusHours(1))
				.build();
		repository.save(session);

		// Find by token
		Optional<UserSession> found = repository.findBySessionToken("unique_token_456");

		assertTrue(found.isPresent());
		assertEquals(session.getId(), found.get().getId());
	}

	@Test
	void findByExpiresAtBefore_When_ExpiredSessionsExist_Expect_ExpiredSessionsReturned() {
		LocalDateTime pastTime = now.minusHours(2);

		// Create expired session
		UserSession expiredSession = UserSession.builder()
				.user(user)
				.sessionToken("expired_token")
				.loginTimestamp(pastTime)
				.lastActivityTimestamp(pastTime)
				.isActive(true)
				.expiresAt(pastTime.plusMinutes(30))
				.build();
		repository.save(expiredSession);

		// Find expired sessions
		List<UserSession> expired = repository.findByExpiresAtBefore(now);

		assertNotNull(expired);
		assertTrue(expired.size() >= 1);
		assertTrue(expired.stream().anyMatch(s -> s.getId().equals(expiredSession.getId())));
	}

	@Test
	void countByUserIdAndIsActiveTrue_When_UserHasActiveSessions_Expect_CorrectCount() {
		// Create multiple active sessions
		for (int i = 0; i < 3; i++) {
			UserSession session = UserSession.builder()
					.user(user)
					.sessionToken("token_" + i)
					.loginTimestamp(now)
					.lastActivityTimestamp(now)
					.isActive(true)
					.expiresAt(now.plusHours(1))
					.build();
			repository.save(session);
		}

		// Count active sessions
		long count = repository.countByUserIdAndIsActiveTrue(user.getId());

		assertTrue(count >= 3);
	}
}

