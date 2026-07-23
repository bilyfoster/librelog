package com.onelpro.librelog.schedule;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Which media package airs in a given show instance on a given day (explicit, per-day). */
@Entity
@Table(name = "feature_assignment")
@IdClass(FeatureAssignment.Key.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureAssignment {

    @Id
    @Column(name = "schedule_day_id")
    private UUID scheduleDayId;

    @Id
    @Column(name = "show_instance_id")
    private Long showInstanceId;

    @Column(name = "package_id", nullable = false)
    private UUID packageId;

    public static class Key implements Serializable {
        private UUID scheduleDayId;
        private Long showInstanceId;

        public Key() {}

        public Key(UUID scheduleDayId, Long showInstanceId) {
            this.scheduleDayId = scheduleDayId;
            this.showInstanceId = showInstanceId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key k)) return false;
            return Objects.equals(scheduleDayId, k.scheduleDayId)
                    && Objects.equals(showInstanceId, k.showInstanceId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(scheduleDayId, showInstanceId);
        }
    }
}
