package com.onelpro.librelog.repositories;

import com.onelpro.librelog.config.TestcontainersConfiguration;
import com.onelpro.librelog.enums.AuditActionType;
import com.onelpro.librelog.enums.UserRole;
import com.onelpro.librelog.enums.UserStatus;
import com.onelpro.librelog.models.AuditLog;
import com.onelpro.librelog.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Transactional
@DirtiesContext
class AuditLogRepositoryTest {

	@Autowired
	private AuditLogRepository repository;

	@Autowired
	private com.onelpro.librelog.repositories.UserRepository userRepository;

	private User user;
	private LocalDateTime timestamp;

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

		timestamp = LocalDateTime.now();
	}

	@Test
	void findByUserId_When_UserHasLogs_Expect_LogsReturned() {
		// Create audit log
		AuditLog auditLog = AuditLog.builder()
				.user(user)
				.actionType(AuditActionType.CREATE)
				.resourceType("USER")
				.ipAddress("192.168.1.1")
				.timestamp(timestamp)
				.build();
		repository.save(auditLog);

		// Find by user ID
		List<AuditLog> logs = repository.findByUserId(user.getId());

		assertNotNull(logs);
		assertTrue(logs.size() >= 1);
		assertTrue(logs.stream().anyMatch(log -> log.getId().equals(auditLog.getId())));
	}

	@Test
	void findByActionType_When_LogsExist_Expect_LogsReturned() {
		// Create audit logs
		AuditLog log1 = AuditLog.builder()
				.user(user)
				.actionType(AuditActionType.LOGIN)
				.resourceType("USER")
				.timestamp(timestamp)
				.build();
		AuditLog log2 = AuditLog.builder()
				.user(user)
				.actionType(AuditActionType.LOGIN)
				.resourceType("USER")
				.timestamp(timestamp.plusSeconds(1))
				.build();
		repository.save(log1);
		repository.save(log2);

		// Find by action type
		List<AuditLog> logs = repository.findByActionType(AuditActionType.LOGIN);

		assertNotNull(logs);
		assertTrue(logs.size() >= 2);
	}

	@Test
	void findByTimestampBetween_When_LogsInRange_Expect_LogsReturned() {
		LocalDateTime start = timestamp.minusHours(1);
		LocalDateTime end = timestamp.plusHours(1);

		// Create audit log
		AuditLog auditLog = AuditLog.builder()
				.user(user)
				.actionType(AuditActionType.UPDATE)
				.resourceType("ORDER")
				.timestamp(timestamp)
				.build();
		repository.save(auditLog);

		// Find by timestamp range
		List<AuditLog> logs = repository.findByTimestampBetween(start, end);

		assertNotNull(logs);
		assertTrue(logs.size() >= 1);
		assertTrue(logs.stream().anyMatch(log -> log.getId().equals(auditLog.getId())));
	}

	@Test
	void findByUserId_WithPagination_When_UserHasLogs_Expect_PagedLogsReturned() {
		// Create multiple audit logs
		for (int i = 0; i < 5; i++) {
			AuditLog auditLog = AuditLog.builder()
					.user(user)
					.actionType(AuditActionType.CREATE)
					.resourceType("ORDER")
					.timestamp(timestamp.plusSeconds(i))
					.build();
			repository.save(auditLog);
		}

		// Find by user ID with pagination
		Pageable pageable = PageRequest.of(0, 2);
		Page<AuditLog> page = repository.findByUserId(user.getId(), pageable);

		assertNotNull(page);
		assertTrue(page.getTotalElements() >= 5);
		assertEquals(2, page.getContent().size());
	}

	@Test
	void findWithFilters_When_MultipleFiltersProvided_Expect_FilteredLogsReturned() {
		// Create audit logs
		AuditLog log1 = AuditLog.builder()
				.user(user)
				.actionType(AuditActionType.CREATE)
				.resourceType("ORDER")
				.timestamp(timestamp)
				.build();
		AuditLog log2 = AuditLog.builder()
				.user(user)
				.actionType(AuditActionType.UPDATE)
				.resourceType("ORDER")
				.timestamp(timestamp.plusSeconds(1))
				.build();
		repository.save(log1);
		repository.save(log2);

		// Find with filters
		Pageable pageable = PageRequest.of(0, 10);
		Page<AuditLog> page = repository.findWithFilters(
				user.getId(),
				AuditActionType.CREATE,
				"ORDER",
				null,
				timestamp.minusHours(1),
				timestamp.plusHours(1),
				pageable);

		assertNotNull(page);
		assertTrue(page.getTotalElements() >= 1);
		assertTrue(page.getContent().stream().anyMatch(log -> log.getActionType() == AuditActionType.CREATE));
	}
}

