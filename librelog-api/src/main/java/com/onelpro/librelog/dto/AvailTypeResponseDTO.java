package com.onelpro.librelog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for avail type response data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailTypeResponseDTO {
	private UUID id;
	private String name;
	private String description;
	private Boolean isActive;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}

