package com.onelpro.librelog.repositories;

import com.onelpro.librelog.enums.AuditActionType;
import com.onelpro.librelog.models.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for AuditLog entity operations.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID>, JpaSpecificationExecutor<AuditLog> {

	/**
	 * Find all audit logs for a specific user.
	 */
	List<AuditLog> findByUserId(UUID userId);

	/**
	 * Find all audit logs for a specific user with pagination.
	 */
	Page<AuditLog> findByUserId(UUID userId, Pageable pageable);

	/**
	 * Find all audit logs by action type.
	 */
	List<AuditLog> findByActionType(AuditActionType actionType);

	/**
	 * Find all audit logs by action type with pagination.
	 */
	Page<AuditLog> findByActionType(AuditActionType actionType, Pageable pageable);

	/**
	 * Find all audit logs by resource type.
	 */
	List<AuditLog> findByResourceType(String resourceType);

	/**
	 * Find all audit logs by resource type with pagination.
	 */
	Page<AuditLog> findByResourceType(String resourceType, Pageable pageable);

	/**
	 * Find all audit logs within a time range.
	 */
	List<AuditLog> findByTimestampBetween(LocalDateTime startDate, LocalDateTime endDate);

	/**
	 * Find all audit logs within a time range with pagination.
	 */
	Page<AuditLog> findByTimestampBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

	/**
	 * Find all audit logs for a specific station.
	 */
	List<AuditLog> findByStationId(UUID stationId);

	/**
	 * Find all audit logs for a specific station with pagination.
	 */
	Page<AuditLog> findByStationId(UUID stationId, Pageable pageable);

	/**
	 * Find all audit logs for a specific user within a time range.
	 */
	List<AuditLog> findByUserIdAndTimestampBetween(UUID userId, LocalDateTime startDate, LocalDateTime endDate);

	/**
	 * Find all audit logs for a specific user within a time range with pagination.
	 */
	Page<AuditLog> findByUserIdAndTimestampBetween(UUID userId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

	/**
	 * Find all audit logs by action type and resource type.
	 */
	Page<AuditLog> findByActionTypeAndResourceType(AuditActionType actionType, String resourceType, Pageable pageable);

	/**
	 * Find all audit logs for a specific resource.
	 */
	List<AuditLog> findByResourceTypeAndResourceId(String resourceType, UUID resourceId);

	/**
	 * Find all audit logs for a specific resource with pagination.
	 */
	Page<AuditLog> findByResourceTypeAndResourceId(String resourceType, UUID resourceId, Pageable pageable);

	/**
	 * Find audit logs with multiple filters using Specifications.
	 * This method uses Criteria API to avoid PostgreSQL parameter type inference issues.
	 * 
	 * @param userId optional user ID filter
	 * @param actionType optional action type filter
	 * @param resourceType optional resource type filter
	 * @param stationId optional station ID filter
	 * @param startDate optional start date filter (inclusive)
	 * @param endDate optional end date filter (inclusive)
	 * @param pageable pagination information
	 * @return page of filtered audit logs
	 */
	default Page<AuditLog> findWithFilters(
			UUID userId,
			AuditActionType actionType,
			String resourceType,
			UUID stationId,
			LocalDateTime startDate,
			LocalDateTime endDate,
			Pageable pageable) {
		Specification<AuditLog> spec = com.onelpro.librelog.repositories.specifications.AuditLogSpecifications
				.withFilters(userId, actionType, resourceType, stationId, startDate, endDate);
		return findAll(spec, pageable);
	}

}

