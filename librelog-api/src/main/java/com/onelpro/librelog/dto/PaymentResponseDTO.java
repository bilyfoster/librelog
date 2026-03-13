package com.onelpro.librelog.dto;

import com.onelpro.librelog.models.Payment.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for payment responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDTO {

	private UUID id;
	private UUID invoiceId;
	private String invoiceNumber;
	private BigDecimal amount;
	private LocalDate paymentDate;
	private PaymentMethod paymentMethod;
	private String referenceNumber;
	private String notes;
	private LocalDateTime createdAt;

}
