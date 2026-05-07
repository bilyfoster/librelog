package com.onelpro.librelog.playback;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PlaybackLogRepository extends JpaRepository<PlaybackLogEntry, UUID> {
    List<PlaybackLogEntry> findByStationIdAndPlayedAtBetweenOrderByPlayedAtAsc(
            UUID stationId, Instant from, Instant to);
}
