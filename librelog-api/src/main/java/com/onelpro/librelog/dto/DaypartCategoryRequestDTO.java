package com.onelpro.librelog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for daypart category creation and update requests.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DaypartCategoryRequestDTO {

	@NotBlank(message = "Name is required")
	private String name;

	private String description;

}

