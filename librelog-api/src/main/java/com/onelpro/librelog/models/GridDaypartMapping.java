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
 * GridDaypartMapping entity representing weekly schedule assignments.
 * Maps dayparts to specific days of the week within a grid.
 * Day of week: 1 = Monday, 2 = Tuesday, ..., 7 = Sunday
 */
@Entity
@Table(name = "grid_daypart_mappings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GridDaypartMapping {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne
	@JoinColumn(name = "grid_id", nullable = false)
	private Grid grid;

	@ManyToOne
	@JoinColumn(name = "daypart_id", nullable = false)
	private Daypart daypart;

	@Column(name = "day_of_week", nullable = false)
	private Integer dayOfWeek;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

}

