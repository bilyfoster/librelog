package com.onelpro.librelog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * DTO for creating/updating a voice track.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceTrackRequestDTO {

	@NotBlank(message = "Voice track title is required")
	private String title;

	@NotNull(message = "Station ID is required")
	private UUID stationId;

	private String showName;

	private String segmentType; // INTRO, OUTRO, TRANSITION, LINER, TEASER

	private String fileUrl;

	private String filePath;

	private Integer durationSeconds;

	private LocalDate scheduledDate;

	private LocalTime scheduledTime;

	private String scriptText;

	private String recordedText;

	private String status;

}
