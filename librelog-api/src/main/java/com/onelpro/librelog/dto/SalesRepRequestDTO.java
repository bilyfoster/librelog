package com.onelpro.librelog.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for sales representative creation and update requests.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesRepRequestDTO {

	@NotBlank(message = "First name is required")
	private String firstName;

	@NotBlank(message = "Last name is required")
	private String lastName;

	@NotBlank(message = "Email is required")
	@Email(message = "Email must be valid")
	private String email;

	private String phone;

	private String employeeId;

	private UUID salesTeamId;

	private UUID salesOfficeId;

	private UUID salesRegionId;

	private BigDecimal commissionRate;

	private Boolean isActive;

}

