package com.onelpro.librelog.schedule;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "schedule_item")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleItem {

    @Id
    private UUID id;

    @Column(name = "schedule_day_id", nullable = false)
    private UUID scheduleDayId;

    @Column(name = "show_instance_id")
    private Long showInstanceId;

    @Column(name = "slot_index", nullable = false)
    private int slotIndex;

    @Column(nullable = false)
    private String kind;

    @Column(name = "spot_id")
    private UUID spotId;

    @Column(name = "librtime_file_id")
    private Long librtimeFileId;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "length_seconds")
    private Integer lengthSeconds;

    @Column(nullable = false)
    private int position;

    @Column(name = "cart_id")
    private UUID cartId;

    @Column(name = "cart_category")
    private String cartCategory;

    @Column(name = "resolved_member_id")
    private UUID resolvedMemberId;

    private String label;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
    }
}
