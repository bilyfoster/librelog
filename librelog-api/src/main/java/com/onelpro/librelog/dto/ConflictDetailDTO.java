package com.onelpro.librelog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * DTO for conflict detail information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConflictDetailDTO {
	private String conflictType; // OVERLAP, TIMING_CONFLICT, PRIORITY_CONFLICT, etc.
	private String elementType; // BREAK, FIXED_ASSET, AUTOMATION_COMMAND
	private String elementName;
	private LocalTime startTime;
	private LocalTime endTime;
	private String description;
	private String conflictingElementName;
	private LocalTime conflictingStartTime;
	private LocalTime conflictingEndTime;
}

