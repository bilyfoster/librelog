package com.onelpro.librelog.models;

import com.onelpro.librelog.enums.OrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Order entity representing a broadcast advertising order.
 */
@Entity
@Table(name = "orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "order_number", nullable = false, unique = true)
	private String orderNumber;

	@ManyToOne
	@JoinColumn(name = "station_id", nullable = false)
	private Station station;

	/**
	 * Reference to the advertiser entity.
	 * When set, advertiserName, agencyName, and salesRepName are auto-populated.
	 */
	@ManyToOne
	@JoinColumn(name = "advertiser_id")
	private Advertiser advertiser;

	/**
	 * Denormalized advertiser name for display purposes.
	 * Auto-populated from advertiser.name when advertiser is set.
	 */
	@Column(name = "advertiser_name", nullable = false)
	private String advertiserName;

	/**
	 * Denormalized agency name for display purposes.
	 * Auto-populated from advertiser's agency when advertiser is set.
	 */
	@Column(name = "agency_name")
	private String agencyName;

	/**
	 * Denormalized sales rep name for display purposes.
	 * Auto-populated from advertiser's sales rep when advertiser is set.
	 */
	@Column(name = "sales_rep_name")
	private String salesRepName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private OrderStatus status;

	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;

	@Column(name = "end_date", nullable = false)
	private LocalDate endDate;

	@Column(name = "total_spots")
	private Integer totalSpots;

	@Column(name = "total_amount", precision = 19, scale = 2)
	private java.math.BigDecimal totalAmount;

	@Column(name = "notes", columnDefinition = "TEXT")
	private String notes;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

}

