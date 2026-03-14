package com.onelpro.librelog.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for login request.
 * Supports both 'email' and 'username' fields for compatibility.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDTO {

	/**
	 * User's email address (preferred field).
	 * Either email or username must be provided.
	 */
	@Email(message = "Email must be valid")
	private String email;

	/**
	 * Alternative field name for email (for backward compatibility).
	 * Either email or username must be provided.
	 */
	private String username;

	@NotBlank(message = "Password is required")
	private String password;

	/**
	 * Returns the email if set, otherwise returns username.
	 * This allows clients to use either field.
	 */
	public String getEffectiveEmail() {
		if (email != null && !email.trim().isEmpty()) {
			return email;
		}
		return username;
	}

}

