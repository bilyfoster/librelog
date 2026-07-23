package com.onelpro.librelog.media;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MediaPackageRepository extends JpaRepository<MediaPackage, UUID> {
    List<MediaPackage> findByStationIdOrderByCreatedAtDesc(UUID stationId);
    List<MediaPackage> findByStationIdAndStatusOrderByCreatedAtDesc(UUID stationId, String status);
}
