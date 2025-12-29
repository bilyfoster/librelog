package com.onelpro.librelog.enums;

/**
 * Represents the types of actions that can be logged in the audit trail.
 * These action types are used to categorize and filter audit log entries.
 */
public enum AuditActionType {
	/**
	 * Resource creation action.
	 */
	CREATE,
	
	/**
	 * Resource update/modification action.
	 */
	UPDATE,
	
	/**
	 * Resource deletion action.
	 */
	DELETE,
	
	/**
	 * User login action.
	 */
	LOGIN,
	
	/**
	 * User logout action.
	 */
	LOGOUT,
	
	/**
	 * Permission change action (when user permissions are modified).
	 */
	PERMISSION_CHANGE,
	
	/**
	 * Station assignment action (when user is assigned to or removed from a station).
	 */
	STATION_ASSIGNMENT,
	
	/**
	 * Role assignment action (when user's role is changed).
	 */
	ROLE_ASSIGNMENT,
	
	/**
	 * Impersonation start action (when admin starts impersonating a user).
	 */
	IMPERSONATION_START,
	
	/**
	 * Impersonation end action (when admin stops impersonating a user).
	 */
	IMPERSONATION_END,
	
	/**
	 * Session termination action (when a user session is terminated).
	 */
	SESSION_TERMINATED
}

