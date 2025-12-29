package com.onelpro.librelog.repositories;

import com.onelpro.librelog.config.TestcontainersConfiguration;
import com.onelpro.librelog.enums.UserRole;
import com.onelpro.librelog.enums.UserStatus;
import com.onelpro.librelog.models.CustomRole;
import com.onelpro.librelog.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Transactional
@DirtiesContext
class CustomRoleRepositoryTest {

	@Autowired
	private CustomRoleRepository repository;

	@Autowired
	private com.onelpro.librelog.repositories.UserRepository userRepository;

	private User createdByUser;
	private Map<String, Object> permissions;

	@BeforeEach
	void setUp() {
		// Create user with unique email
		String uniqueEmail = "admin" + UUID.randomUUID() + "@example.com";
		createdByUser = User.builder()
				.email(uniqueEmail)
				.password("hashedPassword")
				.status(UserStatus.ACTIVE)
				.role(UserRole.ADMIN)
				.createdAt(LocalDateTime.now())
				.build();
		createdByUser = userRepository.save(createdByUser);

		// Create permissions map
		permissions = new HashMap<>();
		permissions.put("ORDERS", "VIEW,CREATE");
		permissions.put("LOGS", "VIEW");
	}

	@Test
	void findByName_When_RoleExists_Expect_RoleReturned() {
		// Create custom role
		CustomRole role = CustomRole.builder()
				.name("Custom Sales Rep")
				.description("Sales rep with limited permissions")
				.permissions(permissions)
				.createdByUser(createdByUser)
				.createdAt(LocalDateTime.now())
				.build();
		repository.save(role);

		// Find by name
		Optional<CustomRole> found = repository.findByName("Custom Sales Rep");

		assertTrue(found.isPresent());
		assertEquals(role.getId(), found.get().getId());
		assertEquals("Custom Sales Rep", found.get().getName());
	}

	@Test
	void existsByName_When_RoleExists_Expect_True() {
		// Create custom role
		CustomRole role = CustomRole.builder()
				.name("Test Role")
				.permissions(permissions)
				.createdByUser(createdByUser)
				.createdAt(LocalDateTime.now())
				.build();
		repository.save(role);

		// Check existence
		boolean exists = repository.existsByName("Test Role");

		assertTrue(exists);
	}

	@Test
	void existsByName_When_RoleDoesNotExist_Expect_False() {
		// Check existence without creating role
		boolean exists = repository.existsByName("Non-existent Role");

		assertFalse(exists);
	}

	@Test
	void findByCreatedByUserId_When_UserCreatedRoles_Expect_RolesReturned() {
		// Create custom roles
		CustomRole role1 = CustomRole.builder()
				.name("Role 1")
				.permissions(permissions)
				.createdByUser(createdByUser)
				.createdAt(LocalDateTime.now())
				.build();
		CustomRole role2 = CustomRole.builder()
				.name("Role 2")
				.permissions(permissions)
				.createdByUser(createdByUser)
				.createdAt(LocalDateTime.now())
				.build();
		repository.save(role1);
		repository.save(role2);

		// Find by created by user ID
		List<CustomRole> roles = repository.findByCreatedByUserId(createdByUser.getId());

		assertNotNull(roles);
		assertTrue(roles.size() >= 2);
		assertTrue(roles.stream().anyMatch(r -> r.getName().equals("Role 1")));
		assertTrue(roles.stream().anyMatch(r -> r.getName().equals("Role 2")));
	}
}

