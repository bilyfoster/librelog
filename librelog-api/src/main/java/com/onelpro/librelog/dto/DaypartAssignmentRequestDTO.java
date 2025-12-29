package com.onelpro.librelog.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for daypart assignment creation and update requests.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DaypartAssignmentRequestDTO {

	@NotNull(message = "Daypart ID is required")
	private UUID daypartId;

	@NotNull(message = "Clock Template ID is required")
	private UUID clockTemplateId;

}

