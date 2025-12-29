package com.onelpro.librelog.models;

import com.onelpro.librelog.enums.PermissionLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Entity representing the assignment of a user to a cluster with specific permission levels.
 * Cluster-level permissions automatically inherit to all stations within that cluster.
 * This is prepared for Phase 2 implementation.
 */
@Entity
@Table(name = "user_cluster_assignments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserClusterAssignment {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne
	@JoinColumn(name = "cluster_id", nullable = false)
	private Cluster cluster;

	@Enumerated(EnumType.STRING)
	@Column(name = "permission_level", nullable = false)
	private PermissionLevel permissionLevel;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "custom_permissions", columnDefinition = "jsonb")
	private Map<String, Object> customPermissions;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

}

