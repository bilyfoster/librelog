package com.onelpro.librelog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for organization creation and update requests.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationRequestDTO {

	@NotBlank(message = "Name is required")
	private String name;

	private String legalName;

	private String taxId;

	private String addressLine1;

	private String addressLine2;

	private String city;

	private String state;

	private String zipCode;

	private String country;

	private String phone;

	private String email;

	private String website;

	@NotNull(message = "Is active flag is required")
	private Boolean isActive;
}

