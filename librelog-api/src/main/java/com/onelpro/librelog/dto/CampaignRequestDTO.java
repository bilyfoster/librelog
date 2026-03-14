package com.onelpro.librelog.dto;

import com.onelpro.librelog.enums.CampaignStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for creating/updating a campaign.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignRequestDTO {

	@NotBlank(message = "Campaign name is required")
	private String name;

	@NotNull(message = "Station ID is required")
	private UUID stationId;

	private UUID advertiserId;

	private String advertiserName;

	private UUID orderId;

	@NotNull(message = "Start date is required")
	private LocalDate startDate;

	@NotNull(message = "End date is required")
	private LocalDate endDate;

	private CampaignStatus status;

	private Integer totalSpots;

	private BigDecimal budget;

	private String notes;

	private String copyInstructions;

	private String salesRepName;

	private Integer priority;

}
