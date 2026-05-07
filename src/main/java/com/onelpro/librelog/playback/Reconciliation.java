package com.onelpro.librelog.playback;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reconciliation")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reconciliation {

    @Id
    private UUID id;

    @Column(name = "schedule_item_id", nullable = false)
    private UUID scheduleItemId;

    @Column(name = "playback_log_entry_id")
    private UUID playbackLogEntryId;

    @Column(name = "matched_at")
    private Instant matchedAt;

    @Column(nullable = false)
    private String status;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
    }
}
