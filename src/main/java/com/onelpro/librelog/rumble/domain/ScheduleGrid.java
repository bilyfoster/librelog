package com.onelpro.librelog.rumble.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "rumble_schedule_grid")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleGrid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "hour_of_day", nullable = false)
    private int hourOfDay;

    @Column(name = "clock_id", nullable = false)
    private Long clockId;
}
