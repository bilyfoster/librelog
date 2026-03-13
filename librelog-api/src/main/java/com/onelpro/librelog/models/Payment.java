package com.onelpro.librelog.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payment entity for tracking invoice payments.
 */
@Entity
@Table(name = "payments", indexes = {
		@Index(name = "ix_payments_invoice", columnList = "invoice_id"),
		@Index(name = "ix_payments_date", columnList = "payment_date")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "invoice_id", nullable = false)
	private Invoice invoice;

	@Column(name = "amount", precision = 19, scale = 2, nullable = false)
	private BigDecimal amount;

	@Column(name = "payment_date", nullable = false)
	private LocalDate paymentDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "payment_method", nullable = false)
	private PaymentMethod paymentMethod;

	@Column(name = "reference_number")
	private String referenceNumber;

	@Column(name = "notes", columnDefinition = "TEXT")
	private String notes;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	/**
	 * Payment method enum
	 */
	public enum PaymentMethod {
		CASH,
		CHECK,
		CREDIT_CARD,
		DEBIT_CARD,
		ACH,
		WIRE_TRANSFER,
		ONLINE_PAYMENT,
		OTHER
	}

}
