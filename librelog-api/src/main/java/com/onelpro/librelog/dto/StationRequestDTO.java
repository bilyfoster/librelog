package com.onelpro.librelog.dto;

import com.onelpro.librelog.enums.StationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for station creation and update requests.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StationRequestDTO {

	@NotNull(message = "Organization ID is required")
	private UUID organizationId;

	private UUID marketId;

	private UUID clusterId;

	@NotBlank(message = "Call sign is required")
	private String callSign;

	@NotBlank(message = "Name is required")
	private String name;

	private String frequency;

	@NotNull(message = "Station type is required")
	private StationType stationType;

	@NotNull(message = "Is active flag is required")
	private Boolean isActive;

}

