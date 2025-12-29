package com.onelpro.librelog.repositories;

import com.onelpro.librelog.config.TestcontainersConfiguration;
import com.onelpro.librelog.enums.PermissionLevel;
import com.onelpro.librelog.enums.StationType;
import com.onelpro.librelog.enums.UserRole;
import com.onelpro.librelog.enums.UserStatus;
import com.onelpro.librelog.models.Organization;
import com.onelpro.librelog.models.Station;
import com.onelpro.librelog.models.User;
import com.onelpro.librelog.models.UserStationAssignment;
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
class UserStationAssignmentRepositoryTest {

	@Autowired
	private UserStationAssignmentRepository repository;

	@Autowired
	private com.onelpro.librelog.repositories.UserRepository userRepository;

	@Autowired
	private com.onelpro.librelog.repositories.StationRepository stationRepository;

	@Autowired
	private com.onelpro.librelog.repositories.OrganizationRepository organizationRepository;

	private User user;
	private Station station;
	private Organization organization;

	@BeforeEach
	void setUp() {
		// Create organization with unique name
		String uniqueOrgName = "Test Organization " + UUID.randomUUID();
		organization = Organization.builder()
				.name(uniqueOrgName)
				.isActive(true)
				.createdAt(LocalDateTime.now())
				.build();
		organization = organizationRepository.save(organization);

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

		// Create station with unique call sign
		String uniqueCallSign = "K" + UUID.randomUUID().toString().substring(0, 3).toUpperCase();
		station = Station.builder()
				.organization(organization)
				.callSign(uniqueCallSign)
				.name("Test Station")
				.stationType(StationType.AM)
				.isActive(true)
				.createdAt(LocalDateTime.now())
				.build();
		station = stationRepository.save(station);
	}

	@Test
	void findByUserId_When_UserHasAssignments_Expect_AssignmentsReturned() {
		// Create assignment
		UserStationAssignment assignment = UserStationAssignment.builder()
				.user(user)
				.station(station)
				.permissionLevel(PermissionLevel.FULL_ACCESS)
				.createdAt(LocalDateTime.now())
				.build();
		repository.save(assignment);

		// Find by user ID
		List<UserStationAssignment> assignments = repository.findByUserId(user.getId());

		assertNotNull(assignments);
		assertEquals(1, assignments.size());
		assertEquals(assignment.getId(), assignments.get(0).getId());
	}

	@Test
	void findByStationId_When_StationHasAssignments_Expect_AssignmentsReturned() {
		// Create assignment
		UserStationAssignment assignment = UserStationAssignment.builder()
				.user(user)
				.station(station)
				.permissionLevel(PermissionLevel.VIEW_ONLY)
				.createdAt(LocalDateTime.now())
				.build();
		repository.save(assignment);

		// Find by station ID
		List<UserStationAssignment> assignments = repository.findByStationId(station.getId());

		assertNotNull(assignments);
		assertEquals(1, assignments.size());
		assertEquals(assignment.getId(), assignments.get(0).getId());
	}

	@Test
	void findByUserIdAndStationId_When_AssignmentExists_Expect_AssignmentReturned() {
		// Create assignment
		UserStationAssignment assignment = UserStationAssignment.builder()
				.user(user)
				.station(station)
				.permissionLevel(PermissionLevel.CUSTOM)
				.createdAt(LocalDateTime.now())
				.build();
		repository.save(assignment);

		// Find by user ID and station ID
		Optional<UserStationAssignment> found = repository.findByUserIdAndStationId(user.getId(), station.getId());

		assertTrue(found.isPresent());
		assertEquals(assignment.getId(), found.get().getId());
		assertEquals(PermissionLevel.CUSTOM, found.get().getPermissionLevel());
	}

	@Test
	void existsByUserIdAndStationId_When_AssignmentExists_Expect_True() {
		// Create assignment
		UserStationAssignment assignment = UserStationAssignment.builder()
				.user(user)
				.station(station)
				.permissionLevel(PermissionLevel.FULL_ACCESS)
				.createdAt(LocalDateTime.now())
				.build();
		repository.save(assignment);

		// Check existence
		boolean exists = repository.existsByUserIdAndStationId(user.getId(), station.getId());

		assertTrue(exists);
	}

	@Test
	void existsByUserIdAndStationId_When_AssignmentDoesNotExist_Expect_False() {
		// Check existence without creating assignment
		boolean exists = repository.existsByUserIdAndStationId(user.getId(), station.getId());

		assertFalse(exists);
	}
}

