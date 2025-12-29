package com.onelpro.librelog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for clock template creation and update requests.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClockTemplateRequestDTO {

	@NotNull(message = "Channel ID is required")
	private UUID channelId;

	@NotBlank(message = "Name is required")
	private String name;

	private String description;

	@NotNull(message = "Is active flag is required")
	private Boolean isActive;

}

