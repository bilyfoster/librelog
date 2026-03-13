package com.onelpro.librelog.dto;

import com.onelpro.librelog.enums.TrackType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for track responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackResponseDTO {

	private UUID id;
	private String title;
	private String artist;
	private String album;
	private TrackType type;
	private String genre;
	private Integer durationSeconds;
	private String filepath;
	private String libretimeId;
	private UUID stationId;
	private String stationName;
	private String isrc;
	private Integer year;
	private Integer bpm;
	private Integer rating;
	private Integer playCount;
	private LocalDateTime lastPlayed;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

}
