package com.onelpro.librelog.repositories;

import com.onelpro.librelog.models.ClockTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for ClockTemplate entity operations.
 */
@Repository
public interface ClockTemplateRepository extends JpaRepository<ClockTemplate, UUID> {

	List<ClockTemplate> findByChannelId(UUID channelId);

	List<ClockTemplate> findByChannelIdAndIsActive(UUID channelId, Boolean isActive);

}

