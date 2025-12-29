package com.onelpro.librelog.models;

import com.onelpro.librelog.enums.FormatType;
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
 * Channel entity representing a channel/stream within a station.
 * A station can have multiple outputs (e.g., HD-1, HD-2, Mobile Stream, Podcast Feed).
 * Part of the hierarchy: Organization -> Market -> Station -> Channel
 * 
 * Each channel has a format type:
 * - LINEAR: Traditional broadcast with 24-hour clock and breaks
 * - DIGITAL: Online/OTT with impression-based inventory
 * - PODCAST: Mix of baked-in and dynamic insertion
 */
@Entity
@Table(name = "channels")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Channel {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne
	@JoinColumn(name = "station_id", nullable = false)
	private Station station;

	@Column(nullable = false)
	private String name;

	@Column(name = "channel_number")
	private Integer channelNumber;

	@Enumerated(EnumType.STRING)
	@Column(name = "format_type", nullable = false)
	private FormatType formatType;

	@Column(name = "description")
	private String description;

	@Column(name = "is_active", nullable = false)
	private Boolean isActive;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

}

