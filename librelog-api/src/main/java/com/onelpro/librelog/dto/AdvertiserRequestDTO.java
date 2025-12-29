package com.onelpro.librelog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for advertiser creation and update requests.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvertiserRequestDTO {

	@NotBlank(message = "Name is required")
	private String name;

	private String legalName;

	private String taxId;

	private BigDecimal creditLimit;

	private String paymentTerms;

	private String addressLine1;

	private String addressLine2;

	private String city;

	private String state;

	private String zipCode;

	private String country;

	private String phone;

	private String email;

	private String website;

	private UUID agencyId;

	private UUID salesRepId;

	private String notes;

	private Boolean isActive;

}

