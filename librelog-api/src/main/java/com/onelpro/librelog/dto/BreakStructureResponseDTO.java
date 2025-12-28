package com.onelpro.librelog.dto;

import com.onelpro.librelog.enums.TimingType;
import com.onelpro.librelog.enums.TransitionCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * DTO for break structure response data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BreakStructureResponseDTO {
	private UUID id;
	private UUID clockTemplateId;
	private String clockTemplateName;
	private String name;
	private LocalTime startTime;
	private Integer durationSeconds;
	private Boolean isFloating;
	private UUID availTypeId;
	private String availTypeName;
	private TimingType timingType;
	private TransitionCode transitionCode;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}

