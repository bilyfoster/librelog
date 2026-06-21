package com.onelpro.librelog.station;

import com.onelpro.librelog.time.TimeWindowUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class DayPartController {

    private final DayPartRepository dayParts;
    private final StationRepository stations;

    public record DayPartDto(String id, String stationId, String name,
                             int startMinutes, int endMinutes, int sortOrder) {
        static DayPartDto from(DayPart d) {
            return new DayPartDto(d.getId().toString(), d.getStationId().toString(),
                    d.getName(), d.getStartMinutes(), d.getEndMinutes(), d.getSortOrder());
        }
    }

    public record DayPartRequest(@NotBlank String name,
                                 @NotNull Integer startMinutes,
                                 @NotNull Integer endMinutes,
                                 Integer sortOrder) {}

    @GetMapping("/api/stations/{stationId}/day-parts")
    public ResponseEntity<List<DayPartDto>> list(@PathVariable UUID stationId) {
        if (!stations.existsById(stationId)) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(dayParts.findByStationIdOrderBySortOrderAscNameAsc(stationId).stream()
                .map(DayPartDto::from).toList());
    }

    @PostMapping("/api/stations/{stationId}/day-parts")
    public ResponseEntity<?> create(@PathVariable UUID stationId, @Valid @RequestBody DayPartRequest req) {
        if (!stations.existsById(stationId)) return ResponseEntity.notFound().build();
        try {
            validateWindow(req.startMinutes(), req.endMinutes());
            dayParts.findByStationIdAndNameIgnoreCase(stationId, req.name().trim()).ifPresent(d -> {
                throw new IllegalArgumentException("A day part with this name already exists for this station");
            });
            int end = TimeWindowUtil.normalizeExclusiveEnd(req.endMinutes());
            DayPart d = DayPart.builder()
                    .stationId(stationId)
                    .name(req.name().trim())
                    .startMinutes(req.startMinutes())
                    .endMinutes(end)
                    .sortOrder(req.sortOrder() != null ? req.sortOrder() : 0)
                    .build();
            return ResponseEntity.ok(DayPartDto.from(dayParts.save(d)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/api/stations/{stationId}/day-parts/{dayPartId}")
    public ResponseEntity<?> update(@PathVariable UUID stationId, @PathVariable UUID dayPartId,
                                    @Valid @RequestBody DayPartRequest req) {
        DayPart d = dayParts.findById(dayPartId).orElse(null);
        if (d == null || !d.getStationId().equals(stationId)) {
            return ResponseEntity.notFound().build();
        }
        try {
            validateWindow(req.startMinutes(), req.endMinutes());
            String trimmed = req.name().trim();
            dayParts.findByStationIdAndNameIgnoreCase(stationId, trimmed).ifPresent(other -> {
                if (!other.getId().equals(dayPartId)) {
                    throw new IllegalArgumentException("A day part with this name already exists for this station");
                }
            });
            d.setName(trimmed);
            d.setStartMinutes(req.startMinutes());
            d.setEndMinutes(TimeWindowUtil.normalizeExclusiveEnd(req.endMinutes()));
            if (req.sortOrder() != null) d.setSortOrder(req.sortOrder());
            return ResponseEntity.ok(DayPartDto.from(dayParts.save(d)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/api/stations/{stationId}/day-parts/{dayPartId}")
    public ResponseEntity<Void> delete(@PathVariable UUID stationId, @PathVariable UUID dayPartId) {
        DayPart d = dayParts.findById(dayPartId).orElse(null);
        if (d == null || !d.getStationId().equals(stationId)) {
            return ResponseEntity.notFound().build();
        }
        dayParts.deleteById(dayPartId);
        return ResponseEntity.noContent().build();
    }

    private static void validateWindow(int start, int end) {
        TimeWindowUtil.validateWindowPair(start, end, "Day part");
    }
}
