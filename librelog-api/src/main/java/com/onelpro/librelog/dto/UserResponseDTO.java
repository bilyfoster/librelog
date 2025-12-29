package com.onelpro.librelog.dto;

import com.onelpro.librelog.enums.UserRole;
import com.onelpro.librelog.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for user response data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {

	private UUID id;
	private String email;
	private UserStatus status;
	private UserRole role;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

}
