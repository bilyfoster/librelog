package com.onelpro.librelog.dto;

import com.onelpro.librelog.models.Payment.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for creating a payment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestDTO {

	@NotNull(message = "Invoice ID is required")
	private UUID invoiceId;

	@NotNull(message = "Payment amount is required")
	private BigDecimal amount;

	@NotNull(message = "Payment date is required")
	private LocalDate paymentDate;

	@NotNull(message = "Payment method is required")
	private PaymentMethod paymentMethod;

	private String referenceNumber;

	private String notes;

}
