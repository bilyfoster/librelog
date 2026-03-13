package com.onelpro.librelog.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating the current user's profile.
 * Used by users to update their own profile (not admin functions).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileUpdateRequestDTO {

	@Email(message = "Please provide a valid email address")
	private String email;

	@Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
	private String currentPassword;

	@Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
	private String newPassword;

}
