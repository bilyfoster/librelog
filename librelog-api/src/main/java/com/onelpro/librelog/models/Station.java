package com.onelpro.librelog.models;

import com.onelpro.librelog.enums.StationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Station entity representing a broadcast station (the "Property").
 * Part of the hierarchy: Organization -> Market -> Station -> Channel
 */
@Entity
@Table(name = "stations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Station {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne
	@JoinColumn(name = "organization_id", nullable = false)
	private Organization organization;

	@ManyToOne
	@JoinColumn(name = "market_id")
	private Market market;

	@ManyToOne
	@JoinColumn(name = "cluster_id")
	private Cluster cluster;

	@Column(nullable = false, unique = true)
	private String callSign;

	@Column(nullable = false)
	private String name;

	@Column(name = "frequency")
	private String frequency;

	@Enumerated(EnumType.STRING)
	@Column(name = "station_type", nullable = false)
	private StationType stationType;

	@Column(name = "is_active", nullable = false)
	private Boolean isActive;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

}

