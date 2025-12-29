package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.UserStationAssignmentRequestDTO;
import com.onelpro.librelog.dto.UserStationAssignmentResponseDTO;
import com.onelpro.librelog.enums.AuditActionType;
import com.onelpro.librelog.enums.PermissionLevel;
import com.onelpro.librelog.exceptions.BadRequestException;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.models.Station;
import com.onelpro.librelog.models.User;
import com.onelpro.librelog.models.UserStationAssignment;
import com.onelpro.librelog.repositories.StationRepository;
import com.onelpro.librelog.repositories.UserRepository;
import com.onelpro.librelog.repositories.UserStationAssignmentRepository;
import com.onelpro.librelog.services.AuditService;
import com.onelpro.librelog.services.PermissionService;
import com.onelpro.librelog.services.UserStationAssignmentService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of user-station assignment service.
 * Handles property-based access control assignments with audit logging.
 */
@Service
public class UserStationAssignmentServiceImpl implements UserStationAssignmentService {

	private static final Logger logger = LoggerFactory.getLogger(UserStationAssignmentServiceImpl.class);

	private final UserStationAssignmentRepository userStationAssignmentRepository;
	private final UserRepository userRepository;
	private final StationRepository stationRepository;
	private final AuditService auditService;
	private final PermissionService permissionService;

	public UserStationAssignmentServiceImpl(
			UserStationAssignmentRepository userStationAssignmentRepository,
			UserRepository userRepository,
			StationRepository stationRepository,
			AuditService auditService,
			PermissionService permissionService) {
		this.userStationAssignmentRepository = userStationAssignmentRepository;
		this.userRepository = userRepository;
		this.stationRepository = stationRepository;
		this.auditService = auditService;
		this.permissionService = permissionService;
	}

	@Override
	@Transactional
	public UserStationAssignmentResponseDTO assignUserToStation(UserStationAssignmentRequestDTO request) {
		logger.info("Assigning user {} to station {}", request.getUserId(), request.getStationId());

		// Validate user exists
		User user = userRepository.findById(request.getUserId())
				.orElseThrow(() -> new NotFoundException("User not found with id: " + request.getUserId()));

		// Validate station exists
		Station station = stationRepository.findById(request.getStationId())
				.orElseThrow(() -> new NotFoundException("Station not found with id: " + request.getStationId()));

		// Check if assignment already exists
		if (userStationAssignmentRepository.existsByUserIdAndStationId(request.getUserId(), request.getStationId())) {
			throw new BadRequestException("User is already assigned to this station");
		}

		// Validate custom permissions if permission level is CUSTOM
		if (request.getPermissionLevel() == PermissionLevel.CUSTOM) {
			if (request.getCustomPermissions() == null || request.getCustomPermissions().isEmpty()) {
				throw new BadRequestException("Custom permissions are required when permission level is CUSTOM");
			}
		}

		// Create assignment
		LocalDateTime now = LocalDateTime.now();
		Map<String, Object> customPerms = null;
		if (request.getCustomPermissions() != null) {
			customPerms = convertCustomPermissionsToObjectMap(request.getCustomPermissions());
		}
		UserStationAssignment assignment = UserStationAssignment.builder()
				.user(user)
				.station(station)
				.permissionLevel(request.getPermissionLevel())
				.customPermissions(customPerms)
				.createdAt(now)
				.updatedAt(now)
				.build();

		UserStationAssignment savedAssignment = userStationAssignmentRepository.save(assignment);

		// Evict permission cache for this user
		permissionService.evictAllPermissionCaches();

		// Audit log
		UUID currentUserId = getCurrentUserId();
		Map<String, Object> newValue = new HashMap<>();
		newValue.put("userId", request.getUserId());
		newValue.put("stationId", request.getStationId());
		newValue.put("permissionLevel", request.getPermissionLevel().name());
		newValue.put("customPermissions", request.getCustomPermissions());

		auditService.logAction(
				AuditActionType.CREATE,
				"UserStationAssignment",
				savedAssignment.getId(),
				null,
				newValue,
				currentUserId,
				request.getStationId(),
				getClientIpAddress(),
				getUserAgent()
		);

		logger.info("User {} assigned to station {} with permission level {}", 
				request.getUserId(), request.getStationId(), request.getPermissionLevel());

		return mapToResponseDTO(savedAssignment);
	}

	@Override
	@Transactional
	public void removeUserFromStation(UUID userId, UUID stationId) {
		logger.info("Removing user {} from station {}", userId, stationId);

		UserStationAssignment assignment = userStationAssignmentRepository
				.findByUserIdAndStationId(userId, stationId)
				.orElseThrow(() -> new NotFoundException(
						"User-station assignment not found for user " + userId + " and station " + stationId));

		// Store previous value for audit log
		Map<String, Object> previousValue = new HashMap<>();
		previousValue.put("userId", userId);
		previousValue.put("stationId", stationId);
		previousValue.put("permissionLevel", assignment.getPermissionLevel().name());
		previousValue.put("customPermissions", assignment.getCustomPermissions());

		userStationAssignmentRepository.delete(assignment);

		// Evict permission cache for this user
		permissionService.evictAllPermissionCaches();

		// Audit log
		UUID currentUserId = getCurrentUserId();
		auditService.logAction(
				AuditActionType.DELETE,
				"UserStationAssignment",
				assignment.getId(),
				previousValue,
				null,
				currentUserId,
				stationId,
				getClientIpAddress(),
				getUserAgent()
		);

		logger.info("User {} removed from station {}", userId, stationId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<UserStationAssignmentResponseDTO> getUserStationAssignments(UUID userId) {
		logger.debug("Getting station assignments for user: {}", userId);

		// Validate user exists
		if (!userRepository.existsById(userId)) {
			throw new NotFoundException("User not found with id: " + userId);
		}

		List<UserStationAssignment> assignments = userStationAssignmentRepository.findByUserId(userId);
		return assignments.stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public List<UserStationAssignmentResponseDTO> getStationUserAssignments(UUID stationId) {
		logger.debug("Getting user assignments for station: {}", stationId);

		// Validate station exists
		if (!stationRepository.existsById(stationId)) {
			throw new NotFoundException("Station not found with id: " + stationId);
		}

		List<UserStationAssignment> assignments = userStationAssignmentRepository.findByStationId(stationId);
		return assignments.stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public UserStationAssignmentResponseDTO updatePermissionLevel(UUID userId, UUID stationId,
	                                                               PermissionLevel permissionLevel,
	                                                               Map<String, Object> customPermissions) {
		logger.info("Updating permission level for user {} on station {} to {}", userId, stationId, permissionLevel);

		UserStationAssignment assignment = userStationAssignmentRepository
				.findByUserIdAndStationId(userId, stationId)
				.orElseThrow(() -> new NotFoundException(
						"User-station assignment not found for user " + userId + " and station " + stationId));

		// Store previous value for audit log
		Map<String, Object> previousValue = new HashMap<>();
		previousValue.put("permissionLevel", assignment.getPermissionLevel().name());
		previousValue.put("customPermissions", assignment.getCustomPermissions());

		// Validate custom permissions if permission level is CUSTOM
		if (permissionLevel == PermissionLevel.CUSTOM) {
			if (customPermissions == null || customPermissions.isEmpty()) {
				throw new BadRequestException("Custom permissions are required when permission level is CUSTOM");
			}
		}

		// Update assignment
		assignment.setPermissionLevel(permissionLevel);
		Map<String, Object> customPerms = null;
		if (customPermissions != null) {
			customPerms = convertCustomPermissionsToObjectMap(customPermissions);
		}
		assignment.setCustomPermissions(customPerms);
		assignment.setUpdatedAt(LocalDateTime.now());

		UserStationAssignment savedAssignment = userStationAssignmentRepository.save(assignment);

		// Evict permission cache for this user
		permissionService.evictAllPermissionCaches();

		// Audit log
		UUID currentUserId = getCurrentUserId();
		Map<String, Object> newValue = new HashMap<>();
		newValue.put("permissionLevel", permissionLevel.name());
		newValue.put("customPermissions", customPermissions);

		auditService.logAction(
				AuditActionType.UPDATE,
				"UserStationAssignment",
				savedAssignment.getId(),
				previousValue,
				newValue,
				currentUserId,
				stationId,
				getClientIpAddress(),
				getUserAgent()
		);

		logger.info("Permission level updated for user {} on station {}", userId, stationId);

		return mapToResponseDTO(savedAssignment);
	}

	/**
	 * Converts custom permissions from Map<String, Object> to Map<String, Object> format.
	 * This method validates and normalizes the permissions structure for storage.
	 */
	@SuppressWarnings("unchecked")
	private Map<String, Object> convertCustomPermissionsToObjectMap(Map<String, Object> customPermissions) {
		Map<String, Object> result = new HashMap<>();
		
		for (Map.Entry<String, Object> entry : customPermissions.entrySet()) {
			if (entry.getValue() instanceof Map) {
				Map<String, Object> actionMap = (Map<String, Object>) entry.getValue();
				Map<String, Object> normalizedMap = new HashMap<>();
				
				for (Map.Entry<String, Object> actionEntry : actionMap.entrySet()) {
					if (actionEntry.getValue() instanceof Boolean) {
						normalizedMap.put(actionEntry.getKey(), actionEntry.getValue());
					}
				}
				
				if (!normalizedMap.isEmpty()) {
					result.put(entry.getKey(), normalizedMap);
				}
			}
		}
		
		return result;
	}

	/**
	 * Maps UserStationAssignment entity to UserStationAssignmentResponseDTO.
	 */
	private UserStationAssignmentResponseDTO mapToResponseDTO(UserStationAssignment assignment) {
		return UserStationAssignmentResponseDTO.builder()
				.id(assignment.getId())
				.userId(assignment.getUser().getId())
				.userEmail(assignment.getUser().getEmail())
				.stationId(assignment.getStation().getId())
				.stationName(assignment.getStation().getName())
				.permissionLevel(assignment.getPermissionLevel())
				.customPermissions(assignment.getCustomPermissions() != null ?
						convertToObjectMap(assignment.getCustomPermissions()) : null)
				.createdAt(assignment.getCreatedAt())
				.updatedAt(assignment.getUpdatedAt())
				.build();
	}

	/**
	 * Converts the stored Map<String, Object> back to Map<String, Object> for DTO.
	 * This is a pass-through since the entity already stores it as Map<String, Object>.
	 */
	private Map<String, Object> convertToObjectMap(Map<String, Object> permissions) {
		return permissions;
	}

	/**
	 * Gets the current user ID from the security context.
	 * Returns null if not authenticated.
	 */
	private UUID getCurrentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return null;
		}

		// JwtAuthenticationFilter sets the principal as the userId (String)
		Object principal = authentication.getPrincipal();
		if (principal instanceof String) {
			try {
				return UUID.fromString((String) principal);
			} catch (IllegalArgumentException e) {
				logger.warn("Invalid UUID format in authentication principal: {}", principal);
				return null;
			}
		}

		return null;
	}

	/**
	 * Gets the client IP address from the current HTTP request.
	 */
	private String getClientIpAddress() {
		try {
			ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
			if (attributes != null) {
				HttpServletRequest request = attributes.getRequest();
				String xForwardedFor = request.getHeader("X-Forwarded-For");
				if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
					return xForwardedFor.split(",")[0].trim();
				}
				return request.getRemoteAddr();
			}
		} catch (Exception e) {
			logger.warn("Failed to get client IP address", e);
		}
		return null;
	}

	/**
	 * Gets the user agent from the current HTTP request.
	 */
	private String getUserAgent() {
		try {
			ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
			if (attributes != null) {
				HttpServletRequest request = attributes.getRequest();
				return request.getHeader("User-Agent");
			}
		} catch (Exception e) {
			logger.warn("Failed to get user agent", e);
		}
		return null;
	}

}

