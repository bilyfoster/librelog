package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.AuditLogFilterDTO;
import com.onelpro.librelog.dto.AuditLogResponseDTO;
import com.onelpro.librelog.enums.AuditActionType;
import com.onelpro.librelog.enums.ResourceType;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.models.AuditLog;
import com.onelpro.librelog.models.Station;
import com.onelpro.librelog.models.User;
import com.onelpro.librelog.repositories.AuditLogRepository;
import com.onelpro.librelog.repositories.StationRepository;
import com.onelpro.librelog.repositories.UserRepository;
import com.onelpro.librelog.services.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of audit service.
 * Handles comprehensive action logging for compliance and troubleshooting.
 */
@Service
public class AuditServiceImpl implements AuditService {

	private static final Logger logger = LoggerFactory.getLogger(AuditServiceImpl.class);

	private final AuditLogRepository auditLogRepository;
	private final UserRepository userRepository;
	private final StationRepository stationRepository;

	public AuditServiceImpl(
			AuditLogRepository auditLogRepository,
			UserRepository userRepository,
			StationRepository stationRepository) {
		this.auditLogRepository = auditLogRepository;
		this.userRepository = userRepository;
		this.stationRepository = stationRepository;
	}

	@Override
	@Transactional
	public void logAction(AuditActionType actionType, String resourceType, UUID resourceId,
	                      Map<String, Object> previousValue, Map<String, Object> newValue,
	                      UUID userId, UUID stationId, String ipAddress, String userAgent) {
		logger.debug("Logging action: {} on {}:{} by user {}", actionType, resourceType, resourceId, userId);

		User user = userId != null ? userRepository.findById(userId).orElse(null) : null;
		Station station = stationId != null ? stationRepository.findById(stationId).orElse(null) : null;

		AuditLog auditLog = AuditLog.builder()
				.user(user)
				.actionType(actionType)
				.resourceType(resourceType)
				.resourceId(resourceId)
				.previousValue(previousValue)
				.newValue(newValue)
				.ipAddress(ipAddress)
				.userAgent(userAgent != null && userAgent.length() > 500 ? userAgent.substring(0, 500) : userAgent)
				.station(station)
				.timestamp(LocalDateTime.now())
				.build();

		auditLogRepository.save(auditLog);
		logger.debug("Audit log created with ID: {}", auditLog.getId());
	}

	@Override
	@Transactional
	public void logLogin(UUID userId, String ipAddress, String userAgent) {
		logger.info("Logging login for user: {}", userId);

		User user = userRepository.findById(userId)
				.orElseThrow(() -> new NotFoundException("User not found with id: " + userId));

		AuditLog auditLog = AuditLog.builder()
				.user(user)
				.actionType(AuditActionType.LOGIN)
				.resourceType(ResourceType.USER.name())
				.resourceId(userId)
				.ipAddress(ipAddress)
				.userAgent(userAgent != null && userAgent.length() > 500 ? userAgent.substring(0, 500) : userAgent)
				.timestamp(LocalDateTime.now())
				.build();

		auditLogRepository.save(auditLog);
	}

	@Override
	@Transactional
	public void logLogout(UUID userId, String ipAddress, String userAgent) {
		logger.info("Logging logout for user: {}", userId);

		User user = userRepository.findById(userId)
				.orElseThrow(() -> new NotFoundException("User not found with id: " + userId));

		AuditLog auditLog = AuditLog.builder()
				.user(user)
				.actionType(AuditActionType.LOGOUT)
				.resourceType(ResourceType.USER.name())
				.resourceId(userId)
				.ipAddress(ipAddress)
				.userAgent(userAgent != null && userAgent.length() > 500 ? userAgent.substring(0, 500) : userAgent)
				.timestamp(LocalDateTime.now())
				.build();

		auditLogRepository.save(auditLog);
	}

	@Override
	@Transactional
	public void logPermissionChange(UUID userId, UUID targetUserId,
	                                Map<String, Object> previousPermissions, Map<String, Object> newPermissions,
	                                String ipAddress, String userAgent) {
		logger.info("Logging permission change for user: {} by user: {}", targetUserId, userId);

		User user = userRepository.findById(userId)
				.orElseThrow(() -> new NotFoundException("User not found with id: " + userId));

		User targetUser = userRepository.findById(targetUserId)
				.orElseThrow(() -> new NotFoundException("Target user not found with id: " + targetUserId));

		Map<String, Object> previousValue = new HashMap<>();
		previousValue.put("permissions", previousPermissions);

		Map<String, Object> newValue = new HashMap<>();
		newValue.put("permissions", newPermissions);

		AuditLog auditLog = AuditLog.builder()
				.user(user)
				.actionType(AuditActionType.PERMISSION_CHANGE)
				.resourceType(ResourceType.PERMISSION.name())
				.resourceId(targetUserId)
				.previousValue(previousValue)
				.newValue(newValue)
				.ipAddress(ipAddress)
				.userAgent(userAgent != null && userAgent.length() > 500 ? userAgent.substring(0, 500) : userAgent)
				.timestamp(LocalDateTime.now())
				.build();

		auditLogRepository.save(auditLog);
	}

	@Override
	public Page<AuditLogResponseDTO> getAuditLogs(AuditLogFilterDTO filter) {
		logger.debug("Getting audit logs with filter: {}", filter);

		Pageable pageable = PageRequest.of(
				filter.getPage() != null ? filter.getPage() : 0,
				filter.getSize() != null ? filter.getSize() : 20,
				Sort.by(Sort.Direction.DESC, "timestamp")
		);

		Page<AuditLog> auditLogs = auditLogRepository.findWithFilters(
				filter.getUserId(),
				filter.getActionType(),
				filter.getResourceType(),
				filter.getStationId(),
				filter.getStartDate(),
				filter.getEndDate(),
				pageable
		);

		return auditLogs.map(this::mapToResponseDTO);
	}

	@Override
	public byte[] exportAuditLogs(AuditLogFilterDTO filter) {
		logger.info("Exporting audit logs with filter: {}", filter);

		// Get all matching logs (no pagination for export)
		Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.DESC, "timestamp"));
		Page<AuditLog> auditLogs = auditLogRepository.findWithFilters(
				filter.getUserId(),
				filter.getActionType(),
				filter.getResourceType(),
				filter.getStationId(),
				filter.getStartDate(),
				filter.getEndDate(),
				pageable
		);

		// Generate CSV
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
		     PrintWriter writer = new PrintWriter(baos)) {

			// Write CSV header
			writer.println("Timestamp,User Email,Action Type,Resource Type,Resource ID,IP Address,Station Name");

			// Write data rows
			for (AuditLog log : auditLogs.getContent()) {
				writer.printf("%s,%s,%s,%s,%s,%s,%s%n",
						log.getTimestamp(),
						log.getUser() != null ? log.getUser().getEmail() : "",
						log.getActionType(),
						log.getResourceType(),
						log.getResourceId() != null ? log.getResourceId() : "",
						log.getIpAddress() != null ? log.getIpAddress() : "",
						log.getStation() != null ? log.getStation().getName() : ""
				);
			}

			writer.flush();
			return baos.toByteArray();
		} catch (IOException e) {
			logger.error("Error exporting audit logs", e);
			throw new RuntimeException("Failed to export audit logs", e);
		}
	}

	/**
	 * Maps AuditLog entity to AuditLogResponseDTO.
	 */
	private AuditLogResponseDTO mapToResponseDTO(AuditLog auditLog) {
		return AuditLogResponseDTO.builder()
				.id(auditLog.getId())
				.userId(auditLog.getUser() != null ? auditLog.getUser().getId() : null)
				.userEmail(auditLog.getUser() != null ? auditLog.getUser().getEmail() : null)
				.impersonatedUserId(auditLog.getImpersonatedUser() != null ? auditLog.getImpersonatedUser().getId() : null)
				.impersonatedUserEmail(auditLog.getImpersonatedUser() != null ? auditLog.getImpersonatedUser().getEmail() : null)
				.actionType(auditLog.getActionType())
				.resourceType(auditLog.getResourceType())
				.resourceId(auditLog.getResourceId())
				.previousValue(auditLog.getPreviousValue())
				.newValue(auditLog.getNewValue())
				.ipAddress(auditLog.getIpAddress())
				.userAgent(auditLog.getUserAgent())
				.stationId(auditLog.getStation() != null ? auditLog.getStation().getId() : null)
				.stationName(auditLog.getStation() != null ? auditLog.getStation().getName() : null)
				.timestamp(auditLog.getTimestamp())
				.build();
	}

}


