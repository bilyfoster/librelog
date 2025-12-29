package com.onelpro.librelog.repositories;

import com.onelpro.librelog.models.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for UserSession entity operations.
 */
@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

	/**
	 * Find all sessions for a specific user.
	 */
	List<UserSession> findByUserId(UUID userId);

	/**
	 * Find all active sessions.
	 */
	List<UserSession> findByIsActiveTrue();

	/**
	 * Find all active sessions for a specific user.
	 */
	List<UserSession> findByUserIdAndIsActiveTrue(UUID userId);

	/**
	 * Find a session by session token.
	 */
	Optional<UserSession> findBySessionToken(String sessionToken);

	/**
	 * Find all expired sessions (sessions where expiresAt is before the given time).
	 */
	List<UserSession> findByExpiresAtBefore(LocalDateTime time);

	/**
	 * Delete all expired sessions.
	 */
	@Modifying
	@Query("DELETE FROM UserSession s WHERE s.expiresAt < :time")
	void deleteByExpiresAtBefore(@Param("time") LocalDateTime time);

	/**
	 * Find all sessions that have expired (isActive = true but expiresAt is in the past).
	 */
	@Query("SELECT s FROM UserSession s WHERE s.isActive = true AND s.expiresAt < :time")
	List<UserSession> findExpiredActiveSessions(@Param("time") LocalDateTime time);

	/**
	 * Find all active sessions for a specific station (users currently editing that station's log).
	 */
	List<UserSession> findByCurrentStationIdAndIsActiveTrue(UUID stationId);

	/**
	 * Count active sessions for a specific user.
	 */
	long countByUserIdAndIsActiveTrue(UUID userId);

}

