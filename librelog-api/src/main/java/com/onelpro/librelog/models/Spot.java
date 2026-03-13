package com.onelpro.librelog.models;

import com.onelpro.librelog.enums.SpotStatus;
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
 * Entity representing an individual spot/spot occurrence in the traffic system.
 * Spots are scheduled instances of a campaign.
 */
@Entity
@Table(name = "spots", indexes = {
		@Index(name = "ix_spots_campaign", columnList = "campaign_id"),
		@Index(name = "ix_spots_station", columnList = "station_id"),
		@Index(name = "ix_spots_date", columnList = "scheduled_date"),
		@Index(name = "ix_spots_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Spot {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "campaign_id", nullable = false)
	private Campaign campaign;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "station_id", nullable = false)
	private Station station;

	@Column(name = "scheduled_date", nullable = false)
	private LocalDate scheduledDate;

	@Column(name = "scheduled_time", nullable = false, length = 8)
	private String scheduledTime;

	@Column(name = "spot_length", nullable = false)
	private Integer spotLength;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	@Builder.Default
	private SpotStatus status = SpotStatus.SCHEDULED;

	@Column(name = "actual_air_time")
	private LocalDateTime actualAirTime;

	@Column(name = "daypart")
	private String daypart;

	@Column(name = "break_name")
	private String breakName;

	@Column(name = "break_position")
	private Integer breakPosition;

	@Column(name = "asset_id")
	private UUID assetId;

	@Column(name = "asset_name")
	private String assetName;

	@Column(name = "makegood_of_id")
	private UUID makegoodOfId;

	@Column(name = "notes", columnDefinition = "TEXT")
	private String notes;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

}
