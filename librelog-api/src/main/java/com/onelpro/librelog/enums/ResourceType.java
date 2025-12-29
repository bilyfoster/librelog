package com.onelpro.librelog.enums;

/**
 * Represents the types of resources that can be referenced in audit logs.
 * Resource types help categorize audit log entries by the type of entity being acted upon.
 */
public enum ResourceType {
	/**
	 * Order resource type.
	 */
	ORDER,
	
	/**
	 * Log resource type.
	 */
	LOG,
	
	/**
	 * User resource type.
	 */
	USER,
	
	/**
	 * Permission resource type.
	 */
	PERMISSION,
	
	/**
	 * Station resource type.
	 */
	STATION,
	
	/**
	 * Role resource type (predefined roles).
	 */
	ROLE,
	
	/**
	 * Custom role resource type.
	 */
	CUSTOM_ROLE,
	
	/**
	 * Session resource type.
	 */
	SESSION
}

