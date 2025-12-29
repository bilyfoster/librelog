package com.onelpro.librelog.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Entity representing a custom role with granular permissions defined per module and action.
 * Custom roles allow administrators to create role variations beyond the predefined system roles.
 */
@Entity
@Table(name = "custom_roles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomRole {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, unique = true)
	private String name;

	@Column(columnDefinition = "TEXT")
	private String description;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "permissions", nullable = false, columnDefinition = "jsonb")
	private Map<String, Object> permissions;

	@ManyToOne
	@JoinColumn(name = "created_by_user_id", nullable = false)
	private User createdByUser;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

}

