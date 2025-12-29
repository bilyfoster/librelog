package com.onelpro.librelog.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Advertiser entity representing a client who purchases advertising.
 */
@Entity
@Table(name = "advertisers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Advertiser {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false)
	private String name;

	@Column(name = "legal_name")
	private String legalName;

	@Column(name = "tax_id")
	private String taxId;

	@Column(name = "credit_limit", precision = 19, scale = 2)
	private BigDecimal creditLimit;

	@Column(name = "payment_terms")
	private String paymentTerms;

	@Column(name = "address_line1")
	private String addressLine1;

	@Column(name = "address_line2")
	private String addressLine2;

	@Column(name = "city")
	private String city;

	@Column(name = "state")
	private String state;

	@Column(name = "zip_code")
	private String zipCode;

	@Column(name = "country")
	private String country;

	@Column(name = "phone")
	private String phone;

	@Column(name = "email")
	private String email;

	@Column(name = "website")
	private String website;

	@Column(name = "agency_id")
	private UUID agencyId;

	@Column(name = "sales_rep_id")
	private UUID salesRepId;

	@Column(name = "notes", columnDefinition = "TEXT")
	private String notes;

	@Column(name = "is_active", nullable = false)
	private Boolean isActive;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

}

