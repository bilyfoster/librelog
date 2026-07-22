package com.onelpro.librelog.rumble;

import com.onelpro.librelog.librtime.LibreTimeClient;
import com.onelpro.librelog.librtime.LibreTimeService;
import com.onelpro.librelog.rumble.service.AudioTranscodePipeline;
import com.onelpro.librelog.rumble.service.JazzSftpService;
import com.onelpro.librelog.schedule.ScheduleDay;
import com.onelpro.librelog.schedule.ScheduleDayRepository;
import com.onelpro.librelog.schedule.ScheduleItem;
import com.onelpro.librelog.schedule.ScheduleItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class VoiceTrackControllerTest {

    private ScheduleItemRepository items;
    private ScheduleDayRepository days;
    private AudioTranscodePipeline pipeline;
    private JazzSftpService jazz;
    private LibreTimeService libretime;
    private Long libraryMatch;
    private VoiceTrackController controller;

    @BeforeEach
    void setUp() {
        items = mock(ScheduleItemRepository.class);
        days = mock(ScheduleDayRepository.class);
        pipeline = mock(AudioTranscodePipeline.class);
        jazz = mock(JazzSftpService.class);
        libretime = mock(LibreTimeService.class);
        libraryMatch = null;
        controller = new VoiceTrackController(items, days, pipeline, jazz, libretime) {
            @Override
            protected Long findImportedFileId(LibreTimeClient client, String title) {
                return libraryMatch;
            }
        };
        when(items.save(any(ScheduleItem.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private MultipartFile takeFile() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        doAnswer(inv -> {
            Files.write(((File) inv.getArgument(0)).toPath(), new byte[]{1, 2, 3});
            return null;
        }).when(file).transferTo(any(File.class));
        return file;
    }

    private ScheduleItem stubVoiceTrackItem() {
        ScheduleItem item = ScheduleItem.builder()
                .id(UUID.randomUUID())
                .scheduleDayId(UUID.randomUUID())
                .slotIndex(3)
                .kind("VOICETRACK")
                .scheduledAt(Instant.parse("2026-07-22T14:30:00Z"))
                .lengthSeconds(30)
                .position(3)
                .build();
        ScheduleDay day = ScheduleDay.builder()
                .id(item.getScheduleDayId())
                .stationId(UUID.randomUUID())
                .date(LocalDate.of(2026, 7, 22))
                .status("DRAFT")
                .build();
        when(items.findById(item.getId())).thenReturn(Optional.of(item));
        when(days.findById(day.getId())).thenReturn(Optional.of(day));
        return item;
    }

    @Test
    void emptyFileIsRejected() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);
        ResponseEntity<?> r = controller.record(file, UUID.randomUUID(), "DJ Dan", null, null, null);
        assertEquals(400, r.getStatusCode().value());
    }

    @Test
    void missingScheduleItemIdIsRejected() throws Exception {
        ResponseEntity<?> r = controller.record(takeFile(), null, "DJ Dan", null, null, null);
        assertEquals(400, r.getStatusCode().value());
    }

    @Test
    void unknownItemIsRejected() throws Exception {
        UUID id = UUID.randomUUID();
        when(items.findById(id)).thenReturn(Optional.empty());
        ResponseEntity<?> r = controller.record(takeFile(), id, "DJ Dan", null, null, null);
        assertEquals(400, r.getStatusCode().value());
    }

    @Test
    void nonVoiceTrackKindIsRejected() throws Exception {
        ScheduleItem item = stubVoiceTrackItem();
        item.setKind("TRACK");
        ResponseEntity<?> r = controller.record(takeFile(), item.getId(), "DJ Dan", null, null, null);
        assertEquals(400, r.getStatusCode().value());
    }

    @Test
    void missingHostIsRejected() throws Exception {
        ScheduleItem item = stubVoiceTrackItem();
        ResponseEntity<?> r = controller.record(takeFile(), item.getId(), "  ", null, null, null);
        assertEquals(400, r.getStatusCode().value());
    }

    @Test
    void jazzUnconfiguredReturns501() throws Exception {
        ScheduleItem item = stubVoiceTrackItem();
        when(jazz.isConfigured()).thenReturn(false);
        ResponseEntity<?> r = controller.record(takeFile(), item.getId(), "DJ Dan", null, null, null);
        assertEquals(501, r.getStatusCode().value());
        assertTrue(((Map<?, ?>) r.getBody()).get("error").toString().contains("Jazz SFTP"));
        verifyNoInteractions(pipeline);
    }

    @Test
    void happyPathUsesAud04TitleAndUpdatesItem() throws Exception {
        ScheduleItem item = stubVoiceTrackItem();
        when(jazz.isConfigured()).thenReturn(true);
        File processed = File.createTempFile("vt-test-", ".mp3");
        processed.deleteOnExit();
        when(pipeline.normalizeAndFormatAudio(any(File.class), eq("DJ Dan"), eq("VT-DJ-Dan-20260722-H14")))
                .thenReturn(processed);
        when(pipeline.probeDurationSeconds(processed)).thenReturn(42);
        libraryMatch = 777L;

        ResponseEntity<?> r = controller.record(takeFile(), item.getId(), "DJ Dan", 5, new BigDecimal("-6.0"), null);

        assertEquals(200, r.getStatusCode().value());
        ScheduleItem saved = (ScheduleItem) r.getBody();
        assertEquals(42, saved.getLengthSeconds());
        assertEquals(777L, saved.getLibrtimeFileId());
        assertEquals(5, saved.getSegueOffsetSeconds());
        assertEquals(new BigDecimal("-6.0"), saved.getDuckDb());
        assertEquals("VT-DJ-Dan-20260722-H14", saved.getLabel());
        verify(jazz).uploadAndImport(processed, "VT-DJ-Dan-20260722-H14.mp3");
    }

    @Test
    void missingLibraryMatchReturns502ButKeepsDuration() throws Exception {
        ScheduleItem item = stubVoiceTrackItem();
        when(jazz.isConfigured()).thenReturn(true);
        File processed = File.createTempFile("vt-test-", ".mp3");
        processed.deleteOnExit();
        when(pipeline.normalizeAndFormatAudio(any(File.class), anyString(), anyString()))
                .thenReturn(processed);
        when(pipeline.probeDurationSeconds(processed)).thenReturn(17);
        libraryMatch = null;

        ResponseEntity<?> r = controller.record(takeFile(), item.getId(), "DJ Dan", null, null, null);

        assertEquals(502, r.getStatusCode().value());
        assertTrue(((Map<?, ?>) r.getBody()).get("error").toString().contains("not found in the LibreTime library"));
        assertEquals(17, item.getLengthSeconds());
        assertNull(item.getLibrtimeFileId());
        verify(items).save(item);
    }
}
