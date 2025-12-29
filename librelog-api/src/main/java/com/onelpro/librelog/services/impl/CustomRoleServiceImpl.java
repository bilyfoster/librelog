package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.CustomRoleRequestDTO;
import com.onelpro.librelog.dto.CustomRoleResponseDTO;
import com.onelpro.librelog.enums.AuditActionType;
import com.onelpro.librelog.exceptions.BadRequestException;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.models.CustomRole;
import com.onelpro.librelog.models.User;
import com.onelpro.librelog.repositories.CustomRoleRepository;
import com.onelpro.librelog.repositories.UserRepository;
import com.onelpro.librelog.repositories.UserStationAssignmentRepository;
import com.onelpro.librelog.services.AuditService;
import com.onelpro.librelog.services.CustomRoleService;
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
 * Implementation of custom role service.
 * Handles creation, modification, and deletion of user-defined roles with granular permissions.
 */
@Service
public class CustomRoleServiceImpl implements CustomRoleService {

	private static final Logger logger = LoggerFactory.getLogger(CustomRoleServiceImpl.class);

	private final CustomRoleRepository customRoleRepository;
	private final UserRepository userRepository;
	private final UserStationAssignmentRepository userStationAssignmentRepository;
	private final AuditService auditService;

	public CustomRoleServiceImpl(
			CustomRoleRepository customRoleRepository,
			UserRepository userRepository,
			UserStationAssignmentRepository userStationAssignmentRepository,
			AuditService auditService) {
		this.customRoleRepository = customRoleRepository;
		this.userRepository = userRepository;
		this.userStationAssignmentRepository = userStationAssignmentRepository;
		this.auditService = auditService;
	}

	@Override
	@Transactional
	public CustomRoleResponseDTO createRole(CustomRoleRequestDTO request, UUID createdByUserId) {
		logger.info("Creating custom role: {} by user: {}", request.getName(), createdByUserId);

		// Validate creator exists
		User createdBy = userRepository.findById(createdByUserId)
				.orElseThrow(() -> new NotFoundException("User not found with id: " + createdByUserId));

		// Check if role name already exists
		if (customRoleRepository.existsByName(request.getName())) {
			throw new BadRequestException("A role with the name '" + request.getName() + "' already exists");
		}

		// Validate permissions
		if (request.getPermissions() == null || request.getPermissions().isEmpty()) {
			throw new BadRequestException("Permissions are required for custom roles");
		}

		// Create role
		LocalDateTime now = LocalDateTime.now();
		CustomRole role = CustomRole.builder()
				.name(request.getName())
				.description(request.getDescription())
				.permissions(convertPermissionsToObjectMap(request.getPermissions()))
				.createdByUser(createdBy)
				.createdAt(now)
				.updatedAt(now)
				.build();

		CustomRole savedRole = customRoleRepository.save(role);

		// Audit log
		Map<String, Object> newValue = new HashMap<>();
		newValue.put("roleId", savedRole.getId());
		newValue.put("name", savedRole.getName());
		newValue.put("description", savedRole.getDescription());
		newValue.put("permissions", savedRole.getPermissions());

		auditService.logAction(
				AuditActionType.CREATE,
				"CustomRole",
				savedRole.getId(),
				null,
				newValue,
				createdByUserId,
				null,
				getClientIpAddress(),
				getUserAgent()
		);

		logger.info("Custom role created: {}", savedRole.getId());

		return mapToResponseDTO(savedRole);
	}

	@Override
	@Transactional
	public CustomRoleResponseDTO updateRole(UUID roleId, CustomRoleRequestDTO request) {
		logger.info("Updating custom role: {}", roleId);

		CustomRole role = customRoleRepository.findById(roleId)
				.orElseThrow(() -> new NotFoundException("Custom role not found with id: " + roleId));

		// Store previous value for audit log
		Map<String, Object> previousValue = new HashMap<>();
		previousValue.put("name", role.getName());
		previousValue.put("description", role.getDescription());
		previousValue.put("permissions", role.getPermissions());

		// Check if new name conflicts with existing role (excluding current role)
		if (!role.getName().equals(request.getName()) && customRoleRepository.existsByName(request.getName())) {
			throw new BadRequestException("A role with the name '" + request.getName() + "' already exists");
		}

		// Validate permissions
		if (request.getPermissions() == null || request.getPermissions().isEmpty()) {
			throw new BadRequestException("Permissions are required for custom roles");
		}

		// Update role
		role.setName(request.getName());
		role.setDescription(request.getDescription());
		role.setPermissions(convertPermissionsToObjectMap(request.getPermissions()));
		role.setUpdatedAt(LocalDateTime.now());

		CustomRole savedRole = customRoleRepository.save(role);

		// Audit log
		UUID currentUserId = getCurrentUserId();
		Map<String, Object> newValue = new HashMap<>();
		newValue.put("name", savedRole.getName());
		newValue.put("description", savedRole.getDescription());
		newValue.put("permissions", savedRole.getPermissions());

		auditService.logAction(
				AuditActionType.UPDATE,
				"CustomRole",
				savedRole.getId(),
				previousValue,
				newValue,
				currentUserId,
				null,
				getClientIpAddress(),
				getUserAgent()
		);

		logger.info("Custom role updated: {}", roleId);

		return mapToResponseDTO(savedRole);
	}

	@Override
	@Transactional
	public void deleteRole(UUID roleId) {
		logger.info("Deleting custom role: {}", roleId);

		CustomRole role = customRoleRepository.findById(roleId)
				.orElseThrow(() -> new NotFoundException("Custom role not found with id: " + roleId));

		// Check if role is assigned to any users
		// Note: Since there's no direct relationship between User and CustomRole in the current schema,
		// we check if any user-station assignments have custom permissions that might reference this role.
		// This is a simplified check - in a full implementation, you might want to add a direct relationship.
		long assignmentCount = userStationAssignmentRepository.findAll().stream()
				.filter(assignment -> assignment.getCustomPermissions() != null)
				.count();

		// For now, we'll allow deletion but log a warning if there are custom permissions in use
		// In a production system, you might want to add a direct relationship or reference tracking
		if (assignmentCount > 0) {
			logger.warn("Deleting custom role {} while {} assignments have custom permissions. " +
					"Consider checking if this role is referenced before deletion.", roleId, assignmentCount);
		}

		// Store previous value for audit log
		Map<String, Object> previousValue = new HashMap<>();
		previousValue.put("roleId", role.getId());
		previousValue.put("name", role.getName());
		previousValue.put("description", role.getDescription());
		previousValue.put("permissions", role.getPermissions());

		customRoleRepository.delete(role);

		// Audit log
		UUID currentUserId = getCurrentUserId();
		auditService.logAction(
				AuditActionType.DELETE,
				"CustomRole",
				roleId,
				previousValue,
				null,
				currentUserId,
				null,
				getClientIpAddress(),
				getUserAgent()
		);

		logger.info("Custom role deleted: {}", roleId);
	}

	@Override
	@Transactional(readOnly = true)
	public CustomRoleResponseDTO getRoleById(UUID roleId) {
		logger.debug("Getting custom role: {}", roleId);

		CustomRole role = customRoleRepository.findById(roleId)
				.orElseThrow(() -> new NotFoundException("Custom role not found with id: " + roleId));

		return mapToResponseDTO(role);
	}

	@Override
	@Transactional(readOnly = true)
	public List<CustomRoleResponseDTO> getAllRoles() {
		logger.debug("Getting all custom roles");

		List<CustomRole> roles = customRoleRepository.findAll();
		return roles.stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public CustomRoleResponseDTO cloneRole(UUID roleId, String newName, UUID createdByUserId) {
		logger.info("Cloning custom role {} with new name: {}", roleId, newName);

		CustomRole sourceRole = customRoleRepository.findById(roleId)
				.orElseThrow(() -> new NotFoundException("Custom role not found with id: " + roleId));

		// Validate creator exists
		User createdBy = userRepository.findById(createdByUserId)
				.orElseThrow(() -> new NotFoundException("User not found with id: " + createdByUserId));

		// Check if new name already exists
		if (customRoleRepository.existsByName(newName)) {
			throw new BadRequestException("A role with the name '" + newName + "' already exists");
		}

		// Clone the role
		LocalDateTime now = LocalDateTime.now();
		CustomRole clonedRole = CustomRole.builder()
				.name(newName)
				.description("Cloned from: " + sourceRole.getName() + 
						(sourceRole.getDescription() != null ? " - " + sourceRole.getDescription() : ""))
				.permissions(new HashMap<>(sourceRole.getPermissions()))
				.createdByUser(createdBy)
				.createdAt(now)
				.updatedAt(now)
				.build();

		CustomRole savedRole = customRoleRepository.save(clonedRole);

		// Audit log
		Map<String, Object> newValue = new HashMap<>();
		newValue.put("roleId", savedRole.getId());
		newValue.put("name", savedRole.getName());
		newValue.put("description", savedRole.getDescription());
		newValue.put("permissions", savedRole.getPermissions());
		newValue.put("clonedFrom", sourceRole.getId());

		auditService.logAction(
				AuditActionType.CREATE,
				"CustomRole",
				savedRole.getId(),
				null,
				newValue,
				createdByUserId,
				null,
				getClientIpAddress(),
				getUserAgent()
		);

		logger.info("Custom role cloned: {} -> {}", roleId, savedRole.getId());

		return mapToResponseDTO(savedRole);
	}

	@Override
	@Transactional(readOnly = true)
	public List<CustomRoleResponseDTO> getRolesAssignedToUser(UUID userId) {
		logger.debug("Getting custom roles assigned to user: {}", userId);

		// Validate user exists
		if (!userRepository.existsById(userId)) {
			throw new NotFoundException("User not found with id: " + userId);
		}

		// Note: Since there's no direct relationship between User and CustomRole in the current schema,
		// this method returns an empty list. In a full implementation, you might want to:
		// 1. Add a direct relationship (e.g., user.customRoleId)
		// 2. Or check user-station assignments for custom permissions that match role permissions
		// For now, we'll return an empty list as custom roles are templates, not directly assigned.
		logger.debug("No direct relationship between User and CustomRole. Returning empty list.");
		return List.of();
	}

	/**
	 * Converts permissions from Map<String, Object> to the format expected by the entity.
	 */
	@SuppressWarnings("unchecked")
	private Map<String, Object> convertPermissionsToObjectMap(Map<String, Object> permissions) {
		Map<String, Object> result = new HashMap<>();

		for (Map.Entry<String, Object> entry : permissions.entrySet()) {
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
			} else if (entry.getValue() instanceof List) {
				// Handle list format: ["VIEW", "CREATE"] -> {"VIEW": true, "CREATE": true}
				List<?> actionList = (List<?>) entry.getValue();
				Map<String, Object> actionMap = new HashMap<>();
				for (Object action : actionList) {
					if (action instanceof String) {
						actionMap.put((String) action, true);
					}
				}
				if (!actionMap.isEmpty()) {
					result.put(entry.getKey(), actionMap);
				}
			}
		}

		return result;
	}

	/**
	 * Maps CustomRole entity to CustomRoleResponseDTO.
	 */
	private CustomRoleResponseDTO mapToResponseDTO(CustomRole role) {
		// Count users with custom permissions (simplified - in production, track direct assignments)
		long assignedUserCount = 0;
		// For now, we can't accurately count users assigned to this role without a direct relationship
		// This is a placeholder - in production, add a direct relationship or reference tracking

		return CustomRoleResponseDTO.builder()
				.id(role.getId())
				.name(role.getName())
				.description(role.getDescription())
				.permissions(role.getPermissions())
				.createdByUserId(role.getCreatedByUser().getId())
				.createdByUserEmail(role.getCreatedByUser().getEmail())
				.createdAt(role.getCreatedAt())
				.updatedAt(role.getUpdatedAt())
				.assignedUserCount(assignedUserCount)
				.build();
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

