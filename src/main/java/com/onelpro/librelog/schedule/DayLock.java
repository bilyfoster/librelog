package com.onelpro.librelog.schedule;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "day_lock")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DayLock {

    @Id
    @Column(name = "schedule_day_id")
    private UUID scheduleDayId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "acquired_at", nullable = false)
    private Instant acquiredAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
}
