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
 * ClockTemplate entity representing a master "Clock" builder.
 * A clock is a template for an hour of airtime that defines where content and commercial breaks occur.
 * 
 * Example structure:
 * - 00:00 - 15:00: Content
 * - 15:00 - 18:00: Commercial Break A (3 mins)
 * - 18:00 - 30:00: Content
 * - ... and so on
 * 
 * Clocks are channel-specific since different channels may have different formats.
 */
@Entity
@Table(name = "clock_templates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClockTemplate {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne
	@JoinColumn(name = "channel_id", nullable = false)
	private Channel channel;

	@Column(nullable = false)
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

