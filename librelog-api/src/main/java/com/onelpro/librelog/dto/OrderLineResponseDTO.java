package com.onelpro.librelog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * DTO for order line response data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderLineResponseDTO {
	private UUID id;
	private UUID orderId;
	private String orderNumber;
	private UUID daypartId;
	private String daypartName;
	private Integer spotLengthSeconds;
	private Integer quantity;
	private BigDecimal rate;
	private LocalDate startDate;
	private LocalDate endDate;
	private LocalTime startTime;
	private LocalTime endTime;
	private String notes;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}

