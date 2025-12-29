package com.onelpro.librelog.repositories;

import com.onelpro.librelog.models.UserClusterAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for UserClusterAssignment entity operations.
 * Prepared for Phase 2 implementation.
 */
@Repository
public interface UserClusterAssignmentRepository extends JpaRepository<UserClusterAssignment, UUID> {

	/**
	 * Find all cluster assignments for a specific user.
	 */
	List<UserClusterAssignment> findByUserId(UUID userId);

	/**
	 * Find all user assignments for a specific cluster.
	 */
	List<UserClusterAssignment> findByClusterId(UUID clusterId);

	/**
	 * Find a specific user-cluster assignment.
	 */
	Optional<UserClusterAssignment> findByUserIdAndClusterId(UUID userId, UUID clusterId);

	/**
	 * Check if a user-cluster assignment exists.
	 */
	boolean existsByUserIdAndClusterId(UUID userId, UUID clusterId);

	/**
	 * Find all assignments for multiple users (useful for bulk operations).
	 */
	List<UserClusterAssignment> findByUserIdIn(List<UUID> userIds);

	/**
	 * Delete all assignments for a specific user.
	 */
	void deleteByUserId(UUID userId);

	/**
	 * Delete all assignments for a specific cluster.
	 */
	void deleteByClusterId(UUID clusterId);

}

