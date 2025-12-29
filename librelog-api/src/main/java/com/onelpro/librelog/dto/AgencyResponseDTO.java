package com.onelpro.librelog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for agency response data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgencyResponseDTO {
	private UUID id;
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
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}

