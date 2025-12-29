package com.onelpro.librelog.models;

import com.onelpro.librelog.enums.AuditActionType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AuditLogTest {

	@Test
	void builder_When_AllFieldsProvided_Expect_EntityCreated() {
		UUID id = UUID.randomUUID();
		User user = User.builder().id(UUID.randomUUID()).build();
		User impersonatedUser = User.builder().id(UUID.randomUUID()).build();
		Station station = Station.builder().id(UUID.randomUUID()).build();
		Map<String, Object> previousValue = new HashMap<>();
		previousValue.put("status", "ACTIVE");
		Map<String, Object> newValue = new HashMap<>();
		newValue.put("status", "INACTIVE");
		LocalDateTime timestamp = LocalDateTime.now();

		AuditLog auditLog = AuditLog.builder()
				.id(id)
				.user(user)
				.impersonatedUser(impersonatedUser)
				.actionType(AuditActionType.UPDATE)
				.resourceType("USER")
				.resourceId(UUID.randomUUID())
				.previousValue(previousValue)
				.newValue(newValue)
				.ipAddress("192.168.1.1")
				.userAgent("Mozilla/5.0")
				.station(station)
				.timestamp(timestamp)
				.build();

		assertNotNull(auditLog);
		assertEquals(id, auditLog.getId());
		assertEquals(user, auditLog.getUser());
		assertEquals(impersonatedUser, auditLog.getImpersonatedUser());
		assertEquals(AuditActionType.UPDATE, auditLog.getActionType());
		assertEquals("USER", auditLog.getResourceType());
		assertEquals(previousValue, auditLog.getPreviousValue());
		assertEquals(newValue, auditLog.getNewValue());
		assertEquals("192.168.1.1", auditLog.getIpAddress());
		assertEquals("Mozilla/5.0", auditLog.getUserAgent());
		assertEquals(station, auditLog.getStation());
		assertEquals(timestamp, auditLog.getTimestamp());
	}

	@Test
	void noArgsConstructor_When_Called_Expect_EntityCreated() {
		AuditLog auditLog = new AuditLog();
		assertNotNull(auditLog);
	}

	@Test
	void setters_When_Called_Expect_ValuesSet() {
		AuditLog auditLog = new AuditLog();
		UUID id = UUID.randomUUID();
		AuditActionType actionType = AuditActionType.CREATE;

		auditLog.setId(id);
		auditLog.setActionType(actionType);
		auditLog.setResourceType("ORDER");

		assertEquals(id, auditLog.getId());
		assertEquals(actionType, auditLog.getActionType());
		assertEquals("ORDER", auditLog.getResourceType());
	}
}

