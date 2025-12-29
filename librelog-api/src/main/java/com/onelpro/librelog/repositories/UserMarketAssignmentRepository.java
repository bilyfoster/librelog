package com.onelpro.librelog.repositories;

import com.onelpro.librelog.models.UserMarketAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for UserMarketAssignment entity operations.
 * Prepared for Phase 2 implementation.
 */
@Repository
public interface UserMarketAssignmentRepository extends JpaRepository<UserMarketAssignment, UUID> {

	/**
	 * Find all market assignments for a specific user.
	 */
	List<UserMarketAssignment> findByUserId(UUID userId);

	/**
	 * Find all user assignments for a specific market.
	 */
	List<UserMarketAssignment> findByMarketId(UUID marketId);

	/**
	 * Find a specific user-market assignment.
	 */
	Optional<UserMarketAssignment> findByUserIdAndMarketId(UUID userId, UUID marketId);

	/**
	 * Check if a user-market assignment exists.
	 */
	boolean existsByUserIdAndMarketId(UUID userId, UUID marketId);

	/**
	 * Find all assignments for multiple users (useful for bulk operations).
	 */
	List<UserMarketAssignment> findByUserIdIn(List<UUID> userIds);

	/**
	 * Delete all assignments for a specific user.
	 */
	void deleteByUserId(UUID userId);

	/**
	 * Delete all assignments for a specific market.
	 */
	void deleteByMarketId(UUID marketId);

}

