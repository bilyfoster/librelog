package com.onelpro.librelog.repositories;

import com.onelpro.librelog.models.FailedLoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for FailedLoginAttempt entity operations.
 */
@Repository
public interface FailedLoginAttemptRepository extends JpaRepository<FailedLoginAttempt, UUID> {

    /**
     * Finds all failed login attempts for a given username.
     *
     * @param username the username to search for
     * @return list of failed login attempts
     */
    List<FailedLoginAttempt> findByUsername(String username);

    /**
     * Counts failed login attempts for a username within a time window.
     *
     * @param username the username to check
     * @param since the start time of the window
     * @return count of failed login attempts
     */
    @Query("SELECT COUNT(f) FROM FailedLoginAttempt f WHERE f.username = :username AND f.attemptedAt >= :since")
    long countByUsernameAndAttemptedAtAfter(@Param("username") String username, @Param("since") Instant since);

    /**
     * Deletes failed login attempts older than the specified time.
     *
     * @param before the time threshold
     * @return number of deleted records
     */
    @Modifying
    @Query("DELETE FROM FailedLoginAttempt f WHERE f.attemptedAt < :before")
    long deleteByAttemptedAtBefore(@Param("before") Instant before);
}

