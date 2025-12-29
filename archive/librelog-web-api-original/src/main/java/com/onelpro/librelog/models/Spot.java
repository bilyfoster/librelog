package com.onelpro.librelog.models;

import com.onelpro.librelog.enums.BreakPosition;
import com.onelpro.librelog.enums.SpotDaypart;
import com.onelpro.librelog.enums.SpotStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entity representing a spot in the system.
 */
@Entity
@Table(name = "spots", indexes = {
        @Index(name = "ix_spots_id", columnList = "id"),
        @Index(name = "ix_spots_order_id", columnList = "order_id"),
        @Index(name = "ix_spots_campaign_id", columnList = "campaign_id"),
        @Index(name = "ix_spots_scheduled_date", columnList = "scheduled_date"),
        @Index(name = "ix_spots_daypart", columnList = "daypart"),
        @Index(name = "ix_spots_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Spot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "scheduled_time", nullable = false, length = 8)
    private String scheduledTime;

    @Column(name = "spot_length", nullable = false)
    private Integer spotLength;

    @Enumerated(EnumType.STRING)
    @Column(name = "break_position", columnDefinition = "breakposition")
    private BreakPosition breakPosition;

    @Enumerated(EnumType.STRING)
    @Column(name = "daypart", columnDefinition = "spotdaypart")
    private SpotDaypart daypart;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "spotstatus")
    @Builder.Default
    private SpotStatus status = SpotStatus.SCHEDULED;

    @Column(name = "actual_air_time")
    private Instant actualAirTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "makegood_of_id")
    private Spot makegoodOf;

    @Column(name = "conflict_resolved")
    @Builder.Default
    private Boolean conflictResolved = false;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}

