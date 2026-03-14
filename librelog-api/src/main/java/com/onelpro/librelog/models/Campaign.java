package com.onelpro.librelog.models;

import com.onelpro.librelog.enums.CampaignStatus;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing an advertising campaign in the traffic system.
 * Campaigns group spots together for scheduling and billing.
 */
@Entity
@Table(name = "campaigns", indexes = {
		@Index(name = "ix_campaigns_advertiser", columnList = "advertiser_id"),
		@Index(name = "ix_campaigns_station", columnList = "station_id"),
		@Index(name = "ix_campaigns_status", columnList = "status"),
		@Index(name = "ix_campaigns_dates", columnList = "start_date, end_date")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Campaign {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "name", nullable = false)
	private String name;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "station_id", nullable = false)
	private Station station;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "advertiser_id")
	private Advertiser advertiser;

	@Column(name = "advertiser_name", nullable = false)
	private String advertiserName;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "order_id")
	private Order order;

	@Column(name = "order_id", insertable = false, updatable = false)
	private UUID orderId;

	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;

	@Column(name = "end_date", nullable = false)
	private LocalDate endDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	@Builder.Default
	private CampaignStatus status = CampaignStatus.DRAFT;

	@Column(name = "total_spots")
	@Builder.Default
	private Integer totalSpots = 0;

	@Column(name = "spots_scheduled")
	@Builder.Default
	private Integer spotsScheduled = 0;

	@Column(name = "spots_aired")
	@Builder.Default
	private Integer spotsAired = 0;

	@Column(name = "budget", precision = 19, scale = 2)
	private java.math.BigDecimal budget;

	@Column(name = "notes", columnDefinition = "TEXT")
	private String notes;

	@Column(name = "copy_instructions", columnDefinition = "TEXT")
	private String copyInstructions;

	@Column(name = "sales_rep_name")
	private String salesRepName;

	@Column(name = "priority")
	private Integer priority;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

}
