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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Invoice entity for billing advertisers.
 * Invoices are generated based on campaign spot airings.
 */
@Entity
@Table(name = "invoices", indexes = {
		@Index(name = "ix_invoices_campaign", columnList = "campaign_id"),
		@Index(name = "ix_invoices_advertiser", columnList = "advertiser_id"),
		@Index(name = "ix_invoices_status", columnList = "status"),
		@Index(name = "ix_invoices_due_date", columnList = "due_date")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "invoice_number", nullable = false, unique = true)
	private String invoiceNumber;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "campaign_id", nullable = false)
	private Campaign campaign;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "advertiser_id", nullable = false)
	private Advertiser advertiser;

	@Column(name = "invoice_date", nullable = false)
	private LocalDate invoiceDate;

	@Column(name = "due_date", nullable = false)
	private LocalDate dueDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	@Builder.Default
	private InvoiceStatus status = InvoiceStatus.DRAFT;

	@Column(name = "subtotal", precision = 19, scale = 2)
	private BigDecimal subtotal;

	@Column(name = "tax_rate", precision = 5, scale = 4)
	private BigDecimal taxRate;

	@Column(name = "tax_amount", precision = 19, scale = 2)
	private BigDecimal taxAmount;

	@Column(name = "total_amount", precision = 19, scale = 2)
	private BigDecimal totalAmount;

	@Column(name = "amount_paid", precision = 19, scale = 2)
	@Builder.Default
	private BigDecimal amountPaid = BigDecimal.ZERO;

	@Column(name = "balance_due", precision = 19, scale = 2)
	private BigDecimal balanceDue;

	@Column(name = "notes", columnDefinition = "TEXT")
	private String notes;

	@OneToMany(mappedBy = "invoice")
	@Builder.Default
	private List<InvoiceLine> lines = new ArrayList<>();

	@OneToMany(mappedBy = "invoice")
	@Builder.Default
	private List<Payment> payments = new ArrayList<>();

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	/**
	 * Invoice status enum
	 */
	public enum InvoiceStatus {
		DRAFT,
		SENT,
		PARTIALLY_PAID,
		PAID,
		OVERDUE,
		CANCELLED
	}

	/**
	 * Calculate totals from invoice lines
	 */
	public void calculateTotals() {
		this.subtotal = lines.stream()
				.map(InvoiceLine::getLineTotal)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		
		if (this.taxRate != null && this.taxRate.compareTo(BigDecimal.ZERO) > 0) {
			this.taxAmount = this.subtotal.multiply(this.taxRate);
		} else {
			this.taxAmount = BigDecimal.ZERO;
		}
		
		this.totalAmount = this.subtotal.add(this.taxAmount);
		this.balanceDue = this.totalAmount.subtract(this.amountPaid != null ? this.amountPaid : BigDecimal.ZERO);
		
		// Update status based on payment
		if (this.balanceDue.compareTo(BigDecimal.ZERO) <= 0) {
			this.status = InvoiceStatus.PAID;
		} else if (this.amountPaid != null && this.amountPaid.compareTo(BigDecimal.ZERO) > 0) {
			this.status = InvoiceStatus.PARTIALLY_PAID;
		}
	}

}
