package com.onelpro.librelog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for spot copy responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpotCopyResponseDTO {

	private UUID id;
	private UUID campaignId;
	private String campaignName;
	private Integer versionNumber;
	private String title;
	private String scriptText;
	private String instructions;
	private Integer durationSeconds;
	private String status;
	private UUID createdById;
	private String createdByName;
	private UUID approvedById;
	private String approvedByName;
	private LocalDateTime approvedAt;
	private String rejectionReason;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

}
