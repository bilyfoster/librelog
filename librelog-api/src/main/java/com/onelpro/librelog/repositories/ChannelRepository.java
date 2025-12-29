package com.onelpro.librelog.repositories;

import com.onelpro.librelog.enums.FormatType;
import com.onelpro.librelog.models.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for Channel entity operations.
 */
@Repository
public interface ChannelRepository extends JpaRepository<Channel, UUID> {

	List<Channel> findByStationId(UUID stationId);

	List<Channel> findByStationIdAndIsActive(UUID stationId, Boolean isActive);

	List<Channel> findByStationIdAndFormatType(UUID stationId, FormatType formatType);

}

