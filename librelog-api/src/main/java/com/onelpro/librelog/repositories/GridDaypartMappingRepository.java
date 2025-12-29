package com.onelpro.librelog.repositories;

import com.onelpro.librelog.models.GridDaypartMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for GridDaypartMapping entity operations.
 */
@Repository
public interface GridDaypartMappingRepository extends JpaRepository<GridDaypartMapping, UUID> {

	List<GridDaypartMapping> findByGridId(UUID gridId);

	List<GridDaypartMapping> findByDaypartId(UUID daypartId);

	List<GridDaypartMapping> findByGridIdAndDayOfWeek(UUID gridId, Integer dayOfWeek);

	void deleteByGridId(UUID gridId);

	void deleteByDaypartId(UUID daypartId);

}

