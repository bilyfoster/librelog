package com.onelpro.librelog.enums;

/**
 * Represents the possible roles of a user in the broadcast traffic management system.
 * Roles are aligned with WideOrbit replacement requirements for broadcaster operations.
 */
public enum UserRole {
	/**
	 * System administrator with full access to all features and user management.
	 */
	ADMIN,
	
	/**
	 * Traffic manager responsible for log management, spot scheduling, and placement operations.
	 */
	TRAFFIC_MANAGER,
	
	/**
	 * Sales representative who creates orders, manages client relationships, and handles sales operations.
	 */
	SALES_REP,
	
	/**
	 * Programming staff responsible for content scheduling and program management.
	 */
	PROGRAMMING,
	
	/**
	 * Accounting staff responsible for billing, invoicing, and financial operations.
	 */
	ACCOUNTING,
	
	/**
	 * Operations staff responsible for day-to-day operational tasks and monitoring.
	 */
	OPERATIONS
}

