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

    /** Production lifecycle. Only {@link #STATUS_APPROVED}/{@link #STATUS_TRAFFICKED} spots may air. */
    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PRODUCED = "PRODUCED";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_TRAFFICKED = "TRAFFICKED";

    /** Statuses a user may set directly; TRAFFICKED is applied by the system at push time. */
    public static final java.util.Set<String> MANUAL_STATUSES =
            java.util.Set.of(STATUS_DRAFT, STATUS_PRODUCED, STATUS_APPROVED);

    @Id
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Builder.Default
    @Column(nullable = false)
    private String status = STATUS_DRAFT;

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
        if (status == null) status = STATUS_DRAFT;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    /** Approved or already trafficked — i.e. cleared to air. */
    public static boolean isAirable(Spot s) {
        return s != null && (STATUS_APPROVED.equals(s.getStatus()) || STATUS_TRAFFICKED.equals(s.getStatus()));
    }
}
