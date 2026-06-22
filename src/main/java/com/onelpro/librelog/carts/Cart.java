package com.onelpro.librelog.carts;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cart")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cart {

    /** Pick the next member by round-robin rotation pointer (default). */
    public static final String STRATEGY_ROTATION = "ROTATION";
    /** Pick the freshest member first — for news, voicetracks, anything time-sensitive. */
    public static final String STRATEGY_NEWEST_FIRST = "NEWEST_FIRST";

    @Id
    private UUID id;

    @Column(name = "station_id", nullable = false)
    private UUID stationId;

    @Column(nullable = false)
    private String name;

    /** MUSIC | COMMERCIAL — selects the underlying source (library file vs spot). */
    @Column(nullable = false)
    private String kind;

    /**
     * Editorial category (MUSIC, IMAGING, CONTENT, NEWS, WEATHER, PROMO, VOICETRACK,
     * SPONSORED_FEATURE, COMMERCIAL). Independent of {@code kind} so multiple categories
     * can share the same source — e.g. IMAGING and CONTENT carts are both library-backed.
     */
    @Column(nullable = false)
    private String category;

    /** MANUAL | ORDER */
    @Column(nullable = false)
    private String source;

    @Column(name = "order_id")
    private UUID orderId;

    private String description;

    @Column(name = "rotation_pointer", nullable = false)
    private int rotationPointer;

    /** {@link #STRATEGY_ROTATION} | {@link #STRATEGY_NEWEST_FIRST}. */
    @Builder.Default
    @Column(name = "selection_strategy", nullable = false)
    private String selectionStrategy = STRATEGY_ROTATION;

    /**
     * When set, members whose audio is older than this many hours are excluded at resolve
     * time (e.g. 6h for news). Null = no freshness window.
     */
    @Column(name = "max_age_hours")
    private Integer maxAgeHours;

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
        if (source == null) source = "MANUAL";
        if (category == null) category = "COMMERCIAL".equals(kind) ? "COMMERCIAL" : "MUSIC";
        if (selectionStrategy == null) selectionStrategy = STRATEGY_ROTATION;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
