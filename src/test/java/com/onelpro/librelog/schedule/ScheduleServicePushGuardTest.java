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

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Playout safety rail: push must be rejected for the current (or any past) day so the
 * active playout buffers are never rewritten, and allowed for future days.
 */
class ScheduleServicePushGuardTest {

    private ScheduleDayRepository days;
    private ScheduleService service;

    @BeforeEach
    void setUp() {
        days = mock(ScheduleDayRepository.class);
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
                mock(ScheduleDayClockSegmentRepository.class),
                mock(ClockGridRowRepository.class),
                mock(SpotRepository.class),
                new JazzHandoffService(),
                mock(FeatureAssignmentRepository.class),
                mock(com.onelpro.librelog.media.MediaPackageRepository.class),
                mock(com.onelpro.librelog.media.MediaPackagePartRepository.class));
    }

    private UUID stubDay(LocalDate date) {
        UUID dayId = UUID.randomUUID();
        ScheduleDay day = ScheduleDay.builder()
                .id(dayId)
                .stationId(UUID.randomUUID())
                .date(date)
                .status("DRAFT")
                .build();
        when(days.findById(dayId)).thenReturn(Optional.of(day));
        return dayId;
    }

    @Test
    void pushIsBlockedForToday() {
        UUID dayId = stubDay(LocalDate.now());
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.push(dayId, UUID.randomUUID()));
        assertTrue(ex.getMessage().contains("playout buffers"));
    }

    @Test
    void pushIsBlockedForPastDays() {
        UUID dayId = stubDay(LocalDate.now().minusDays(3));
        assertThrows(IllegalArgumentException.class,
                () -> service.push(dayId, UUID.randomUUID()));
    }

    @Test
    void pushPassesGuardForFutureDays() {
        UUID dayId = stubDay(LocalDate.now().plusDays(1));
        // Guard passes; the next failure must be the missing day lock, not the date guard.
        ScheduleService.ConcurrencyException ex = assertThrows(ScheduleService.ConcurrencyException.class,
                () -> service.push(dayId, UUID.randomUUID()));
        assertTrue(ex.getMessage().contains("Acquire the day lock"));
    }
}
