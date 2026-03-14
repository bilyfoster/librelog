package com.onelpro.librelog.dto;

import com.onelpro.librelog.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for order response data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDTO {
	private UUID id;
	private String orderNumber;
	private UUID stationId;
	private String stationCallSign;
	private String stationName;
	
	// Advertiser relationship
	private UUID advertiserId;
	private String advertiserName;
	
	// Agency and Sales Rep (auto-populated from advertiser or manually set)
	private UUID agencyId;
	private String agencyName;
	private UUID salesRepId;
	private String salesRepName;
	private OrderStatus status;
	private LocalDate startDate;
	private LocalDate endDate;
	private Integer totalSpots;
	private BigDecimal totalAmount;
	private String notes;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}

