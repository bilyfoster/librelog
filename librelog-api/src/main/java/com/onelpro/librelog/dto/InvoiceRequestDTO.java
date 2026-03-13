package com.onelpro.librelog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO for creating an invoice.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceRequestDTO {

	@NotNull(message = "Campaign ID is required")
	private UUID campaignId;

	@NotNull(message = "Advertiser ID is required")
	private UUID advertiserId;

	@NotNull(message = "Invoice date is required")
	private LocalDate invoiceDate;

	@NotNull(message = "Due date is required")
	private LocalDate dueDate;

	private BigDecimal taxRate;

	private String notes;

	private List<InvoiceLineRequestDTO> lines;

}
