package com.onelpro.librelog.models;

import com.onelpro.librelog.enums.AutomationCommandType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;

/**
 * AutomationCommand entity representing automation system triggers.
 * These are non-audio commands that tell the control room to perform specific actions,
 * such as "Switch to Satellite Feed" or "Start Recording".
 */
@Entity
@Table(name = "automation_commands")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutomationCommand {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne
	@JoinColumn(name = "clock_template_id", nullable = false)
	private ClockTemplate clockTemplate;

	@Enumerated(EnumType.STRING)
	@Column(name = "command_type", nullable = false)
	private AutomationCommandType commandType;

	@Column(name = "trigger_time", nullable = false)
	private LocalTime triggerTime;

	@Column(name = "priority")
	private String priority;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "parameters", columnDefinition = "jsonb")
	private Map<String, Object> parameters;

	// LibreTime integration fields
	@Column(name = "libretime_playlist_id")
	private String libreTimePlaylistId;

	@Column(name = "libretime_smart_block_id")
	private String libreTimeSmartBlockId;

	@Column(name = "libretime_command_type")
	private String libreTimeCommandType; // PLAYLIST, SMART_BLOCK, LIVE_INPUT, NETWORK_FEED, EAS_ALERT

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

}

