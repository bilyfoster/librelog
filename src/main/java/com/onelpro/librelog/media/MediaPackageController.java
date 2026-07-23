package com.onelpro.librelog.media;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Multi-part media packages: create, edit parts (either separate files, or cue windows
 * — break points — into one shared file), and walk DRAFT → READY. AIRED is set by push.
 */
@RestController
@RequiredArgsConstructor
public class MediaPackageController {

    private static final Set<String> MANUAL_STATUSES = Set.of(
            MediaPackage.STATUS_DRAFT, MediaPackage.STATUS_READY);

    private final MediaPackageRepository packages;
    private final MediaPackagePartRepository parts;

    public record PartDto(String id, int sequence, Long librtimeFileId,
                          Integer cueInSeconds, Integer cueOutSeconds,
                          int lengthSeconds, String title) {
        static PartDto from(MediaPackagePart p) {
            return new PartDto(p.getId().toString(), p.getSequence(), p.getLibrtimeFileId(),
                    p.getCueInSeconds(), p.getCueOutSeconds(), p.getLengthSeconds(), p.getTitle());
        }
    }

    public record PackageDto(String id, String stationId, String name, String series,
                             String status, String notes, List<PartDto> parts) {}

    public record PackageRequest(@NotBlank String name, String series, String notes, String status) {}

    public record PartRequest(Long librtimeFileId, Integer cueInSeconds, Integer cueOutSeconds,
                              Integer lengthSeconds, String title) {}

    @GetMapping("/api/stations/{stationId}/packages")
    public List<PackageDto> list(@PathVariable UUID stationId,
                                 @RequestParam(required = false) String status) {
        var list = status == null || status.isBlank()
                ? packages.findByStationIdOrderByCreatedAtDesc(stationId)
                : packages.findByStationIdAndStatusOrderByCreatedAtDesc(stationId, status.trim().toUpperCase());
        return list.stream().map(this::toDto).toList();
    }

    @PostMapping("/api/stations/{stationId}/packages")
    public ResponseEntity<?> create(@PathVariable UUID stationId, @Valid @RequestBody PackageRequest req) {
        MediaPackage p = MediaPackage.builder()
                .stationId(stationId).name(req.name().trim())
                .series(blankToNull(req.series())).notes(req.notes())
                .build();
        return ResponseEntity.ok(toDto(packages.save(p)));
    }

    @PutMapping("/api/packages/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @Valid @RequestBody PackageRequest req) {
        var p = packages.findById(id).orElse(null);
        if (p == null) return ResponseEntity.notFound().build();
        p.setName(req.name().trim());
        p.setSeries(blankToNull(req.series()));
        p.setNotes(req.notes());
        if (req.status() != null && !req.status().isBlank()) {
            String status = req.status().trim().toUpperCase();
            if (!MANUAL_STATUSES.contains(status)) {
                return ResponseEntity.badRequest().body(Map.of("error",
                        "status must be DRAFT or READY (AIRED is set automatically at push)"));
            }
            if (MediaPackage.STATUS_READY.equals(status)
                    && parts.findByPackageIdOrderBySequenceAsc(id).isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error",
                        "Add at least one part before marking the package Ready"));
            }
            p.setStatus(status);
        }
        return ResponseEntity.ok(toDto(packages.save(p)));
    }

    @DeleteMapping("/api/packages/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (!packages.existsById(id)) return ResponseEntity.notFound().build();
        parts.deleteByPackageId(id);
        packages.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Replace all parts. Each part is either a whole file (cues null, lengthSeconds
     * required) or a cue window (both cues set, length derived as cueOut − cueIn).
     */
    @PutMapping("/api/packages/{id}/parts")
    @Transactional
    public ResponseEntity<?> setParts(@PathVariable UUID id, @RequestBody List<PartRequest> req) {
        var p = packages.findById(id).orElse(null);
        if (p == null) return ResponseEntity.notFound().build();
        List<MediaPackagePart> toSave = new ArrayList<>();
        int seq = 1;
        for (PartRequest r : req) {
            if (r.librtimeFileId() == null) {
                return ResponseEntity.badRequest().body(Map.of("error",
                        "Part " + seq + ": librtimeFileId is required"));
            }
            boolean hasIn = r.cueInSeconds() != null;
            boolean hasOut = r.cueOutSeconds() != null;
            if (hasIn != hasOut) {
                return ResponseEntity.badRequest().body(Map.of("error",
                        "Part " + seq + ": set both cueIn and cueOut, or neither"));
            }
            int length;
            if (hasIn) {
                if (r.cueInSeconds() < 0 || r.cueOutSeconds() <= r.cueInSeconds()) {
                    return ResponseEntity.badRequest().body(Map.of("error",
                            "Part " + seq + ": cue window must be 0 <= cueIn < cueOut"));
                }
                length = r.cueOutSeconds() - r.cueInSeconds();
            } else {
                if (r.lengthSeconds() == null || r.lengthSeconds() < 1) {
                    return ResponseEntity.badRequest().body(Map.of("error",
                            "Part " + seq + ": whole-file parts need lengthSeconds"));
                }
                length = r.lengthSeconds();
            }
            toSave.add(MediaPackagePart.builder()
                    .packageId(id).sequence(seq++)
                    .librtimeFileId(r.librtimeFileId())
                    .cueInSeconds(r.cueInSeconds()).cueOutSeconds(r.cueOutSeconds())
                    .lengthSeconds(length)
                    .title(blankToNull(r.title()))
                    .build());
        }
        parts.deleteByPackageId(id);
        parts.flush();
        parts.saveAll(toSave);
        return ResponseEntity.ok(toDto(p));
    }

    private PackageDto toDto(MediaPackage p) {
        return new PackageDto(p.getId().toString(), p.getStationId().toString(),
                p.getName(), p.getSeries(), p.getStatus(), p.getNotes(),
                parts.findByPackageIdOrderBySequenceAsc(p.getId()).stream().map(PartDto::from).toList());
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
