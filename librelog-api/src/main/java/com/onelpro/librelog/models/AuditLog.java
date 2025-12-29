package com.onelpro.librelog.models;

import com.onelpro.librelog.enums.AuditActionType;
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
 * Entity representing an audit log entry for tracking all user actions in the system.
 * Audit logs are immutable and provide a complete history of system changes for compliance
 * and troubleshooting purposes.
 */
@Entity
@Table(name = "audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne
	@JoinColumn(name = "user_id")
	private User user;

	@ManyToOne
	@JoinColumn(name = "impersonated_user_id")
	private User impersonatedUser;

	@Enumerated(EnumType.STRING)
	@Column(name = "action_type", nullable = false)
	private AuditActionType actionType;

	@Column(name = "resource_type", nullable = false)
	private String resourceType;

	@Column(name = "resource_id")
	private UUID resourceId;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "previous_value", columnDefinition = "jsonb")
	private Map<String, Object> previousValue;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "new_value", columnDefinition = "jsonb")
	private Map<String, Object> newValue;

	@Column(name = "ip_address")
	private String ipAddress;

	@Column(name = "user_agent", length = 500)
	private String userAgent;

	@ManyToOne
	@JoinColumn(name = "station_id")
	private Station station;

	@Column(nullable = false)
	private LocalDateTime timestamp;

}

