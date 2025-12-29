package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.UserSessionResponseDTO;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.models.Station;
import com.onelpro.librelog.models.User;
import com.onelpro.librelog.models.UserSession;
import com.onelpro.librelog.repositories.StationRepository;
import com.onelpro.librelog.repositories.UserRepository;
import com.onelpro.librelog.repositories.UserSessionRepository;
import com.onelpro.librelog.services.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of session service.
 * Handles user session creation, tracking, and termination.
 */
@Service
public class SessionServiceImpl implements SessionService {

	private static final Logger logger = LoggerFactory.getLogger(SessionServiceImpl.class);

	// Session timeout: 30 minutes of inactivity
	private static final int SESSION_TIMEOUT_MINUTES = 30;
	// Maximum session duration: 8 hours
	private static final int MAX_SESSION_DURATION_HOURS = 8;

	private final UserSessionRepository userSessionRepository;
	private final UserRepository userRepository;
	private final StationRepository stationRepository;
	private final PasswordEncoder passwordEncoder;

	public SessionServiceImpl(
			UserSessionRepository userSessionRepository,
			UserRepository userRepository,
			StationRepository stationRepository,
			PasswordEncoder passwordEncoder) {
		this.userSessionRepository = userSessionRepository;
		this.userRepository = userRepository;
		this.stationRepository = stationRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional
	public String createSession(UUID userId, String ipAddress, String userAgent) {
		logger.info("Creating session for user: {}", userId);

		User user = userRepository.findById(userId)
				.orElseThrow(() -> new NotFoundException("User not found with id: " + userId));

		// Generate secure session token
		String rawToken = generateSecureToken();
		String hashedToken = passwordEncoder.encode(rawToken);

		LocalDateTime now = LocalDateTime.now();
		LocalDateTime expiresAt = now.plusHours(MAX_SESSION_DURATION_HOURS);

		UserSession session = UserSession.builder()
				.user(user)
				.sessionToken(hashedToken)
				.loginTimestamp(now)
				.lastActivityTimestamp(now)
				.ipAddress(ipAddress)
				.userAgent(userAgent != null && userAgent.length() > 500 ? userAgent.substring(0, 500) : userAgent)
				.isActive(true)
				.expiresAt(expiresAt)
				.build();

		UserSession savedSession = userSessionRepository.save(session);
		logger.info("Session created with ID: {}", savedSession.getId());

		// Return the raw token (client will use this, we store the hash)
		return rawToken;
	}

	@Override
	@Transactional
	public void updateLastActivity(UUID sessionId) {
		logger.debug("Updating last activity for session: {}", sessionId);

		UserSession session = userSessionRepository.findById(sessionId)
				.orElseThrow(() -> new NotFoundException("Session not found with id: " + sessionId));

		if (!session.getIsActive()) {
			logger.warn("Attempted to update inactive session: {}", sessionId);
			return;
		}

		// Check if session has expired
		if (LocalDateTime.now().isAfter(session.getExpiresAt())) {
			logger.info("Session expired: {}", sessionId);
			session.setIsActive(false);
			userSessionRepository.save(session);
			return;
		}

		// Check session timeout (30 minutes of inactivity)
		LocalDateTime timeoutThreshold = session.getLastActivityTimestamp().plusMinutes(SESSION_TIMEOUT_MINUTES);
		if (LocalDateTime.now().isAfter(timeoutThreshold)) {
			logger.info("Session timed out due to inactivity: {}", sessionId);
			session.setIsActive(false);
			userSessionRepository.save(session);
			return;
		}

		session.setLastActivityTimestamp(LocalDateTime.now());
		userSessionRepository.save(session);
	}

	@Override
	@Transactional
	public void terminateSession(UUID sessionId) {
		logger.info("Terminating session: {}", sessionId);

		UserSession session = userSessionRepository.findById(sessionId)
				.orElseThrow(() -> new NotFoundException("Session not found with id: " + sessionId));

		session.setIsActive(false);
		userSessionRepository.save(session);
		logger.info("Session terminated: {}", sessionId);
	}

	@Override
	@Transactional
	public void terminateAllUserSessions(UUID userId) {
		logger.info("Terminating all sessions for user: {}", userId);

		List<UserSession> sessions = userSessionRepository.findByUserIdAndIsActiveTrue(userId);
		for (UserSession session : sessions) {
			session.setIsActive(false);
		}
		userSessionRepository.saveAll(sessions);
		logger.info("Terminated {} sessions for user: {}", sessions.size(), userId);
	}

	@Override
	public List<UserSessionResponseDTO> getActiveSessions() {
		logger.debug("Getting all active sessions");

		List<UserSession> sessions = userSessionRepository.findByIsActiveTrue();
		return sessions.stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	public List<UserSessionResponseDTO> getUserSessions(UUID userId) {
		logger.debug("Getting sessions for user: {}", userId);

		List<UserSession> sessions = userSessionRepository.findByUserId(userId);
		return sessions.stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public void updateCurrentResource(UUID sessionId, UUID stationId, UUID resourceId) {
		logger.debug("Updating current resource for session: {} - station: {}, resource: {}", sessionId, stationId, resourceId);

		UserSession session = userSessionRepository.findById(sessionId)
				.orElseThrow(() -> new NotFoundException("Session not found with id: " + sessionId));

		if (!session.getIsActive()) {
			logger.warn("Attempted to update resource for inactive session: {}", sessionId);
			return;
		}

		Station station = null;
		if (stationId != null) {
			station = stationRepository.findById(stationId)
					.orElseThrow(() -> new NotFoundException("Station not found with id: " + stationId));
		}

		session.setCurrentStation(station);
		session.setCurrentResourceId(resourceId);
		session.setLastActivityTimestamp(LocalDateTime.now());

		userSessionRepository.save(session);
	}

	@Override
	public UserSessionResponseDTO getSession(UUID sessionId) {
		logger.debug("Getting session: {}", sessionId);

		UserSession session = userSessionRepository.findById(sessionId)
				.orElse(null);

		if (session == null) {
			return null;
		}

		return mapToResponseDTO(session);
	}

	@Override
	public UserSessionResponseDTO getSessionByToken(String sessionToken) {
		logger.debug("Getting session by token");

		// Since tokens are hashed, we need to check all active sessions
		// This is not ideal for performance, but necessary for security
		List<UserSession> activeSessions = userSessionRepository.findByIsActiveTrue();
		for (UserSession session : activeSessions) {
			if (passwordEncoder.matches(sessionToken, session.getSessionToken())) {
				return mapToResponseDTO(session);
			}
		}

		return null;
	}

	/**
	 * Scheduled task to clean up expired sessions.
	 * Runs every hour.
	 */
	@Scheduled(fixedRate = 3600000) // 1 hour in milliseconds
	@Transactional
	public void cleanupExpiredSessions() {
		logger.info("Running scheduled cleanup of expired sessions");

		LocalDateTime now = LocalDateTime.now();
		List<UserSession> expiredSessions = userSessionRepository.findExpiredActiveSessions(now);

		for (UserSession session : expiredSessions) {
			session.setIsActive(false);
			logger.debug("Deactivated expired session: {}", session.getId());
		}

		userSessionRepository.saveAll(expiredSessions);
		logger.info("Cleaned up {} expired sessions", expiredSessions.size());

		// Also delete old inactive sessions (older than 30 days)
		LocalDateTime cutoffDate = now.minusDays(30);
		userSessionRepository.deleteByExpiresAtBefore(cutoffDate);
	}

	/**
	 * Generates a secure random token for session identification.
	 */
	private String generateSecureToken() {
		// Generate a UUID-based token with additional randomness
		return UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
	}

	/**
	 * Maps UserSession entity to UserSessionResponseDTO.
	 */
	private UserSessionResponseDTO mapToResponseDTO(UserSession session) {
		LocalDateTime now = LocalDateTime.now();
		Duration duration = Duration.between(session.getLoginTimestamp(), now);

		return UserSessionResponseDTO.builder()
				.id(session.getId())
				.userId(session.getUser().getId())
				.userEmail(session.getUser().getEmail())
				.loginTimestamp(session.getLoginTimestamp())
				.lastActivityTimestamp(session.getLastActivityTimestamp())
				.ipAddress(session.getIpAddress())
				.userAgent(session.getUserAgent())
				.currentStationId(session.getCurrentStation() != null ? session.getCurrentStation().getId() : null)
				.currentStationName(session.getCurrentStation() != null ? session.getCurrentStation().getName() : null)
				.currentResourceId(session.getCurrentResourceId())
				.isActive(session.getIsActive())
				.expiresAt(session.getExpiresAt())
				.sessionDuration(duration)
				.build();
	}

}

