package com.onelpro.librelog.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing copy (script/audio) in the system.
 */
@Entity
@Table(name = "copy", indexes = {
        @Index(name = "ix_copy_id", columnList = "id"),
        @Index(name = "ix_copy_order_id", columnList = "order_id"),
        @Index(name = "ix_copy_advertiser_id", columnList = "advertiser_id"),
        @Index(name = "ix_copy_title", columnList = "title"),
        @Index(name = "ix_copy_expires_at", columnList = "expires_at"),
        @Index(name = "ix_copy_active", columnList = "active")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Copy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "advertiser_id")
    private Advertiser advertiser;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "script_text", columnDefinition = "TEXT")
    private String scriptText;

    @Column(name = "audio_file_path", columnDefinition = "TEXT")
    private String audioFilePath;

    @Column(name = "audio_file_url", columnDefinition = "TEXT")
    private String audioFileUrl;

    @Column(name = "version")
    @Builder.Default
    private Integer version = 1;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "active")
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}

