package com.onelpro.librelog.schedule;

import com.onelpro.librelog.auth.AppUserRepository;
import com.onelpro.librelog.carts.CartRepository;
import com.onelpro.librelog.carts.CartService;
import com.onelpro.librelog.carts.ClockService;
import com.onelpro.librelog.carts.ClockTemplateRepository;
import com.onelpro.librelog.librtime.LibreTimeService;
import com.onelpro.librelog.orders.SpotRepository;
import com.onelpro.librelog.rumble.service.JazzHandoffService;
import com.onelpro.librelog.station.StationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * A brand-new schedule day must seed its per-day clock schedule from the station's
 * weekly grid (matching weekday only); an existing day must never be re-seeded.
 */
class ScheduleServiceGridSeedTest {

    private ScheduleDayRepository days;
    private ClockGridRowRepository gridRows;
    private ScheduleDayClockSegmentRepository clockSegments;
    private ScheduleService service;

    private final UUID stationId = UUID.randomUUID();
    // 2026-07-01 is a Wednesday (ISO day-of-week 3).
    private final LocalDate wednesday = LocalDate.of(2026, 7, 1);

    @BeforeEach
    void setUp() {
        days = mock(ScheduleDayRepository.class);
        gridRows = mock(ClockGridRowRepository.class);
        clockSegments = mock(ScheduleDayClockSegmentRepository.class);
        service = new ScheduleService(
                days,
                mock(ScheduleItemRepository.class),
                mock(DayLockRepository.class),
                mock(AppUserRepository.class),
                mock(LibreTimeService.class),
                mock(StationRepository.class),
                mock(CartService.class),
                mock(CartRepository.class),
                mock(ClockService.class),
                mock(ClockTemplateRepository.class),
                clockSegments,
                gridRows,
                mock(SpotRepository.class),
                new JazzHandoffService());
        when(days.save(any())).thenAnswer(inv -> {
            ScheduleDay d = inv.getArgument(0);
            if (d.getId() == null) d.setId(UUID.randomUUID());
            return d;
        });
    }

    @Test
    void newDaySeedsFromGridForItsWeekday() {
        UUID clockA = UUID.randomUUID();
        UUID clockB = UUID.randomUUID();
        when(days.findByStationIdAndDate(stationId, wednesday)).thenReturn(java.util.Optional.empty());
        when(gridRows.findByStationIdAndDayOfWeekOrderByPositionAsc(stationId, 3)).thenReturn(List.of(
                ClockGridRow.builder().id(UUID.randomUUID()).stationId(stationId).dayOfWeek(3)
                        .position(0).localStartMinutes(360).localEndMinutes(720).clockTemplateId(clockA).build(),
                ClockGridRow.builder().id(UUID.randomUUID()).stationId(stationId).dayOfWeek(3)
                        .position(1).localStartMinutes(720).localEndMinutes(1440).clockTemplateId(clockB).build()));

        service.load(stationId, wednesday, UUID.randomUUID());

        ArgumentCaptor<ScheduleDayClockSegment> cap = ArgumentCaptor.forClass(ScheduleDayClockSegment.class);
        verify(clockSegments, times(2)).save(cap.capture());
        assertThat(cap.getAllValues()).extracting(ScheduleDayClockSegment::getClockTemplateId)
                .containsExactly(clockA, clockB);
        assertThat(cap.getAllValues().get(0).getLocalStartMinutes()).isEqualTo(360);
        assertThat(cap.getAllValues().get(1).getLocalEndMinutes()).isEqualTo(1440);
    }

    @Test
    void existingDayIsNeverReseeded() {
        ScheduleDay existing = ScheduleDay.builder().id(UUID.randomUUID())
                .stationId(stationId).date(wednesday).status("DRAFT").build();
        when(days.findByStationIdAndDate(stationId, wednesday)).thenReturn(java.util.Optional.of(existing));

        service.load(stationId, wednesday, UUID.randomUUID());

        verify(clockSegments, never()).save(any());
        verifyNoInteractions(gridRows);
    }
}
