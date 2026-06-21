package com.onelpro.librelog.carts;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClockTemplateRepository extends JpaRepository<ClockTemplate, UUID> {
    List<ClockTemplate> findByStationIdOrderByNameAsc(UUID stationId);
}
