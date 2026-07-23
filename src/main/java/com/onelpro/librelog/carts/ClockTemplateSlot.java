package com.onelpro.librelog.carts;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "clock_template_slot")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClockTemplateSlot {

    @Id
    private UUID id;

    @Column(name = "clock_template_id", nullable = false)
    private UUID clockTemplateId;

    @Column(nullable = false)
    private int position;

    /** MUSIC_CART | COMMERCIAL_CART | TRACK | SPOT | VOICETRACK | NOTE */
    @Column(nullable = false)
    private String kind;

    @Column(name = "cart_id")
    private UUID cartId;

    /**
     * When set (and {@link #cartId} is null), push resolves any cart of this station
     * {@link #kind} (MUSIC vs COMMERCIAL) with this editorial category — first cart
     * (by name) whose rotation yields an eligible member at the slot time wins.
     */
    @Column(name = "cart_category")
    private String cartCategory;

    @Column(name = "librtime_file_id")
    private Long librtimeFileId;

    @Column(name = "spot_id")
    private UUID spotId;

    private String label;

    @Column(name = "default_length_seconds")
    private Integer defaultLengthSeconds;

    /**
     * Fill block mode for cart slots: {@code COUNT} (fixed number of units), {@code TIME}
     * (fill roughly this many seconds, unit-estimated at apply time), or {@code TO_END}
     * (music only — keep resolving at push until the show instance ends). Null = one unit.
     */
    @Column(name = "fill_mode")
    private String fillMode;

    @Column(name = "fill_target_seconds")
    private Integer fillTargetSeconds;

    @Column(name = "fill_target_count")
    private Integer fillTargetCount;

    /**
     * Hot-clock anchor: this slot should start this many seconds after the show
     * instance starts (legal ID at 0, break A at 1080 = 18:00). Null = floats.
     */
    @Column(name = "anchor_offset_seconds")
    private Integer anchorOffsetSeconds;

    /** SOFT = start late and flag; HARD = trim preceding music/pad to land exactly. */
    @Column(name = "anchor_policy")
    private String anchorPolicy;

    /** For kind=FEATURE: which part (1-based) of the day's assigned package airs here. */
    @Column(name = "feature_sequence")
    private Integer featureSequence;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
    }
}
