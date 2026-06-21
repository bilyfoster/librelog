package com.onelpro.librelog.carts;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cart_member")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartMember {

    @Id
    private UUID id;

    @Column(name = "cart_id", nullable = false)
    private UUID cartId;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private int weight;

    @Column(name = "librtime_file_id")
    private Long librtimeFileId;

    @Column(name = "spot_id")
    private UUID spotId;

    private String artist;
    private String title;
    private String sponsor;
    private String product;

    @Column(name = "length_seconds")
    private Integer lengthSeconds;

    @Column(nullable = false)
    private boolean enabled;

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
        if (weight <= 0) weight = 1;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
