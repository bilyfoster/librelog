package com.onelpro.librelog.models;

import com.onelpro.librelog.enums.PermissionLevel;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserStationAssignmentTest {

	@Test
	void builder_When_AllFieldsProvided_Expect_EntityCreated() {
		UUID id = UUID.randomUUID();
		User user = User.builder().id(UUID.randomUUID()).build();
		Station station = Station.builder().id(UUID.randomUUID()).build();
		Map<String, Object> customPermissions = new HashMap<>();
		customPermissions.put("ORDERS", "VIEW");
		LocalDateTime now = LocalDateTime.now();

		UserStationAssignment assignment = UserStationAssignment.builder()
				.id(id)
				.user(user)
				.station(station)
				.permissionLevel(PermissionLevel.CUSTOM)
				.customPermissions(customPermissions)
				.createdAt(now)
				.updatedAt(now)
				.build();

		assertNotNull(assignment);
		assertEquals(id, assignment.getId());
		assertEquals(user, assignment.getUser());
		assertEquals(station, assignment.getStation());
		assertEquals(PermissionLevel.CUSTOM, assignment.getPermissionLevel());
		assertEquals(customPermissions, assignment.getCustomPermissions());
		assertEquals(now, assignment.getCreatedAt());
		assertEquals(now, assignment.getUpdatedAt());
	}

	@Test
	void noArgsConstructor_When_Called_Expect_EntityCreated() {
		UserStationAssignment assignment = new UserStationAssignment();
		assertNotNull(assignment);
	}

	@Test
	void setters_When_Called_Expect_ValuesSet() {
		UserStationAssignment assignment = new UserStationAssignment();
		UUID id = UUID.randomUUID();
		PermissionLevel level = PermissionLevel.FULL_ACCESS;

		assignment.setId(id);
		assignment.setPermissionLevel(level);

		assertEquals(id, assignment.getId());
		assertEquals(level, assignment.getPermissionLevel());
	}
}

