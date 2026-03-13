package com.onelpro.librelog.repositories;

import com.onelpro.librelog.models.SpotCopy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for SpotCopy entity.
 */
@Repository
public interface SpotCopyRepository extends JpaRepository<SpotCopy, UUID> {

	/**
	 * Find all copy versions for a campaign.
	 */
	List<SpotCopy> findByCampaignIdOrderByVersionNumberDesc(UUID campaignId);

	/**
	 * Find latest approved copy for a campaign.
	 */
	Optional<SpotCopy> findFirstByCampaignIdAndStatusOrderByVersionNumberDesc(UUID campaignId, String status);

	/**
	 * Find latest version number for a campaign.
	 */
	@Query("SELECT MAX(sc.versionNumber) FROM SpotCopy sc WHERE sc.campaign.id = :campaignId")
	Optional<Integer> findMaxVersionNumberByCampaignId(@Param("campaignId") UUID campaignId);

	/**
	 * Find copy by status.
	 */
	List<SpotCopy> findByStatus(String status);

	/**
	 * Count copy versions for a campaign.
	 */
	long countByCampaignId(UUID campaignId);

}
