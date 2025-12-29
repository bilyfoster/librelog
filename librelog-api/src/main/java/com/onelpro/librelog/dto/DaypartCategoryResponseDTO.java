package com.onelpro.librelog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for daypart category response data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DaypartCategoryResponseDTO {
	private UUID id;
	private String name;
	private String description;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}

