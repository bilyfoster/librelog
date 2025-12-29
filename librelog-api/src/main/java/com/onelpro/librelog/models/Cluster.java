package com.onelpro.librelog.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Cluster entity representing a group of stations that share infrastructure.
 * In broadcast, a cluster usually refers to a group of stations (e.g., 4 FM stations and 2 TV stations)
 * in one city that share a single front office, traffic team, and server infrastructure.
 * 
 * Part of the hierarchy: Organization -> Market -> Station -> Channel
 * Stations can optionally belong to a Cluster for operational grouping.
 */
@Entity
@Table(name = "clusters")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cluster {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne
	@JoinColumn(name = "organization_id", nullable = false)
	private Organization organization;

	@ManyToOne
	@JoinColumn(name = "market_id")
	private Market market;

	@Column(nullable = false)
	private String name;

	@Column(name = "description", columnDefinition = "TEXT")
	private String description;

	@Column(name = "is_active", nullable = false)
	private Boolean isActive;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

}

