package com.onelpro.librelog.repositories.specifications;

import com.onelpro.librelog.enums.AuditActionType;
import com.onelpro.librelog.models.AuditLog;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogSpecificationsTest {

	@Mock
	private Root<AuditLog> root;

	@Mock
	private CriteriaQuery<?> query;

	@Mock
	private CriteriaBuilder cb;

	@Mock
	private jakarta.persistence.criteria.Path<Object> userPath;

	@Mock
	private jakarta.persistence.criteria.Path<Object> userIdPath;

	@Mock
	private jakarta.persistence.criteria.Path<Object> stationPath;

	@Mock
	private jakarta.persistence.criteria.Path<Object> stationIdPath;

	@Mock
	private jakarta.persistence.criteria.Path<Object> timestampPath;

	@Mock
	private Predicate predicate;

	private UUID userId;
	private UUID stationId;
	private LocalDateTime startDate;
	private LocalDateTime endDate;

	@BeforeEach
	void setUp() {
		userId = UUID.randomUUID();
		stationId = UUID.randomUUID();
		startDate = LocalDateTime.now().minusHours(1);
		endDate = LocalDateTime.now().plusHours(1);
	}

	@Test
	void withFilters_When_AllFiltersProvided_Expect_SpecificationCreated() {
		lenient().when(root.get(anyString())).thenReturn(userPath);
		lenient().when(userPath.get(anyString())).thenReturn(userIdPath);
		lenient().when(cb.equal(any(), any())).thenReturn(predicate);
		lenient().when(cb.greaterThanOrEqualTo(any(Expression.class), any(LocalDateTime.class))).thenReturn(predicate);
		lenient().when(cb.lessThanOrEqualTo(any(Expression.class), any(LocalDateTime.class))).thenReturn(predicate);
		lenient().when(cb.and(any(Predicate[].class))).thenReturn(predicate);

		Specification<AuditLog> spec = AuditLogSpecifications.withFilters(
				userId,
				AuditActionType.CREATE,
				"ORDER",
				stationId,
				startDate,
				endDate
		);

		assertNotNull(spec);
		Predicate result = spec.toPredicate(root, query, cb);
		assertNotNull(result);
	}

	@Test
	void withFilters_When_NoFiltersProvided_Expect_SpecificationCreated() {
		when(cb.and(any(Predicate[].class))).thenReturn(predicate);

		Specification<AuditLog> spec = AuditLogSpecifications.withFilters(
				null,
				null,
				null,
				null,
				null,
				null
		);

		assertNotNull(spec);
		Predicate result = spec.toPredicate(root, query, cb);
		assertNotNull(result);
	}

	@Test
	void withFilters_When_PartialFiltersProvided_Expect_SpecificationCreated() {
		lenient().when(root.get(anyString())).thenReturn(userPath);
		lenient().when(userPath.get(anyString())).thenReturn(userIdPath);
		lenient().when(cb.equal(any(), any())).thenReturn(predicate);
		lenient().when(cb.and(any(Predicate[].class))).thenReturn(predicate);

		Specification<AuditLog> spec = AuditLogSpecifications.withFilters(
				userId,
				AuditActionType.UPDATE,
				null,
				null,
				null,
				null
		);

		assertNotNull(spec);
		Predicate result = spec.toPredicate(root, query, cb);
		assertNotNull(result);
	}

	@Test
	void withFilters_When_DateFiltersProvided_Expect_SpecificationCreated() {
		lenient().when(root.get(anyString())).thenReturn(timestampPath);
		lenient().when(cb.greaterThanOrEqualTo(any(Expression.class), any(LocalDateTime.class))).thenReturn(predicate);
		lenient().when(cb.lessThanOrEqualTo(any(Expression.class), any(LocalDateTime.class))).thenReturn(predicate);
		lenient().when(cb.and(any(Predicate[].class))).thenReturn(predicate);

		Specification<AuditLog> spec = AuditLogSpecifications.withFilters(
				null,
				null,
				null,
				null,
				startDate,
				endDate
		);

		assertNotNull(spec);
		Predicate result = spec.toPredicate(root, query, cb);
		assertNotNull(result);
	}

	@Test
	void withFilters_When_StationFilterProvided_Expect_SpecificationCreated() {
		lenient().when(root.get(anyString())).thenReturn(stationPath);
		lenient().when(stationPath.get(anyString())).thenReturn(stationIdPath);
		lenient().when(cb.equal(any(), any())).thenReturn(predicate);
		lenient().when(cb.and(any(Predicate[].class))).thenReturn(predicate);

		Specification<AuditLog> spec = AuditLogSpecifications.withFilters(
				null,
				null,
				null,
				stationId,
				null,
				null
		);

		assertNotNull(spec);
		Predicate result = spec.toPredicate(root, query, cb);
		assertNotNull(result);
	}

}
