package com.onelpro.librelog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for grid creation and update requests.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GridRequestDTO {

	@NotBlank(message = "Name is required")
	private String name;

	@NotNull(message = "Channel ID is required")
	private UUID channelId;

	private String description;

	@NotNull(message = "Is active flag is required")
	private Boolean isActive;

}

