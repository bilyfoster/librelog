package com.onelpro.librelog.services;

import com.onelpro.librelog.dto.UserSessionResponseDTO;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for session management operations.
 * Handles user session creation, tracking, and termination.
 */
public interface SessionService {

	/**
	 * Creates a new user session.
	 *
	 * @param userId the user ID
	 * @param ipAddress the IP address
	 * @param userAgent the user agent string
	 * @return the session token
	 */
	String createSession(UUID userId, String ipAddress, String userAgent);

	/**
	 * Updates the last activity timestamp for a session.
	 *
	 * @param sessionId the session ID
	 */
	void updateLastActivity(UUID sessionId);

	/**
	 * Terminates a specific session.
	 *
	 * @param sessionId the session ID
	 */
	void terminateSession(UUID sessionId);

	/**
	 * Terminates all sessions for a user.
	 *
	 * @param userId the user ID
	 */
	void terminateAllUserSessions(UUID userId);

	/**
	 * Gets all currently active sessions.
	 *
	 * @return list of active sessions
	 */
	List<UserSessionResponseDTO> getActiveSessions();

	/**
	 * Gets all sessions for a user.
	 *
	 * @param userId the user ID
	 * @return list of user sessions
	 */
	List<UserSessionResponseDTO> getUserSessions(UUID userId);

	/**
	 * Updates the current resource being accessed in a session.
	 *
	 * @param sessionId the session ID
	 * @param stationId the station ID (optional)
	 * @param resourceId the resource ID (optional)
	 */
	void updateCurrentResource(UUID sessionId, UUID stationId, UUID resourceId);

	/**
	 * Gets a session by session ID.
	 *
	 * @param sessionId the session ID
	 * @return the session, or null if not found
	 */
	UserSessionResponseDTO getSession(UUID sessionId);

	/**
	 * Gets a session by session token.
	 *
	 * @param sessionToken the session token
	 * @return the session, or null if not found
	 */
	UserSessionResponseDTO getSessionByToken(String sessionToken);

}

