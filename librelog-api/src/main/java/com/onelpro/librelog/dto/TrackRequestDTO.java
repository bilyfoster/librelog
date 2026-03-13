package com.onelpro.librelog.dto;

import com.onelpro.librelog.enums.TrackType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for creating/updating a track.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackRequestDTO {

	@NotBlank(message = "Track title is required")
	private String title;

	private String artist;

	private String album;

	@NotNull(message = "Track type is required")
	private TrackType type;

	private String genre;

	private Integer durationSeconds;

	private String filepath;

	private String libretimeId;

	private UUID stationId;

	private String isrc;

	private Integer year;

	private Integer bpm;

	private Integer rating;

}
