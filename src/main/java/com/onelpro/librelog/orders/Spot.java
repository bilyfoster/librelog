package com.onelpro.librelog.orders;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "spot")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Spot {

    @Id
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "librtime_file_id")
    private Long librtimeFileId;

    @Column(name = "length_seconds", nullable = false)
    private int lengthSeconds;

    @Column(nullable = false)
    private String label;

    @Column(name = "rotation_kind", nullable = false)
    private String rotationKind;

    @Column(name = "target_show_id")
    private Long targetShowId;

    /**
     * Optional station-local half-open window [start, end) for when this spot may air.
     * When unset, the spot is eligible whenever its cart slot runs (subject to rotation kind).
     */
    @Column(name = "local_window_start_minutes")
    private Integer localWindowStartMinutes;

    @Column(name = "local_window_end_minutes")
    private Integer localWindowEndMinutes;

    /** When set, {@link #localWindowStartMinutes}/{@link #localWindowEndMinutes} are ignored. */
    @Column(name = "day_part_id")
    private UUID dayPartId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
        if (rotationKind == null) rotationKind = "ANY_TIME";
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
