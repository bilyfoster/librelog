package com.onelpro.librelog.enums;

/**
 * Represents the possible statuses of an order in the broadcast traffic management system.
 */
public enum OrderStatus {
	/**
	 * Order has been created but not yet submitted for processing.
	 */
	DRAFT,
	
	/**
	 * Order has been submitted and is pending approval.
	 */
	PENDING,
	
	/**
	 * Order has been approved and is ready for scheduling.
	 */
	APPROVED,
	
	/**
	 * Order is currently being scheduled (spots are being placed).
	 */
	SCHEDULING,
	
	/**
	 * Order has been scheduled and spots have been placed.
	 */
	SCHEDULED,
	
	/**
	 * Order has been cancelled.
	 */
	CANCELLED,
	
	/**
	 * Order has been completed (all spots have aired).
	 */
	COMPLETED
}

