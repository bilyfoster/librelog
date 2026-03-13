package com.onelpro.librelog.dto;

import com.onelpro.librelog.models.Invoice.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for invoice responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponseDTO {

	private UUID id;
	private String invoiceNumber;
	private UUID campaignId;
	private String campaignName;
	private UUID advertiserId;
	private String advertiserName;
	private LocalDate invoiceDate;
	private LocalDate dueDate;
	private InvoiceStatus status;
	private BigDecimal subtotal;
	private BigDecimal taxRate;
	private BigDecimal taxAmount;
	private BigDecimal totalAmount;
	private BigDecimal amountPaid;
	private BigDecimal balanceDue;
	private String notes;
	private List<InvoiceLineResponseDTO> lines;
	private List<PaymentResponseDTO> payments;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

}
