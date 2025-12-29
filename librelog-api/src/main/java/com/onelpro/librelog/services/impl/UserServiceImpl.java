package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.*;
import com.onelpro.librelog.enums.AuditActionType;
import com.onelpro.librelog.exceptions.BadRequestException;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.models.User;
import com.onelpro.librelog.repositories.UserRepository;
import com.onelpro.librelog.services.AuditService;
import com.onelpro.librelog.services.SessionService;
import com.onelpro.librelog.services.UserService;
import com.onelpro.librelog.services.UserStationAssignmentService;
import com.onelpro.librelog.utils.PasswordValidator;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
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
 * Implementation of user management service.
 * Handles user CRUD operations with audit logging and station assignment integration.
 */
@Service
public class UserServiceImpl implements UserService {

	private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuditService auditService;
	private final UserStationAssignmentService userStationAssignmentService;
	private final SessionService sessionService;

	public UserServiceImpl(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			AuditService auditService,
			UserStationAssignmentService userStationAssignmentService,
			SessionService sessionService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.auditService = auditService;
		this.userStationAssignmentService = userStationAssignmentService;
		this.sessionService = sessionService;
	}

	@Override
	@Transactional
	public UserResponseDTO create(UserRequestDTO request) {
		logger.info("Creating user with email: {}", request.getEmail());

		if (userRepository.existsByEmail(request.getEmail())) {
			throw new BadRequestException("Email already registered");
		}

		if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
			throw new BadRequestException("Password is required");
		}

		if (!PasswordValidator.isValid(request.getPassword())) {
			throw new BadRequestException("Password must be between 8 and 128 characters long, contain at least one uppercase letter, one lowercase letter, one digit, and one special character.");
		}

		User user = User.builder()
				.email(request.getEmail())
				.password(passwordEncoder.encode(request.getPassword()))
				.status(request.getStatus())
				.role(request.getRole())
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();

		User savedUser = userRepository.save(user);
		logger.info("User created successfully with ID: {}", savedUser.getId());

		// Create station assignments if provided
		if (request.getStationAssignments() != null && !request.getStationAssignments().isEmpty()) {
			for (UserStationAssignmentRequestDTO assignmentRequest : request.getStationAssignments()) {
				assignmentRequest.setUserId(savedUser.getId());
				userStationAssignmentService.assignUserToStation(assignmentRequest);
			}
		}

		// Audit log
		UUID currentUserId = getCurrentUserId();
		Map<String, Object> newValue = new HashMap<>();
		newValue.put("userId", savedUser.getId());
		newValue.put("email", savedUser.getEmail());
		newValue.put("role", savedUser.getRole().name());
		newValue.put("status", savedUser.getStatus().name());

		auditService.logAction(
				AuditActionType.CREATE,
				"User",
				savedUser.getId(),
				null,
				newValue,
				currentUserId,
				null,
				getClientIpAddress(),
				getUserAgent()
		);

		return mapToResponseDTO(savedUser);
	}

	@Override
	public UserResponseDTO getById(UUID id) {
		logger.info("Fetching user with ID: {}", id);
		User user = userRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("User not found with id: " + id));
		return mapToResponseDTO(user);
	}

	@Override
	public List<UserResponseDTO> getAll() {
		logger.info("Fetching all users");
		return userRepository.findAll().stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public UserResponseDTO update(UUID id, UserRequestDTO request) {
		logger.info("Updating user with ID: {}", id);
		User user = userRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("User not found with id: " + id));

		// Check if email is being changed and if it's already taken
		if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
			throw new BadRequestException("Email already registered");
		}

		user.setEmail(request.getEmail());
		user.setStatus(request.getStatus());
		user.setRole(request.getRole());
		user.setUpdatedAt(LocalDateTime.now());

		// Update password only if provided
		if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
			if (!PasswordValidator.isValid(request.getPassword())) {
				throw new BadRequestException("Password must be between 8 and 128 characters long, contain at least one uppercase letter, one lowercase letter, one digit, and one special character.");
			}
			user.setPassword(passwordEncoder.encode(request.getPassword()));
		}

		// Store previous value for audit log
		Map<String, Object> previousValue = new HashMap<>();
		previousValue.put("email", user.getEmail());
		previousValue.put("role", user.getRole().name());
		previousValue.put("status", user.getStatus().name());

		User updatedUser = userRepository.save(user);
		logger.info("User updated successfully with ID: {}", updatedUser.getId());

		// Audit log
		UUID currentUserId = getCurrentUserId();
		Map<String, Object> newValue = new HashMap<>();
		newValue.put("email", updatedUser.getEmail());
		newValue.put("role", updatedUser.getRole().name());
		newValue.put("status", updatedUser.getStatus().name());
		if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
			newValue.put("passwordChanged", true);
		}

		auditService.logAction(
				AuditActionType.UPDATE,
				"User",
				updatedUser.getId(),
				previousValue,
				newValue,
				currentUserId,
				null,
				getClientIpAddress(),
				getUserAgent()
		);

		return mapToResponseDTO(updatedUser);
	}

	@Override
	@Transactional
	public void delete(UUID id) {
		logger.info("Deleting user with ID: {}", id);
		User user = userRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("User not found with id: " + id));

		// Store previous value for audit log
		Map<String, Object> previousValue = new HashMap<>();
		previousValue.put("userId", user.getId());
		previousValue.put("email", user.getEmail());
		previousValue.put("role", user.getRole().name());
		previousValue.put("status", user.getStatus().name());

		userRepository.deleteById(id);
		logger.info("User deleted successfully with ID: {}", id);

		// Audit log
		UUID currentUserId = getCurrentUserId();
		auditService.logAction(
				AuditActionType.DELETE,
				"User",
				id,
				previousValue,
				null,
				currentUserId,
				null,
				getClientIpAddress(),
				getUserAgent()
		);
	}

	@Override
	@Transactional(readOnly = true)
	public UserDetailResponseDTO getUserDetail(UUID userId) {
		logger.debug("Getting detailed user information for: {}", userId);

		User user = userRepository.findById(userId)
				.orElseThrow(() -> new NotFoundException("User not found with id: " + userId));

		// Get station assignments
		List<UserStationAssignmentResponseDTO> stationAssignments =
				userStationAssignmentService.getUserStationAssignments(userId);

		// Get active sessions
		List<UserSessionResponseDTO> activeSessions = sessionService.getUserSessions(userId).stream()
				.filter(session -> Boolean.TRUE.equals(session.getIsActive()))
				.collect(Collectors.toList());

		// Get recent audit logs (last 10)
		AuditLogFilterDTO auditFilter = AuditLogFilterDTO.builder()
				.userId(userId)
				.page(0)
				.size(10)
				.build();
		List<AuditLogResponseDTO> recentAuditLogs = auditService.getAuditLogs(auditFilter).getContent();

		return UserDetailResponseDTO.builder()
				.id(user.getId())
				.email(user.getEmail())
				.status(user.getStatus())
				.role(user.getRole())
				.createdAt(user.getCreatedAt())
				.updatedAt(user.getUpdatedAt())
				.stationAssignments(stationAssignments)
				.activeSessions(activeSessions)
				.recentAuditLogs(recentAuditLogs)
				.build();
	}

	@Override
	@Transactional(readOnly = true)
	public UserResponseDTO getUserWithAssignments(UUID userId) {
		logger.debug("Getting user with assignments for: {}", userId);

		User user = userRepository.findById(userId)
				.orElseThrow(() -> new NotFoundException("User not found with id: " + userId));

		// Get station assignments
		List<UserStationAssignmentResponseDTO> assignments =
				userStationAssignmentService.getUserStationAssignments(userId);

		// Map to summary DTOs
		List<UserResponseDTO.StationAssignmentSummaryDTO> summaryList = assignments.stream()
				.map(assignment -> UserResponseDTO.StationAssignmentSummaryDTO.builder()
						.stationId(assignment.getStationId())
						.stationName(assignment.getStationName())
						.permissionLevel(assignment.getPermissionLevel().name())
						.build())
				.collect(Collectors.toList());

		return UserResponseDTO.builder()
				.id(user.getId())
				.email(user.getEmail())
				.status(user.getStatus())
				.role(user.getRole())
				.createdAt(user.getCreatedAt())
				.updatedAt(user.getUpdatedAt())
				.stationAssignmentsSummary(summaryList)
				.build();
	}

	@Override
	@Transactional
	public UserResponseDTO updateUserWithStations(UUID userId, UserRequestDTO request,
	                                              List<UserStationAssignmentRequestDTO> stationAssignments) {
		logger.info("Updating user with stations: {}", userId);

		// Update user
		UserResponseDTO updatedUser = update(userId, request);

		// Update station assignments
		if (stationAssignments != null) {
			// Remove all existing assignments
			List<UserStationAssignmentResponseDTO> existingAssignments =
					userStationAssignmentService.getUserStationAssignments(userId);
			for (UserStationAssignmentResponseDTO assignment : existingAssignments) {
				userStationAssignmentService.removeUserFromStation(userId, assignment.getStationId());
			}

			// Create new assignments
			for (UserStationAssignmentRequestDTO assignmentRequest : stationAssignments) {
				assignmentRequest.setUserId(userId);
				userStationAssignmentService.assignUserToStation(assignmentRequest);
			}
		}

		return getUserWithAssignments(userId);
	}

	private UserResponseDTO mapToResponseDTO(User user) {
		return UserResponseDTO.builder()
				.id(user.getId())
				.email(user.getEmail())
				.status(user.getStatus())
				.role(user.getRole())
				.createdAt(user.getCreatedAt())
				.updatedAt(user.getUpdatedAt())
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

