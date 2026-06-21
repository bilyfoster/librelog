package com.onelpro.librelog.carts;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface ClockTemplateSlotRepository extends JpaRepository<ClockTemplateSlot, UUID> {
    List<ClockTemplateSlot> findByClockTemplateIdOrderByPositionAsc(UUID clockTemplateId);

    @Transactional
    void deleteByClockTemplateId(UUID clockTemplateId);
}
