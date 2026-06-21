package com.onelpro.librelog.carts;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cart_play_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartPlayHistory {

    @Id
    private UUID id;

    @Column(name = "station_id", nullable = false)
    private UUID stationId;

    @Column(name = "cart_id")
    private UUID cartId;

    @Column(name = "cart_member_id")
    private UUID cartMemberId;

    private String artist;
    private String title;
    private String sponsor;
    private String product;

    @Column(name = "librtime_file_id")
    private Long librtimeFileId;

    @Column(name = "spot_id")
    private UUID spotId;

    @Column(name = "played_at", nullable = false)
    private Instant playedAt;

    @Column(name = "schedule_item_id")
    private UUID scheduleItemId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }
}
