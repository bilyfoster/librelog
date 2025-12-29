package com.onelpro.librelog.repositories;

import com.onelpro.librelog.models.UserStationAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for UserStationAssignment entity operations.
 */
@Repository
public interface UserStationAssignmentRepository extends JpaRepository<UserStationAssignment, UUID> {

	/**
	 * Find all station assignments for a specific user.
	 */
	List<UserStationAssignment> findByUserId(UUID userId);

	/**
	 * Find all user assignments for a specific station.
	 */
	List<UserStationAssignment> findByStationId(UUID stationId);

	/**
	 * Find a specific user-station assignment.
	 */
	Optional<UserStationAssignment> findByUserIdAndStationId(UUID userId, UUID stationId);

	/**
	 * Check if a user-station assignment exists.
	 */
	boolean existsByUserIdAndStationId(UUID userId, UUID stationId);

	/**
	 * Find all assignments for multiple users (useful for bulk operations).
	 */
	List<UserStationAssignment> findByUserIdIn(List<UUID> userIds);

	/**
	 * Delete all assignments for a specific user.
	 */
	void deleteByUserId(UUID userId);

	/**
	 * Delete all assignments for a specific station.
	 */
	void deleteByStationId(UUID stationId);

}

