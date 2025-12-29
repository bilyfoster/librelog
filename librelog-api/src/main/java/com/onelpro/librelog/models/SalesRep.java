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

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * SalesRep entity representing a sales representative.
 */
@Entity
@Table(name = "sales_reps")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesRep {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "first_name", nullable = false)
	private String firstName;

	@Column(name = "last_name", nullable = false)
	private String lastName;

	@Column(name = "email", nullable = false, unique = true)
	private String email;

	@Column(name = "phone")
	private String phone;

	@Column(name = "employee_id")
	private String employeeId;

	@Column(name = "sales_team_id")
	private UUID salesTeamId;

	@Column(name = "sales_office_id")
	private UUID salesOfficeId;

	@Column(name = "sales_region_id")
	private UUID salesRegionId;

	@Column(name = "commission_rate", precision = 5, scale = 2)
	private java.math.BigDecimal commissionRate;

	@Column(name = "is_active", nullable = false)
	private Boolean isActive;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

}

