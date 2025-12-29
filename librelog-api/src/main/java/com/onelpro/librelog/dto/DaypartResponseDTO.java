package com.onelpro.librelog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * DTO for daypart response data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DaypartResponseDTO {
	private UUID id;
	private String name;
	private LocalTime startTime;
	private LocalTime endTime;
	private UUID categoryId;
	private String categoryName;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}

