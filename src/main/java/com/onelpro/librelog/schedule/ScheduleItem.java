package com.onelpro.librelog.schedule;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "schedule_item")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleItem {

    @Id
    private UUID id;

    @Column(name = "schedule_day_id", nullable = false)
    private UUID scheduleDayId;

    @Column(name = "show_instance_id")
    private Long showInstanceId;

    @Column(name = "slot_index", nullable = false)
    private int slotIndex;

    @Column(nullable = false)
    private String kind;

    @Column(name = "spot_id")
    private UUID spotId;

    @Column(name = "librtime_file_id")
    private Long librtimeFileId;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "length_seconds")
    private Integer lengthSeconds;

    @Column(nullable = false)
    private int position;

    @Column(name = "cart_id")
    private UUID cartId;

    @Column(name = "cart_category")
    private String cartCategory;

    @Column(name = "resolved_member_id")
    private UUID resolvedMemberId;

    /** Overlap/crossfade marker (PRD §6.3): seconds the next element may start early. */
    @Column(name = "segue_offset_seconds")
    private Integer segueOffsetSeconds;

    /** Duck marker (PRD §6.3): gain reduction applied under this voice track. */
    @Column(name = "duck_db")
    private java.math.BigDecimal duckDb;

    /**
     * Push-time fill marker. Only {@code TO_END} appears on schedule items (music fill to
     * the end of the show instance, resolved unit-by-unit at push). COUNT/TIME fills are
     * expanded into plain items when the clock is applied.
     */
    @Column(name = "fill_mode")
    private String fillMode;

    @Column(name = "fill_target_seconds")
    private Integer fillTargetSeconds;

    @Column(name = "fill_target_count")
    private Integer fillTargetCount;

    /** Hot-clock anchor: start offset (seconds) from the instance start. Null = floats. */
    @Column(name = "anchor_offset_seconds")
    private Integer anchorOffsetSeconds;

    /** SOFT = start late and flag; HARD = trim preceding music/pad to land exactly. */
    @Column(name = "anchor_policy")
    private String anchorPolicy;

    /**
     * Units expanded from the same avail/fill block share a group id; the group's
     * fill_target_seconds acts as a shared total-seconds cap at push time.
     */
    @Column(name = "fill_group")
    private UUID fillGroup;

    /** For kind=FEATURE: which part (1-based) of the day's assigned package airs here. */
    @Column(name = "feature_sequence")
    private Integer featureSequence;

    private String label;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
    }
}
