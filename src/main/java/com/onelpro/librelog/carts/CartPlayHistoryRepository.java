package com.onelpro.librelog.carts;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface CartPlayHistoryRepository extends JpaRepository<CartPlayHistory, UUID> {

    @Query("SELECT h FROM CartPlayHistory h WHERE h.stationId = :stationId AND h.playedAt >= :since")
    List<CartPlayHistory> recentForStation(@Param("stationId") UUID stationId,
                                           @Param("since") Instant since);
}
