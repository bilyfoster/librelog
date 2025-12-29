package com.onelpro.librelog.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for requesting user impersonation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImpersonationRequestDTO {

	@NotNull(message = "Target user ID is required")
	private UUID targetUserId;

}

