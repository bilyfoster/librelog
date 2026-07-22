package com.onelpro.librelog.rumble;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "media_upload")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaUpload {

    @Id
    private UUID id;

    @Column(name = "station_id", nullable = false)
    private UUID stationId;

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Column(name = "final_file_name")
    private String finalFileName;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "artist_tag")
    private String artistTag;

    @Column(name = "title_tag")
    private String titleTag;

    @Column(nullable = false)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String error;

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
        if (status == null) status = "UPLOADED";
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
