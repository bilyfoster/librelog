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
 * DaypartAssignment entity representing the assignment of clock templates to dayparts.
 * This creates the template hierarchy: Daypart -> Clock Template.
 * A daypart (e.g., "Morning Drive 6 AM - 10 AM") can use a specific clock template.
 */
@Entity
@Table(name = "daypart_assignments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DaypartAssignment {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne
	@JoinColumn(name = "daypart_id", nullable = false)
	private Daypart daypart;

	@ManyToOne
	@JoinColumn(name = "clock_template_id", nullable = false)
	private ClockTemplate clockTemplate;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

}

