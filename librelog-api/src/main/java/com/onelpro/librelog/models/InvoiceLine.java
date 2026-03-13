package com.onelpro.librelog.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import java.util.UUID;

/**
 * Individual line item on an invoice.
 * Each line represents a spot or group of spots.
 */
@Entity
@Table(name = "invoice_lines", indexes = {
		@Index(name = "ix_invoice_lines_invoice", columnList = "invoice_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceLine {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "invoice_id", nullable = false)
	private Invoice invoice;

	@Column(name = "description", nullable = false)
	private String description;

	@Column(name = "spot_date")
	private LocalDate spotDate;

	@Column(name = "daypart")
	private String daypart;

	@Column(name = "spot_length_seconds")
	private Integer spotLengthSeconds;

	@Column(name = "quantity", nullable = false)
	private Integer quantity;

	@Column(name = "unit_price", precision = 19, scale = 2)
	private BigDecimal unitPrice;

	@Column(name = "line_total", precision = 19, scale = 2)
	private BigDecimal lineTotal;

	@Column(name = "spot_id")
	private UUID spotId;

	/**
	 * Calculate line total
	 */
	public void calculateLineTotal() {
		if (this.quantity != null && this.unitPrice != null) {
			this.lineTotal = this.unitPrice.multiply(new BigDecimal(this.quantity));
		}
	}

}
