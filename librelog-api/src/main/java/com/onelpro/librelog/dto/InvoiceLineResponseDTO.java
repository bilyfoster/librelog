package com.onelpro.librelog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for invoice line responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceLineResponseDTO {

	private UUID id;
	private String description;
	private LocalDate spotDate;
	private String daypart;
	private Integer spotLengthSeconds;
	private Integer quantity;
	private BigDecimal unitPrice;
	private BigDecimal lineTotal;
	private UUID spotId;

}
