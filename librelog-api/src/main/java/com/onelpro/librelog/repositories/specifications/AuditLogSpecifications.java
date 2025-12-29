package com.onelpro.librelog.repositories.specifications;

import com.onelpro.librelog.enums.AuditActionType;
import com.onelpro.librelog.models.AuditLog;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Specifications for building dynamic queries for AuditLog entities.
 * Uses JPA Criteria API for type-safe query building.
 */
public class AuditLogSpecifications {

	/**
	 * Creates a specification with multiple optional filters.
	 *
	 * @param userId optional user ID filter
	 * @param actionType optional action type filter
	 * @param resourceType optional resource type filter
	 * @param stationId optional station ID filter
	 * @param startDate optional start date filter (inclusive)
	 * @param endDate optional end date filter (inclusive)
	 * @return specification for filtering audit logs
	 */
	public static Specification<AuditLog> withFilters(
			UUID userId,
			AuditActionType actionType,
			String resourceType,
			UUID stationId,
			LocalDateTime startDate,
			LocalDateTime endDate) {

		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();

			if (userId != null) {
				predicates.add(cb.equal(root.get("user").get("id"), userId));
			}

			if (actionType != null) {
				predicates.add(cb.equal(root.get("actionType"), actionType));
			}

			if (resourceType != null && !resourceType.isEmpty()) {
				predicates.add(cb.equal(root.get("resourceType"), resourceType));
			}

			if (stationId != null) {
				predicates.add(cb.equal(root.get("station").get("id"), stationId));
			}

			if (startDate != null) {
				predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), startDate));
			}

			if (endDate != null) {
				predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), endDate));
			}

			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}

}
