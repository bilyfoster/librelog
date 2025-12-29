package com.onelpro.librelog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for market creation and update requests.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketRequestDTO {

	@NotNull(message = "Organization ID is required")
	private UUID organizationId;

	@NotBlank(message = "Name is required")
	private String name;

	private String description;

	private String city;

	private String state;

	private String country;

	private String timezone;

	@NotNull(message = "Is active flag is required")
	private Boolean isActive;
}

