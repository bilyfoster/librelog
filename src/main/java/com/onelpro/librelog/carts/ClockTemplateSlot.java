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

    /** MUSIC_CART | COMMERCIAL_CART | TRACK | SPOT | NOTE */
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

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
    }
}
