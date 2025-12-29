package com.onelpro.librelog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.UUID;

/**
 * DTO for daypart creation and update requests.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DaypartRequestDTO {

	@NotBlank(message = "Name is required")
	private String name;

	@NotNull(message = "Start time is required")
	private LocalTime startTime;

	@NotNull(message = "End time is required")
	private LocalTime endTime;

	private UUID categoryId;

}

