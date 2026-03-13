package com.onelpro.librelog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for creating/updating spot copy.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpotCopyRequestDTO {

	@NotNull(message = "Campaign ID is required")
	private UUID campaignId;

	@NotBlank(message = "Copy title is required")
	private String title;

	@NotBlank(message = "Script text is required")
	private String scriptText;

	private String instructions;

	private Integer durationSeconds;

	private String status;

}
