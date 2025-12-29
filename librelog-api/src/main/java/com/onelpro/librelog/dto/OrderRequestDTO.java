package com.onelpro.librelog.dto;

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
 * DTO for order creation and update requests.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequestDTO {

	@NotNull(message = "Station ID is required")
	private UUID stationId;

	@NotBlank(message = "Advertiser name is required")
	private String advertiserName;

	private String agencyName;

	private String salesRepName;

	@NotNull(message = "Start date is required")
	private LocalDate startDate;

	@NotNull(message = "End date is required")
	private LocalDate endDate;

	private Integer totalSpots;

	private BigDecimal totalAmount;

	private String notes;

}

