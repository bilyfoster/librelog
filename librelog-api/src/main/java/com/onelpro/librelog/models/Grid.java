package com.onelpro.librelog.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Grid entity representing weekly schedule management.
 * A grid assigns dayparts to specific days of the week, creating the weekly schedule.
 * Grids distinguish between weekday and weekend schedules.
 */
@Entity
@Table(name = "grids")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Grid {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false)
	private String name;

	@ManyToOne
	@JoinColumn(name = "channel_id", nullable = false)
	private Channel channel;

	@Column(name = "description")
	private String description;

	@Column(name = "is_active", nullable = false)
	private Boolean isActive;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

}

