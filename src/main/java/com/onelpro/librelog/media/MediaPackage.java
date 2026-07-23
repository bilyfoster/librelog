package com.onelpro.librelog.media;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * A multi-part media package — a long-form interview/feature aired in segments around
 * breaks. Parts are windows: either separate files (externally edited) or cue windows
 * into one shared file (break points, no slicing). Only READY packages can be assigned
 * in Day Builder; a package flips to AIRED when pushed.
 */
@Entity
@Table(name = "media_package")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaPackage {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_READY = "READY";
    public static final String STATUS_AIRED = "AIRED";

    @Id
    private UUID id;

    @Column(name = "station_id", nullable = false)
    private UUID stationId;

    @Column(nullable = false)
    private String name;

    /** Optional series grouping, e.g. "Mayor Interviews". */
    private String series;

    @Builder.Default
    @Column(nullable = false)
    private String status = STATUS_DRAFT;

    @Column(columnDefinition = "TEXT")
    private String notes;

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
        if (status == null) status = STATUS_DRAFT;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
