package com.onelpro.librelog.dto;

import com.onelpro.librelog.enums.UserRole;
import com.onelpro.librelog.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for user response data.
 * Excludes sensitive information like password hash.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private UUID id;
    private String username;
    private UserRole role;
    private UserStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastLogin;
}

