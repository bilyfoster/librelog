package com.onelpro.librelog.schedule;

import com.onelpro.librelog.auth.AppUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService schedule;
    private final ScheduleDayRepository days;

    public record ItemDto(String id, Long showInstanceId, int slotIndex, String kind,
                         String spotId, Long librtimeFileId, Instant scheduledAt,
                         Integer lengthSeconds, int position,
                         String cartId, String cartCategory, String resolvedMemberId, String label,
                         Integer segueOffsetSeconds, java.math.BigDecimal duckDb,
                         String fillMode, Integer fillTargetSeconds, Integer fillTargetCount) {
        static ItemDto from(ScheduleItem i) {
            return new ItemDto(i.getId().toString(), i.getShowInstanceId(), i.getSlotIndex(),
                    i.getKind(), i.getSpotId() == null ? null : i.getSpotId().toString(),
                    i.getLibrtimeFileId(), i.getScheduledAt(), i.getLengthSeconds(), i.getPosition(),
                    i.getCartId() == null ? null : i.getCartId().toString(),
                    i.getCartCategory(),
                    i.getResolvedMemberId() == null ? null : i.getResolvedMemberId().toString(),
                    i.getLabel(), i.getSegueOffsetSeconds(), i.getDuckDb(),
                    i.getFillMode(), i.getFillTargetSeconds(), i.getFillTargetCount());
        }
    }

    public record ClockSegmentDto(String id, int position,
                                  int localStartMinutes, int localEndMinutes,
                                  String clockTemplateId) {
        static ClockSegmentDto from(ScheduleDayClockSegment s) {
            return new ClockSegmentDto(s.getId().toString(), s.getPosition(),
                    s.getLocalStartMinutes(), s.getLocalEndMinutes(),
                    s.getClockTemplateId().toString());
        }
    }

    public record DayDto(String id, String stationId, LocalDate date, String status,
                         Instant pushedAt, String pushedBy, Long version,
                         LockDto lock, List<ItemDto> items,
                         List<ClockSegmentDto> clockSegments,
                         boolean readOnly) {}

    public record ClockSegmentRequest(@NotNull Integer localStartMinutes,
                                      @NotNull Integer localEndMinutes,
                                      @NotBlank String clockTemplateId) {}

    public record ClockScheduleSaveRequest(@Valid List<ClockSegmentRequest> segments) {}

    public record LockDto(String userId, String userName, Instant acquiredAt, Instant expiresAt, boolean self) {}

    public record ItemRequest(Long showInstanceId, int slotIndex, String kind,
                              String spotId, Long librtimeFileId, Instant scheduledAt,
                              Integer lengthSeconds, String cartId, String cartCategory, String label,
                              Integer segueOffsetSeconds, java.math.BigDecimal duckDb,
                              String fillMode, Integer fillTargetSeconds, Integer fillTargetCount) {}

    public record SaveRequest(Long expectedVersion, List<ItemRequest> items) {}

    @GetMapping("/api/stations/{stationId}/days/{date}")
    public DayDto load(@PathVariable UUID stationId, @PathVariable String date,
                       @AuthenticationPrincipal AppUser user) {
        var view = schedule.load(stationId, LocalDate.parse(date), user.getId());
        return toDto(view, user.getId());
    }

    @PostMapping("/api/stations/{stationId}/days/{date}/preload")
    public ResponseEntity<?> preloadFromLibreTime(@PathVariable UUID stationId,
                                                  @PathVariable String date,
                                                  @RequestParam(defaultValue = "false") boolean replace,
                                                  @AuthenticationPrincipal AppUser user) {
        try {
            var view = schedule.preloadFromLibreTime(stationId, LocalDate.parse(date), user.getId(), replace);
            return ResponseEntity.ok(toDto(view, user.getId()));
        } catch (ScheduleService.ConcurrencyException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(502).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/api/days/{dayId}/lock")
    public ResponseEntity<?> acquire(@PathVariable UUID dayId, @AuthenticationPrincipal AppUser user) {
        try {
            var lock = schedule.acquireLock(dayId, user.getId());
            return ResponseEntity.ok(Map.of("acquiredAt", lock.getAcquiredAt(),
                    "expiresAt", lock.getExpiresAt()));
        } catch (ScheduleService.ConcurrencyException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/api/days/{dayId}/lock/extend")
    public ResponseEntity<?> extend(@PathVariable UUID dayId, @AuthenticationPrincipal AppUser user) {
        try {
            var lock = schedule.extendLock(dayId, user.getId());
            return ResponseEntity.ok(Map.of("expiresAt", lock.getExpiresAt()));
        } catch (ScheduleService.ConcurrencyException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/api/days/{dayId}/lock")
    public ResponseEntity<?> release(@PathVariable UUID dayId,
                                      @RequestParam(defaultValue = "false") boolean force,
                                      @AuthenticationPrincipal AppUser user) {
        try {
            boolean isAdmin = "ADMIN".equals(user.getRole());
            schedule.releaseLock(dayId, user.getId(), force && isAdmin);
            return ResponseEntity.noContent().build();
        } catch (ScheduleService.ConcurrencyException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/api/days/{dayId}")
    public ResponseEntity<?> save(@PathVariable UUID dayId,
                                   @Valid @RequestBody SaveRequest req,
                                   @AuthenticationPrincipal AppUser user) {
        try {
            var newItems = req.items().stream().map(r -> ScheduleItem.builder()
                    .showInstanceId(r.showInstanceId())
                    .slotIndex(r.slotIndex())
                    .kind(r.kind())
                    .spotId(r.spotId() == null || r.spotId().isBlank() ? null : UUID.fromString(r.spotId().trim()))
                    .librtimeFileId(r.librtimeFileId())
                    .scheduledAt(r.scheduledAt())
                    .lengthSeconds(r.lengthSeconds())
                    .cartId(r.cartId() == null || r.cartId().isBlank() ? null : UUID.fromString(r.cartId().trim()))
                    .cartCategory(r.cartCategory() == null || r.cartCategory().isBlank() ? null : r.cartCategory().trim())
                    .label(r.label())
                    .segueOffsetSeconds(r.segueOffsetSeconds())
                    .duckDb(r.duckDb())
                    .fillMode(r.fillMode() == null || r.fillMode().isBlank() ? null : r.fillMode().trim().toUpperCase())
                    .fillTargetSeconds(r.fillTargetSeconds())
                    .fillTargetCount(r.fillTargetCount())
                    .build()).toList();
            var view = schedule.save(dayId, user.getId(), req.expectedVersion(), newItems);
            return ResponseEntity.ok(toDto(view, user.getId()));
        } catch (ScheduleService.ConcurrencyException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/api/days/{dayId}/push")
    public ResponseEntity<?> push(@PathVariable UUID dayId, @AuthenticationPrincipal AppUser user) {
        try {
            ScheduleService.PushResult r = schedule.push(dayId, user.getId());
            Map<String, Object> body = new java.util.LinkedHashMap<>();
            body.put("status", "PUSHED");
            body.put("instancesTouched", r.instancesTouched());
            body.put("rowsWritten", r.rowsWritten());
            body.put("rowsResolved", r.rowsResolved());
            body.put("rowsSkipped", r.rowsSkipped());
            body.put("notes", r.notes());
            return ResponseEntity.ok(body);
        } catch (ScheduleService.ConcurrencyException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            // Includes the playout safety-rail rejection (push target too close to air time).
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(502).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/api/days/{dayId}/apply-clock")
    public ResponseEntity<?> applyClock(@PathVariable UUID dayId,
                                        @RequestParam("instance") long showInstanceId,
                                        @RequestParam("clock") UUID clockId,
                                        @AuthenticationPrincipal AppUser user) {
        try {
            var view = schedule.applyClock(dayId, user.getId(), showInstanceId, clockId);
            return ResponseEntity.ok(toDto(view, user.getId()));
        } catch (ScheduleService.ConcurrencyException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    public record ApplyClockScheduleResponse(DayDto day, int instancesUpdated) {}

    /**
     * Materializes each show instance's slots from the day's clock schedule (local hour windows → clocks).
     */
    @PostMapping("/api/days/{dayId}/apply-clock-schedule")
    public ResponseEntity<?> applyClockSchedule(@PathVariable UUID dayId,
                                                @AuthenticationPrincipal AppUser user) {
        try {
            var result = schedule.applyClockScheduleToShows(dayId, user.getId());
            return ResponseEntity.ok(new ApplyClockScheduleResponse(
                    toDto(result.dayView(), user.getId()), result.instancesUpdated()));
        } catch (ScheduleService.ConcurrencyException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/api/days/{dayId}/clock-schedule")
    public ResponseEntity<?> saveClockSchedule(@PathVariable UUID dayId,
                                                @Valid @RequestBody ClockScheduleSaveRequest req,
                                                @AuthenticationPrincipal AppUser user) {
        try {
            var rows = (req.segments() == null ? List.<ClockSegmentRequest>of() : req.segments()).stream()
                    .map(r -> new ScheduleService.ClockSegmentInput(
                            r.localStartMinutes(),
                            r.localEndMinutes(),
                            UUID.fromString(r.clockTemplateId().trim())))
                    .toList();
            var view = schedule.saveClockSchedule(dayId, user.getId(), rows);
            return ResponseEntity.ok(toDto(view, user.getId()));
        } catch (ScheduleService.ConcurrencyException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/api/days/{dayId}/preview")
    public ResponseEntity<?> preview(@PathVariable UUID dayId) {
        return ResponseEntity.ok(schedule.previewResolution(dayId));
    }

    @PostMapping("/api/days/{dayId}/reopen")
    public ResponseEntity<?> reopen(@PathVariable UUID dayId, @AuthenticationPrincipal AppUser user) {
        if (!"ADMIN".equals(user.getRole())) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin only"));
        }
        if (!days.existsById(dayId)) return ResponseEntity.notFound().build();
        ScheduleDay d = schedule.reopen(dayId);
        return ResponseEntity.ok(Map.of("id", d.getId().toString(), "status", d.getStatus()));
    }

    private DayDto toDto(ScheduleService.DayView v, UUID requesterId) {
        LockDto lockDto = null;
        if (v.lock() != null) {
            String name = v.lockHolder() != null ? v.lockHolder().getName() : null;
            if (name == null && v.lockHolder() != null) name = v.lockHolder().getEmail();
            lockDto = new LockDto(v.lock().getUserId().toString(), name,
                    v.lock().getAcquiredAt(), v.lock().getExpiresAt(),
                    v.lock().getUserId().equals(requesterId));
        }
        boolean readOnly = v.lockedByOtherUser() || "PUSHED".equals(v.day().getStatus());
        return new DayDto(
                v.day().getId().toString(),
                v.day().getStationId().toString(),
                v.day().getDate(),
                v.day().getStatus(),
                v.day().getPushedAt(),
                v.day().getPushedBy() == null ? null : v.day().getPushedBy().toString(),
                v.day().getVersion(),
                lockDto,
                v.items().stream().map(ItemDto::from).toList(),
                v.clockSegments().stream().map(ClockSegmentDto::from).toList(),
                readOnly);
    }
}
