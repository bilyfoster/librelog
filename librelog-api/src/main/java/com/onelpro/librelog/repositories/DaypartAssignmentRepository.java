package com.onelpro.librelog.repositories;

import com.onelpro.librelog.models.DaypartAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for DaypartAssignment entity operations.
 */
@Repository
public interface DaypartAssignmentRepository extends JpaRepository<DaypartAssignment, UUID> {

	List<DaypartAssignment> findByDaypartId(UUID daypartId);

	List<DaypartAssignment> findByClockTemplateId(UUID clockTemplateId);

	Optional<DaypartAssignment> findByDaypartIdAndClockTemplateId(UUID daypartId, UUID clockTemplateId);

	void deleteByDaypartId(UUID daypartId);

	void deleteByClockTemplateId(UUID clockTemplateId);

}

