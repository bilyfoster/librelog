package com.onelpro.librelog.repositories;

import com.onelpro.librelog.models.BreakStructure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for BreakStructure entity operations.
 */
@Repository
public interface BreakStructureRepository extends JpaRepository<BreakStructure, UUID> {

	List<BreakStructure> findByClockTemplateId(UUID clockTemplateId);

}

