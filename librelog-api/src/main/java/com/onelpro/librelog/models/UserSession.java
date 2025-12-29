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

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing an active user session in the system.
 * Tracks login information, activity, and current resource being accessed
 * to enable session management and prevent "Locked Record" conflicts.
 */
@Entity
@Table(name = "user_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSession {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "session_token", nullable = false, unique = true, length = 500)
	private String sessionToken;

	@Column(name = "login_timestamp", nullable = false)
	private LocalDateTime loginTimestamp;

	@Column(name = "last_activity_timestamp", nullable = false)
	private LocalDateTime lastActivityTimestamp;

	@Column(name = "ip_address")
	private String ipAddress;

	@Column(name = "user_agent", length = 500)
	private String userAgent;

	@ManyToOne
	@JoinColumn(name = "current_station_id")
	private Station currentStation;

	@Column(name = "current_resource_id")
	private UUID currentResourceId;

	@Column(name = "is_active", nullable = false)
	private Boolean isActive;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

}

