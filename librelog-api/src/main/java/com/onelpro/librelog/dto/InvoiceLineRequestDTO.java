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
 * DTO for creating an invoice line item.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceLineRequestDTO {

	@NotBlank(message = "Description is required")
	private String description;

	private LocalDate spotDate;

	private String daypart;

	private Integer spotLengthSeconds;

	@NotNull(message = "Quantity is required")
	private Integer quantity;

	@NotNull(message = "Unit price is required")
	private BigDecimal unitPrice;

	private UUID spotId;

}
