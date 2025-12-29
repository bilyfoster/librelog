package com.onelpro.librelog.dto;

import com.onelpro.librelog.enums.UserRole;
import com.onelpro.librelog.enums.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating or updating a user.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRequestDTO {

	@NotBlank(message = "Email is required")
	@Email(message = "Email must be valid")
	private String email;

	private String password; // Optional for updates

	@NotNull(message = "Status is required")
	private UserStatus status;

	@NotNull(message = "Role is required")
	private UserRole role;

}

