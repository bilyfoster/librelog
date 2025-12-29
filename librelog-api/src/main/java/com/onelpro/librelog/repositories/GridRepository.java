package com.onelpro.librelog.repositories;

import com.onelpro.librelog.models.Grid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for Grid entity operations.
 */
@Repository
public interface GridRepository extends JpaRepository<Grid, UUID> {

	List<Grid> findByChannelId(UUID channelId);

	List<Grid> findByIsActiveTrue();

	List<Grid> findByChannelIdAndIsActiveTrue(UUID channelId);

}

