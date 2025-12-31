package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.enums.ActionType;
import com.onelpro.librelog.enums.ModuleType;
import com.onelpro.librelog.enums.PermissionLevel;
import com.onelpro.librelog.enums.UserRole;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.models.User;
import com.onelpro.librelog.models.UserStationAssignment;
import com.onelpro.librelog.repositories.UserRepository;
import com.onelpro.librelog.repositories.UserStationAssignmentRepository;
import com.onelpro.librelog.services.PermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of permission service.
 * Handles property-based access control, role-based permissions, and custom permissions.
 */
@Service
public class PermissionServiceImpl implements PermissionService {

	private static final Logger logger = LoggerFactory.getLogger(PermissionServiceImpl.class);

	private final UserRepository userRepository;
	private final UserStationAssignmentRepository userStationAssignmentRepository;

	public PermissionServiceImpl(
			UserRepository userRepository,
			UserStationAssignmentRepository userStationAssignmentRepository) {
		this.userRepository = userRepository;
		this.userStationAssignmentRepository = userStationAssignmentRepository;
	}

	@Override
	@Cacheable(value = "permissions", key = "#userId + '_' + #stationId + '_' + #moduleType + '_' + #actionType")
	public boolean hasPermission(UUID userId, UUID stationId, ModuleType moduleType, ActionType actionType) {
		logger.debug("Checking permission for user {} at station {} for {}:{}", userId, stationId, moduleType, actionType);

		User user = userRepository.findById(userId)
				.orElseThrow(() -> new NotFoundException("User not found with id: " + userId));

		// Admin users have full access
		if (user.getRole() == UserRole.ADMIN) {
			logger.debug("User {} is ADMIN, granting permission", userId);
			return true;
		}

		// If stationId is null, check tenant-level permissions
		if (stationId == null) {
			return hasTenantLevelPermission(user, moduleType, actionType);
		}

		// Check station-specific assignment
		UserStationAssignment assignment = userStationAssignmentRepository
				.findByUserIdAndStationId(userId, stationId)
				.orElse(null);

		if (assignment != null) {
			return checkAssignmentPermission(assignment, moduleType, actionType);
		}

		// No station assignment found, deny access
		logger.debug("No station assignment found for user {} at station {}, denying permission", userId, stationId);
		return false;
	}

	@Override
	@Cacheable(value = "userStations", key = "#userId")
	public List<UUID> getUserStations(UUID userId) {
		logger.debug("Getting stations for user {}", userId);

		User user = userRepository.findById(userId)
				.orElseThrow(() -> new NotFoundException("User not found with id: " + userId));

		// Admin users have access to all stations
		if (user.getRole() == UserRole.ADMIN) {
			logger.debug("User {} is ADMIN, returning all stations", userId);
			// Return all station IDs (would need StationRepository for this)
			// For now, return assigned stations only
		}

		return userStationAssignmentRepository.findByUserId(userId).stream()
				.map(assignment -> assignment.getStation().getId())
				.collect(Collectors.toList());
	}

	@Override
	@Cacheable(value = "stationAccess", key = "#userId + '_' + #stationId")
	public boolean canAccessStation(UUID userId, UUID stationId) {
		logger.debug("Checking station access for user {} at station {}", userId, stationId);

		User user = userRepository.findById(userId)
				.orElseThrow(() -> new NotFoundException("User not found with id: " + userId));

		// Admin users can access all stations
		if (user.getRole() == UserRole.ADMIN) {
			return true;
		}

		return userStationAssignmentRepository.existsByUserIdAndStationId(userId, stationId);
	}

	@Override
	@Cacheable(value = "effectivePermissions", key = "#userId + '_' + #stationId")
	public Map<ModuleType, List<ActionType>> getEffectivePermissions(UUID userId, UUID stationId) {
		logger.debug("Getting effective permissions for user {} at station {}", userId, stationId);

		User user = userRepository.findById(userId)
				.orElseThrow(() -> new NotFoundException("User not found with id: " + userId));

		Map<ModuleType, List<ActionType>> permissions = new HashMap<>();

		// Admin users have all permissions
		if (user.getRole() == UserRole.ADMIN) {
			for (ModuleType module : ModuleType.values()) {
				permissions.put(module, List.of(ActionType.values()));
			}
			return permissions;
		}

		// Get role-based permissions
		Map<ModuleType, List<ActionType>> rolePermissions = getRolePermissions(user.getRole());
		permissions.putAll(rolePermissions);

		// If stationId is provided, check station-specific overrides
		if (stationId != null) {
			UserStationAssignment assignment = userStationAssignmentRepository
					.findByUserIdAndStationId(userId, stationId)
					.orElse(null);

			if (assignment != null) {
				applyStationOverrides(permissions, assignment);
			}
		}

		return permissions;
	}

	/**
	 * Checks tenant-level permissions based on user role.
	 */
	private boolean hasTenantLevelPermission(User user, ModuleType moduleType, ActionType actionType) {
		Map<ModuleType, List<ActionType>> rolePermissions = getRolePermissions(user.getRole());
		List<ActionType> allowedActions = rolePermissions.get(moduleType);
		return allowedActions != null && allowedActions.contains(actionType);
	}

	/**
	 * Checks permission based on station assignment.
	 */
	private boolean checkAssignmentPermission(UserStationAssignment assignment, ModuleType moduleType, ActionType actionType) {
		PermissionLevel level = assignment.getPermissionLevel();

		if (level == PermissionLevel.FULL_ACCESS) {
			return true;
		}

		if (level == PermissionLevel.VIEW_ONLY) {
			return actionType == ActionType.VIEW;
		}

		if (level == PermissionLevel.CUSTOM) {
			return checkCustomPermissions(assignment.getCustomPermissions(), moduleType, actionType);
		}

		return false;
	}

	/**
	 * Checks custom permissions from the assignment.
	 */
	private boolean checkCustomPermissions(Map<String, Object> customPermissions, ModuleType moduleType, ActionType actionType) {
		if (customPermissions == null || customPermissions.isEmpty()) {
			return false;
		}

		Object modulePerms = customPermissions.get(moduleType.name());
		if (modulePerms == null) {
			return false;
		}

		// Handle different formats: Set<String>, List<String>, or String
		if (modulePerms instanceof Set) {
			@SuppressWarnings("unchecked")
			Set<String> actions = (Set<String>) modulePerms;
			return actions.contains(actionType.name());
		} else if (modulePerms instanceof List) {
			@SuppressWarnings("unchecked")
			List<String> actions = (List<String>) modulePerms;
			return actions.contains(actionType.name());
		} else if (modulePerms instanceof String) {
			String actions = (String) modulePerms;
			return actions.contains(actionType.name());
		}

		return false;
	}

	/**
	 * Gets role-based permissions for a user role.
	 * This is a simplified implementation - in production, this would be more sophisticated.
	 */
	private Map<ModuleType, List<ActionType>> getRolePermissions(UserRole role) {
		Map<ModuleType, List<ActionType>> permissions = new HashMap<>();

		switch (role) {
			case ADMIN:
				// Admin has all permissions
				for (ModuleType module : ModuleType.values()) {
					permissions.put(module, List.of(ActionType.values()));
				}
				break;
			case TRAFFIC_MANAGER:
				// Traffic Manager can view and edit logs, view orders
				permissions.put(ModuleType.LOGS, List.of(ActionType.VIEW, ActionType.CREATE, ActionType.EDIT));
				permissions.put(ModuleType.ORDERS, List.of(ActionType.VIEW));
				permissions.put(ModuleType.INVENTORY, List.of(ActionType.VIEW));
				permissions.put(ModuleType.REPORTS, List.of(ActionType.VIEW));
				break;
			case SALES_REP:
				// Sales Rep can view and create orders, view inventory
				permissions.put(ModuleType.ORDERS, List.of(ActionType.VIEW, ActionType.CREATE));
				permissions.put(ModuleType.INVENTORY, List.of(ActionType.VIEW));
				permissions.put(ModuleType.REPORTS, List.of(ActionType.VIEW));
				break;
			case ACCOUNTING:
				// Accounting can view and edit billing, view orders
				permissions.put(ModuleType.BILLING, List.of(ActionType.VIEW, ActionType.CREATE, ActionType.EDIT));
				permissions.put(ModuleType.ORDERS, List.of(ActionType.VIEW));
				permissions.put(ModuleType.REPORTS, List.of(ActionType.VIEW));
				break;
			case PROGRAMMING:
				// Programming can view and edit material instructions, view logs
				permissions.put(ModuleType.MATERIAL_INSTRUCTIONS, List.of(ActionType.VIEW, ActionType.CREATE, ActionType.EDIT));
				permissions.put(ModuleType.LOGS, List.of(ActionType.VIEW));
				break;
			case OPERATIONS:
				// Operations can view most modules and sync LibreTime integration
				permissions.put(ModuleType.ORDERS, List.of(ActionType.VIEW));
				permissions.put(ModuleType.LOGS, List.of(ActionType.VIEW));
				permissions.put(ModuleType.INVENTORY, List.of(ActionType.VIEW));
				permissions.put(ModuleType.REPORTS, List.of(ActionType.VIEW));
				permissions.put(ModuleType.LIBRETIME_INTEGRATION, List.of(ActionType.VIEW, ActionType.CREATE)); // VIEW and SYNC
				break;
		}

		// Add LibreTime integration permissions for all roles that should have access
		// Administrators already have all permissions via the ADMIN case above
		// Operations role gets VIEW and SYNC (CREATE) as defined above
		// For other roles, add VIEW only if they don't already have it
		if (role != UserRole.ADMIN && !permissions.containsKey(ModuleType.LIBRETIME_INTEGRATION)) {
			// Default: VIEW only for roles that don't have explicit LibreTime permissions
			permissions.put(ModuleType.LIBRETIME_INTEGRATION, List.of(ActionType.VIEW));
		}

		return permissions;
	}

	/**
	 * Applies station-specific permission overrides.
	 */
	private void applyStationOverrides(Map<ModuleType, List<ActionType>> permissions, UserStationAssignment assignment) {
		PermissionLevel level = assignment.getPermissionLevel();

		if (level == PermissionLevel.FULL_ACCESS) {
			// Grant all permissions
			for (ModuleType module : ModuleType.values()) {
				permissions.put(module, List.of(ActionType.values()));
			}
		} else if (level == PermissionLevel.VIEW_ONLY) {
			// Restrict all modules to view only
			for (ModuleType module : permissions.keySet()) {
				permissions.put(module, List.of(ActionType.VIEW));
			}
		} else if (level == PermissionLevel.CUSTOM && assignment.getCustomPermissions() != null) {
			// Apply custom permissions
			Map<String, Object> customPerms = assignment.getCustomPermissions();
			for (Map.Entry<String, Object> entry : customPerms.entrySet()) {
				try {
					ModuleType module = ModuleType.valueOf(entry.getKey());
					Object value = entry.getValue();
					List<ActionType> actions = parseActionTypes(value);
					if (actions != null && !actions.isEmpty()) {
						permissions.put(module, actions);
					}
				} catch (IllegalArgumentException e) {
					logger.warn("Invalid module type in custom permissions: {}", entry.getKey());
				}
			}
		}
	}

	/**
	 * Parses action types from various formats.
	 */
	private List<ActionType> parseActionTypes(Object value) {
		List<ActionType> actions = new ArrayList<>();

		if (value instanceof Set) {
			@SuppressWarnings("unchecked")
			Set<String> actionStrings = (Set<String>) value;
			for (String actionStr : actionStrings) {
				try {
					actions.add(ActionType.valueOf(actionStr));
				} catch (IllegalArgumentException e) {
					logger.warn("Invalid action type: {}", actionStr);
				}
			}
		} else if (value instanceof List) {
			@SuppressWarnings("unchecked")
			List<String> actionStrings = (List<String>) value;
			for (String actionStr : actionStrings) {
				try {
					actions.add(ActionType.valueOf(actionStr));
				} catch (IllegalArgumentException e) {
					logger.warn("Invalid action type: {}", actionStr);
				}
			}
		} else if (value instanceof String) {
			String actionStr = (String) value;
			try {
				actions.add(ActionType.valueOf(actionStr));
			} catch (IllegalArgumentException e) {
				logger.warn("Invalid action type: {}", actionStr);
			}
		}

		return actions;
	}

	/**
	 * Evicts permission cache for a user.
	 */
	@CacheEvict(value = {"permissions", "userStations", "stationAccess", "effectivePermissions"}, allEntries = true)
	public void evictPermissionCache() {
		logger.debug("Evicting permission cache");
	}

	/**
	 * Evicts permission cache for a specific user.
	 */
	@CacheEvict(value = {"permissions", "userStations", "stationAccess", "effectivePermissions"}, key = "#userId")
	public void evictPermissionCache(UUID userId) {
		logger.debug("Evicting permission cache for user {}", userId);
	}

	@Override
	@CacheEvict(value = {"permissions", "userStations", "stationAccess", "effectivePermissions"}, allEntries = true)
	@Transactional
	public void evictAllPermissionCaches() {
		logger.info("Evicting all permission-related caches");
	}

	@Override
	@Cacheable(value = "globalPermissions", key = "#userId + '_' + #moduleType + '_' + #actionType")
	public boolean hasGlobalPermission(UUID userId, ModuleType moduleType, ActionType actionType) {
		logger.debug("Checking global permission for user {} for {}:{}", userId, moduleType, actionType);

		User user = userRepository.findById(userId)
				.orElseThrow(() -> new NotFoundException("User not found with id: " + userId));

		// Admin users have all permissions
		if (user.getRole() == UserRole.ADMIN) {
			logger.debug("User {} is ADMIN, granting global permission", userId);
			return true;
		}

		// Check tenant-level permissions first
		if (hasTenantLevelPermission(user, moduleType, actionType)) {
			return true;
		}

		// Check if user has permission for any station
		List<UUID> userStations = getUserStations(userId);
		for (UUID stationId : userStations) {
			if (hasPermission(userId, stationId, moduleType, actionType)) {
				logger.debug("User {} has permission for {}:{} at station {}", userId, moduleType, actionType, stationId);
				return true;
			}
		}

		logger.debug("User {} does not have global permission for {}:{}", userId, moduleType, actionType);
		return false;
	}

}

