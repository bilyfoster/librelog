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
 */
public class AuditLogSpecifications {

	/**
	 * Build a specification for filtering audit logs with multiple optional filters.
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

			if (resourceType != null) {
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

