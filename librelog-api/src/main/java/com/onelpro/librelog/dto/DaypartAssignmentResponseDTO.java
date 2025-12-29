package com.onelpro.librelog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for daypart assignment response data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DaypartAssignmentResponseDTO {

	private UUID id;
	private UUID daypartId;
	private String daypartName;
	private String daypartTimeRange; // e.g., "06:00 - 10:00"
	private UUID clockTemplateId;
	private String clockTemplateName;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

}

