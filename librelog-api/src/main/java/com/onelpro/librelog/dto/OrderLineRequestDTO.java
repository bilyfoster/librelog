package com.onelpro.librelog.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * DTO for order line creation and update requests.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderLineRequestDTO {

	@NotNull(message = "Order ID is required")
	private UUID orderId;

	private UUID daypartId;

	@NotNull(message = "Spot length is required")
	private Integer spotLengthSeconds;

	@NotNull(message = "Quantity is required")
	private Integer quantity;

	private BigDecimal rate;

	private LocalDate startDate;

	private LocalDate endDate;

	private LocalTime startTime;

	private LocalTime endTime;

	private String notes;

}

