package com.onelpro.librelog.models;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CustomRoleTest {

	@Test
	void builder_When_AllFieldsProvided_Expect_EntityCreated() {
		UUID id = UUID.randomUUID();
		User createdBy = User.builder().id(UUID.randomUUID()).build();
		Map<String, Object> permissions = new HashMap<>();
		permissions.put("ORDERS", "VIEW,CREATE");
		LocalDateTime now = LocalDateTime.now();

		CustomRole role = CustomRole.builder()
				.id(id)
				.name("Custom Sales Rep")
				.description("Sales rep with limited permissions")
				.permissions(permissions)
				.createdByUser(createdBy)
				.createdAt(now)
				.updatedAt(now)
				.build();

		assertNotNull(role);
		assertEquals(id, role.getId());
		assertEquals("Custom Sales Rep", role.getName());
		assertEquals("Sales rep with limited permissions", role.getDescription());
		assertEquals(permissions, role.getPermissions());
		assertEquals(createdBy, role.getCreatedByUser());
		assertEquals(now, role.getCreatedAt());
		assertEquals(now, role.getUpdatedAt());
	}

	@Test
	void noArgsConstructor_When_Called_Expect_EntityCreated() {
		CustomRole role = new CustomRole();
		assertNotNull(role);
	}

	@Test
	void setters_When_Called_Expect_ValuesSet() {
		CustomRole role = new CustomRole();
		UUID id = UUID.randomUUID();
		String name = "Test Role";

		role.setId(id);
		role.setName(name);

		assertEquals(id, role.getId());
		assertEquals(name, role.getName());
	}
}

