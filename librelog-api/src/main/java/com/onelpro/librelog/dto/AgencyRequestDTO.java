package com.onelpro.librelog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for agency creation and update requests.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgencyRequestDTO {

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

	private String notes;

	private Boolean isActive;

}

