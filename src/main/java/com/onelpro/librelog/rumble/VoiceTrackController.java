package com.onelpro.librelog.rumble;

import com.fasterxml.jackson.databind.JsonNode;
import com.onelpro.librelog.auth.AppUser;
import com.onelpro.librelog.librtime.LibreTimeClient;
import com.onelpro.librelog.librtime.LibreTimeService;
import com.onelpro.librelog.rumble.service.AudioTranscodePipeline;
import com.onelpro.librelog.rumble.service.JazzSftpService;
import com.onelpro.librelog.schedule.ScheduleDay;
import com.onelpro.librelog.schedule.ScheduleDayRepository;
import com.onelpro.librelog.schedule.ScheduleItem;
import com.onelpro.librelog.schedule.ScheduleItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

/**
 * Phase 4: browser-recorded voice tracks. A take is normalized/tagged, shipped to Jazz
 * over SFTP, located in the LibreTime library, and linked to its VOICETRACK schedule
 * item so the next push treats it like a resolved track.
 */
@RestController
@RequiredArgsConstructor
public class VoiceTrackController {

    private final ScheduleItemRepository scheduleItems;
    private final ScheduleDayRepository scheduleDays;
    private final AudioTranscodePipeline transcodePipeline;
    private final JazzSftpService jazzSftp;
    private final LibreTimeService libretime;

    @PostMapping("/api/voicetracks")
    public ResponseEntity<?> record(@RequestParam("file") MultipartFile file,
                                    @RequestParam(value = "scheduleItemId", required = false) UUID scheduleItemId,
                                    @RequestParam(value = "host", required = false) String host,
                                    @RequestParam(value = "segueOffsetSeconds", required = false) Integer segueOffsetSeconds,
                                    @RequestParam(value = "duckDb", required = false) BigDecimal duckDb,
                                    @AuthenticationPrincipal AppUser user) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "file is required"));
        }
        if (scheduleItemId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "scheduleItemId is required"));
        }
        ScheduleItem item = scheduleItems.findById(scheduleItemId).orElse(null);
        if (item == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Unknown schedule item"));
        }
        if (!"VOICETRACK".equals(item.getKind())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Schedule item is not a voice track slot"));
        }
        if (host == null || host.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "host is required"));
        }
        ScheduleDay day = scheduleDays.findById(item.getScheduleDayId()).orElse(null);
        if (day == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Schedule day not found for item"));
        }
        if (!jazzSftp.isConfigured()) {
            return ResponseEntity.status(501).body(Map.of("error",
                    "Voice track handoff requires Jazz SFTP to be configured"));
        }

        String title = voiceTrackTitle(host.trim(), day, item);
        File tempRaw = null;
        try {
            tempRaw = File.createTempFile("librelog-vt-", ".raw");
            file.transferTo(tempRaw);

            File processed = transcodePipeline.normalizeAndFormatAudio(tempRaw, host.trim(), title);
            item.setLengthSeconds(transcodePipeline.probeDurationSeconds(processed));
            item.setLabel(title);
            if (segueOffsetSeconds != null) item.setSegueOffsetSeconds(segueOffsetSeconds);
            if (duckDb != null) item.setDuckDb(duckDb);

            jazzSftp.uploadAndImport(processed, title + ".mp3");

            Long fileId = findImportedFileId(libretime.clientFor(day.getStationId()), title);
            if (fileId == null) {
                scheduleItems.save(item);
                return ResponseEntity.status(502).body(Map.of("error",
                        "Voice track uploaded to Jazz but not found in the LibreTime library as \""
                                + title + "\"; transcoded file kept at " + processed.getAbsolutePath()));
            }
            item.setLibrtimeFileId(fileId);
            return ResponseEntity.ok(scheduleItems.save(item));
        } catch (Exception e) {
            return ResponseEntity.status(502).body(Map.of("error",
                    e.getMessage() == null ? "Voice track processing failed" : e.getMessage()));
        } finally {
            if (tempRaw != null) tempRaw.delete();
        }
    }

    /**
     * AUD-04 naming: {@code VT-[Host]-[Date]-[Hour]}. The hour comes from the item's
     * scheduled air time when known, otherwise the slot index stands in ({@code S<n>}).
     * The whole title is sanitized so it doubles as the remote file name.
     */
    static String voiceTrackTitle(String host, ScheduleDay day, ScheduleItem item) {
        String datePart = day.getDate() != null
                ? day.getDate().format(DateTimeFormatter.BASIC_ISO_DATE) : "undated";
        String hourPart = item.getScheduledAt() != null
                ? "H" + String.format("%02d", item.getScheduledAt().atOffset(ZoneOffset.UTC).getHour())
                : "S" + item.getSlotIndex();
        return sanitizeFileName("VT-" + host + "-" + datePart + "-" + hourPart);
    }

    static String sanitizeFileName(String s) {
        return s.replaceAll("[^A-Za-z0-9._-]+", "-");
    }

    /** Seam for tests: locate the freshly-imported file in the Jazz library by title. */
    protected Long findImportedFileId(LibreTimeClient client, String title) {
        for (JsonNode f : client.listFiles(null)) {
            String trackTitle = textField(f, "track_title");
            String name = textField(f, "name");
            if (title.equals(trackTitle) || title.equals(name)) {
                JsonNode id = f.get("id");
                if (id != null && id.isNumber()) return id.asLong();
                if (id != null && id.isTextual()) {
                    try { return Long.parseLong(id.asText()); } catch (NumberFormatException ignored) {}
                }
            }
        }
        return null;
    }

    private static String textField(JsonNode n, String field) {
        return n != null && n.has(field) && !n.get(field).isNull() ? n.get(field).asText() : null;
    }
}
