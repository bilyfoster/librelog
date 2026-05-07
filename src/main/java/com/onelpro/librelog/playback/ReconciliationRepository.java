package com.onelpro.librelog.playback;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReconciliationRepository extends JpaRepository<Reconciliation, UUID> {
    List<Reconciliation> findByScheduleItemIdIn(List<UUID> scheduleItemIds);
    void deleteByScheduleItemIdIn(List<UUID> scheduleItemIds);
}
