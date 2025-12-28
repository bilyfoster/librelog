package com.onelpro.librelog.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * AvailType entity representing commercial break categorization.
 * Used to tag commercial breaks with specific avail types (e.g., "Weather Sponsor Only",
 * "Sports Content Only") so the auto-placer knows which types of ads are allowed.
 */
@Entity
@Table(name = "avail_types")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailType {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, unique = true)
	private String name;

	@Column(name = "description")
	private String description;

	@Column(name = "is_active", nullable = false)
	private Boolean isActive;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

}

