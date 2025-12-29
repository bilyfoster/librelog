package com.onelpro.librelog.repositories;

import com.onelpro.librelog.enums.AuditActionType;
import com.onelpro.librelog.models.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for AuditLog entity operations.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

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
	 * Custom query to find audit logs with multiple filters.
	 */
	@Query("SELECT al FROM AuditLog al WHERE " +
			"(:userId IS NULL OR al.user.id = :userId) AND " +
			"(:actionType IS NULL OR al.actionType = :actionType) AND " +
			"(:resourceType IS NULL OR al.resourceType = :resourceType) AND " +
			"(:stationId IS NULL OR al.station.id = :stationId) AND " +
			"(:startDate IS NULL OR al.timestamp >= :startDate) AND " +
			"(:endDate IS NULL OR al.timestamp <= :endDate) " +
			"ORDER BY al.timestamp DESC")
	Page<AuditLog> findWithFilters(
			@Param("userId") UUID userId,
			@Param("actionType") AuditActionType actionType,
			@Param("resourceType") String resourceType,
			@Param("stationId") UUID stationId,
			@Param("startDate") LocalDateTime startDate,
			@Param("endDate") LocalDateTime endDate,
			Pageable pageable);

}

