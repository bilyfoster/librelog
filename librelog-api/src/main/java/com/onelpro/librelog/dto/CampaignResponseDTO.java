package com.onelpro.librelog.dto;

import com.onelpro.librelog.enums.CampaignStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for campaign responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignResponseDTO {

	private UUID id;
	private String name;
	private UUID stationId;
	private String stationName;
	private UUID advertiserId;
	private String advertiserName;
	private LocalDate startDate;
	private LocalDate endDate;
	private CampaignStatus status;
	private Integer totalSpots;
	private Integer spotsScheduled;
	private Integer spotsAired;
	private BigDecimal budget;
	private String notes;
	private String copyInstructions;
	private String salesRepName;
	private Integer priority;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

}
