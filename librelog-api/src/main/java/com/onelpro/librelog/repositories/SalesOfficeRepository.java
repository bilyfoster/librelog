package com.onelpro.librelog.repositories;

import com.onelpro.librelog.models.SalesOffice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for SalesOffice entity operations.
 */
@Repository
public interface SalesOfficeRepository extends JpaRepository<SalesOffice, UUID> {

	List<SalesOffice> findBySalesRegionId(UUID salesRegionId);

}

