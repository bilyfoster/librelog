package com.onelpro.librelog.repositories;

import com.onelpro.librelog.models.FixedAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for FixedAsset entity operations.
 */
@Repository
public interface FixedAssetRepository extends JpaRepository<FixedAsset, UUID> {

	List<FixedAsset> findByClockTemplateId(UUID clockTemplateId);

}

