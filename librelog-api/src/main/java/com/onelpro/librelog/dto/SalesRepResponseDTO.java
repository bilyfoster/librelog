package com.onelpro.librelog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for sales representative response data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesRepResponseDTO {
	private UUID id;
	private String firstName;
	private String lastName;
	private String email;
	private String phone;
	private String employeeId;
	private UUID salesTeamId;
	private String salesTeamName;
	private UUID salesOfficeId;
	private String salesOfficeName;
	private UUID salesRegionId;
	private String salesRegionName;
	private BigDecimal commissionRate;
	private Boolean isActive;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}

