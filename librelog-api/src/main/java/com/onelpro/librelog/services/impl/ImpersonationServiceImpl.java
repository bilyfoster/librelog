package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.UserResponseDTO;
import com.onelpro.librelog.enums.AuditActionType;
import com.onelpro.librelog.enums.UserRole;
import com.onelpro.librelog.exceptions.BadRequestException;
import com.onelpro.librelog.exceptions.ForbiddenException;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.models.User;
import com.onelpro.librelog.repositories.UserRepository;
import com.onelpro.librelog.services.AuditService;
import com.onelpro.librelog.services.ImpersonationService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of impersonation service.
 * Allows administrators to "View as User" for troubleshooting purposes.
 */
@Service
public class ImpersonationServiceImpl implements ImpersonationService {

	private static final Logger logger = LoggerFactory.getLogger(ImpersonationServiceImpl.class);

	// In-memory storage for active impersonation sessions
	// Key: adminUserId, Value: targetUserId
	private final Map<UUID, UUID> activeImpersonations = new ConcurrentHashMap<>();
	// Key: targetUserId, Value: adminUserId (reverse lookup)
	private final Map<UUID, UUID> impersonatedBy = new ConcurrentHashMap<>();

	private final UserRepository userRepository;
	private final AuditService auditService;
	private final PasswordEncoder passwordEncoder;

	public ImpersonationServiceImpl(
			UserRepository userRepository,
			AuditService auditService,
			PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.auditService = auditService;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional
	public String startImpersonation(UUID adminUserId, UUID targetUserId) {
		logger.info("Starting impersonation: admin {} -> target {}", adminUserId, targetUserId);

		// Validate admin user exists and is an admin
		User admin = userRepository.findById(adminUserId)
				.orElseThrow(() -> new NotFoundException("Admin user not found with id: " + adminUserId));

		if (admin.getRole() != UserRole.ADMIN) {
			throw new ForbiddenException("Only administrators can impersonate users");
		}

		// Validate target user exists
		User targetUser = userRepository.findById(targetUserId)
				.orElseThrow(() -> new NotFoundException("Target user not found with id: " + targetUserId));

		// Prevent impersonating higher-level users (admins cannot impersonate other admins)
		if (targetUser.getRole() == UserRole.ADMIN) {
			throw new BadRequestException("Cannot impersonate another administrator");
		}

		// Check if admin is already impersonating someone
		if (activeImpersonations.containsKey(adminUserId)) {
			throw new BadRequestException("Admin is already impersonating another user. Stop current impersonation first.");
		}

		// Check if target user is already being impersonated
		if (impersonatedBy.containsKey(targetUserId)) {
			throw new BadRequestException("Target user is already being impersonated by another admin");
		}

		// Start impersonation
		activeImpersonations.put(adminUserId, targetUserId);
		impersonatedBy.put(targetUserId, adminUserId);

		// Generate impersonation token (for session management)
		String impersonationToken = generateImpersonationToken(adminUserId, targetUserId);

		// Audit log
		Map<String, Object> newValue = new HashMap<>();
		newValue.put("adminUserId", adminUserId);
		newValue.put("adminEmail", admin.getEmail());
		newValue.put("targetUserId", targetUserId);
		newValue.put("targetEmail", targetUser.getEmail());
		newValue.put("timestamp", LocalDateTime.now());

		auditService.logAction(
				AuditActionType.IMPERSONATION_START,
				"User",
				targetUserId,
				null,
				newValue,
				adminUserId,
				null,
				getClientIpAddress(),
				getUserAgent()
		);

		logger.info("Impersonation started: admin {} -> target {}", adminUserId, targetUserId);

		return impersonationToken;
	}

	@Override
	@Transactional
	public void stopImpersonation(UUID adminUserId) {
		logger.info("Stopping impersonation for admin: {}", adminUserId);

		UUID targetUserId = activeImpersonations.remove(adminUserId);
		if (targetUserId == null) {
			throw new BadRequestException("No active impersonation found for this admin");
		}

		impersonatedBy.remove(targetUserId);

		// Audit log
		User admin = userRepository.findById(adminUserId)
				.orElseThrow(() -> new NotFoundException("Admin user not found with id: " + adminUserId));
		User targetUser = userRepository.findById(targetUserId)
				.orElseThrow(() -> new NotFoundException("Target user not found with id: " + targetUserId));

		Map<String, Object> previousValue = new HashMap<>();
		previousValue.put("adminUserId", adminUserId);
		previousValue.put("adminEmail", admin.getEmail());
		previousValue.put("targetUserId", targetUserId);
		previousValue.put("targetEmail", targetUser.getEmail());

		auditService.logAction(
				AuditActionType.IMPERSONATION_END,
				"User",
				targetUserId,
				previousValue,
				null,
				adminUserId,
				null,
				getClientIpAddress(),
				getUserAgent()
		);

		logger.info("Impersonation stopped: admin {} -> target {}", adminUserId, targetUserId);
	}

	@Override
	public boolean isImpersonating(UUID userId) {
		return impersonatedBy.containsKey(userId);
	}

	@Override
	public UUID getImpersonatingAdmin(UUID userId) {
		return impersonatedBy.get(userId);
	}

	@Override
	public UserResponseDTO getImpersonatedUser(UUID adminUserId) {
		UUID targetUserId = activeImpersonations.get(adminUserId);
		if (targetUserId == null) {
			return null;
		}

		User targetUser = userRepository.findById(targetUserId)
				.orElseThrow(() -> new NotFoundException("Target user not found with id: " + targetUserId));

		return mapToResponseDTO(targetUser);
	}

	/**
	 * Generates a secure impersonation token.
	 */
	private String generateImpersonationToken(UUID adminUserId, UUID targetUserId) {
		// Generate a token that includes both admin and target user IDs
		// In a production system, this would be a JWT or similar secure token
		String rawToken = adminUserId.toString() + ":" + targetUserId.toString() + ":" + UUID.randomUUID();
		return passwordEncoder.encode(rawToken);
	}

	/**
	 * Maps User entity to UserResponseDTO.
	 */
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

