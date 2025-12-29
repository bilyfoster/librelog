package com.onelpro.librelog.repositories;

import com.onelpro.librelog.enums.UserStatus;
import com.onelpro.librelog.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for User entity.
 * Provides data access operations for user management.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user by username.
     *
     * @param username the username to search for
     * @return Optional containing the user if found, empty otherwise
     */
    Optional<User> findByUsername(String username);

    /**
     * Checks if a user exists with the given username.
     *
     * @param username the username to check
     * @return true if a user exists with the username, false otherwise
     */
    boolean existsByUsername(String username);

    /**
     * Finds all users with the given status.
     *
     * @param status the status to filter by
     * @return list of users with the specified status
     */
    java.util.List<User> findByStatus(UserStatus status);
}

