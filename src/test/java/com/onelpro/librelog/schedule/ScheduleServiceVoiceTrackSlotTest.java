package com.onelpro.librelog.schedule;

import com.onelpro.librelog.auth.AppUserRepository;
import com.onelpro.librelog.carts.CartRepository;
import com.onelpro.librelog.carts.CartService;
import com.onelpro.librelog.carts.ClockService;
import com.onelpro.librelog.carts.ClockTemplateRepository;
import com.onelpro.librelog.carts.ClockTemplateSlot;
import com.onelpro.librelog.librtime.LibreTimeService;
import com.onelpro.librelog.orders.SpotRepository;
import com.onelpro.librelog.rumble.service.JazzHandoffService;
import com.onelpro.librelog.station.StationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Clock slots of kind VOICETRACK materialize into empty voice-track schedule items
 * (no LibreTime file, default 30s) via {@code applyClock}.
 */
class ScheduleServiceVoiceTrackSlotTest {

    private ScheduleDayRepository days;
    private ScheduleItemRepository items;
    private DayLockRepository locks;
    private ClockService clockService;
    private ScheduleDayClockSegmentRepository clockSegments;
    private ScheduleService service;

    @BeforeEach
    void setUp() {
        days = mock(ScheduleDayRepository.class);
        items = mock(ScheduleItemRepository.class);
        locks = mock(DayLockRepository.class);
        clockService = mock(ClockService.class);
        clockSegments = mock(ScheduleDayClockSegmentRepository.class);
        service = new ScheduleService(
                days,
                items,
                locks,
                mock(AppUserRepository.class),
                mock(LibreTimeService.class),
                mock(StationRepository.class),
                mock(CartService.class),
                mock(CartRepository.class),
                clockService,
                mock(ClockTemplateRepository.class),
                clockSegments,
                mock(ClockGridRowRepository.class),
                mock(SpotRepository.class),
                new JazzHandoffService(),
                mock(FeatureAssignmentRepository.class),
                mock(com.onelpro.librelog.media.MediaPackageRepository.class),
                mock(com.onelpro.librelog.media.MediaPackagePartRepository.class));
    }

    @Test
    void voiceTrackClockSlotBecomesEmptyVoiceTrackItem() {
        UUID dayId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID stationId = UUID.randomUUID();
        LocalDate date = LocalDate.now().plusDays(2);
        ScheduleDay day = ScheduleDay.builder()
                .id(dayId).stationId(stationId).date(date).status("DRAFT").build();
        when(days.findById(dayId)).thenReturn(Optional.of(day));
        when(days.findByStationIdAndDate(stationId, date)).thenReturn(Optional.of(day));
        when(days.save(any(ScheduleDay.class))).thenAnswer(inv -> inv.getArgument(0));
        when(locks.findById(dayId)).thenReturn(Optional.of(DayLock.builder()
                .scheduleDayId(dayId).userId(userId)
                .acquiredAt(Instant.now()).expiresAt(Instant.now().plusSeconds(600))
                .build()));
        when(locks.save(any(DayLock.class))).thenAnswer(inv -> inv.getArgument(0));
        when(items.findByScheduleDayIdOrderByPositionAsc(dayId)).thenReturn(List.of());
        when(items.save(any(ScheduleItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(clockSegments.findByScheduleDayIdOrderByPositionAsc(dayId)).thenReturn(List.of());

        UUID clockId = UUID.randomUUID();
        when(clockService.slotsOf(clockId)).thenReturn(List.of(
                ClockTemplateSlot.builder().kind("VOICETRACK").build()));

        service.applyClock(dayId, userId, 42L, clockId);

        ArgumentCaptor<ScheduleItem> captor = ArgumentCaptor.forClass(ScheduleItem.class);
        verify(items, atLeastOnce()).save(captor.capture());
        ScheduleItem vt = captor.getAllValues().stream()
                .filter(i -> "VOICETRACK".equals(i.getKind()))
                .findFirst().orElseThrow(() -> new AssertionError("no VOICETRACK item saved"));
        assertNull(vt.getLibrtimeFileId());
        assertEquals("Voice track", vt.getLabel());
        assertEquals(30, vt.getLengthSeconds());
        assertEquals(42L, vt.getShowInstanceId());
    }
}
