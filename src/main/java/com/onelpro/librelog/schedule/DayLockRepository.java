package com.onelpro.librelog.schedule;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DayLockRepository extends JpaRepository<DayLock, UUID> {
}
