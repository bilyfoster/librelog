package com.onelpro.librelog.rumble;

import com.onelpro.librelog.auth.AppUser;
import com.onelpro.librelog.rumble.service.AudioTranscodePipeline;
import com.onelpro.librelog.rumble.service.JazzSftpService;
import com.onelpro.librelog.station.StationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class MediaUploadController {

    private final MediaUploadRepository uploads;
    private final StationRepository stations;
    private final AudioTranscodePipeline transcodePipeline;
    private final JazzSftpService jazzSftp;

    @PostMapping("/api/media/uploads")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file,
                                    @RequestParam(value = "artist", required = false) String artist,
                                    @RequestParam(value = "title", required = false) String title,
                                    @RequestParam("stationId") UUID stationId,
                                    @AuthenticationPrincipal AppUser user) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "file is required"));
        }
        if (stationId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "stationId is required"));
        }
        if (!stations.existsById(stationId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Unknown station"));
        }

        MediaUpload row = MediaUpload.builder()
                .stationId(stationId)
                .originalFileName(file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename())
                .artistTag(artist)
                .titleTag(title)
                .status("UPLOADED")
                .build();

        File tempRaw = null;
        try {
            tempRaw = File.createTempFile("librelog-upload-", ".raw");
            file.transferTo(tempRaw);

            File processed = transcodePipeline.normalizeAndFormatAudio(tempRaw, artist, title);
            row.setFinalFileName(processed.getName());
            row.setDurationSeconds(transcodePipeline.probeDurationSeconds(processed));
            row = uploads.save(row);

            if (!jazzSftp.isConfigured()) {
                row.setError("Jazz SFTP not configured; file kept on the LibreLog server only");
                row = uploads.save(row);
            } else {
                try {
                    jazzSftp.uploadAndImport(processed, processed.getName());
                    row.setStatus("IMPORTED");
                    row.setError(null);
                } catch (Exception e) {
                    row.setStatus("FAILED");
                    row.setError(e.getMessage());
                }
                row = uploads.save(row);
            }
            return ResponseEntity.ok(row);
        } catch (Exception e) {
            row.setStatus("FAILED");
            row.setError(e.getMessage());
            row = uploads.save(row);
            return ResponseEntity.ok(row);
        } finally {
            if (tempRaw != null) tempRaw.delete();
        }
    }

    @GetMapping("/api/media/uploads")
    public List<MediaUpload> list(@RequestParam("stationId") UUID stationId,
                                  @AuthenticationPrincipal AppUser user) {
        return uploads.findByStationIdOrderByCreatedAtDesc(stationId);
    }
}
