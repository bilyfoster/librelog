package com.onelpro.librelog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * DTO for voice track responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceTrackResponseDTO {

	private UUID id;
	private String title;
	private UUID stationId;
	private String stationName;
	private String showName;
	private String segmentType;
	private String fileUrl;
	private String filePath;
	private Integer durationSeconds;
	private LocalDate scheduledDate;
	private LocalTime scheduledTime;
	private String scriptText;
	private String recordedText;
	private String status;
	private UUID createdById;
	private String createdByName;
	private UUID recordedById;
	private String recordedByName;
	private LocalDateTime recordedAt;

	/**
	 * Song context for DJ recording.
	 * songBefore = song playing before this voice track
	 * songAfter = song playing after this voice track
	 */
	private UUID songBeforeId;
	private String songBeforeTitle;
	private String songBeforeArtist;
	private UUID songAfterId;
	private String songAfterTitle;
	private String songAfterArtist;

	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

}
