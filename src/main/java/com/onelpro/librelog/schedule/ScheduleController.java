package com.onelpro.librelog.schedule;

import com.onelpro.librelog.auth.AppUser;
import jakarta.validation.Valid;
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
                         Integer lengthSeconds, int position) {
        static ItemDto from(ScheduleItem i) {
            return new ItemDto(i.getId().toString(), i.getShowInstanceId(), i.getSlotIndex(),
                    i.getKind(), i.getSpotId() == null ? null : i.getSpotId().toString(),
                    i.getLibrtimeFileId(), i.getScheduledAt(), i.getLengthSeconds(), i.getPosition());
        }
    }

    public record DayDto(String id, String stationId, LocalDate date, String status,
                         Instant pushedAt, String pushedBy, Long version,
                         LockDto lock, List<ItemDto> items, boolean readOnly) {}

    public record LockDto(String userId, String userName, Instant acquiredAt, Instant expiresAt, boolean self) {}

    public record ItemRequest(Long showInstanceId, int slotIndex, String kind,
                              String spotId, Long librtimeFileId, Instant scheduledAt,
                              Integer lengthSeconds) {}

    public record SaveRequest(Long expectedVersion, List<ItemRequest> items) {}

    @GetMapping("/api/stations/{stationId}/days/{date}")
    public DayDto load(@PathVariable UUID stationId, @PathVariable String date,
                       @AuthenticationPrincipal AppUser user) {
        var view = schedule.load(stationId, LocalDate.parse(date), user.getId());
        return toDto(view, user.getId());
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
                    .spotId(r.spotId() == null ? null : UUID.fromString(r.spotId()))
                    .librtimeFileId(r.librtimeFileId())
                    .scheduledAt(r.scheduledAt())
                    .lengthSeconds(r.lengthSeconds())
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
            ScheduleDay d = schedule.push(dayId, user.getId());
            return ResponseEntity.ok(Map.of(
                    "id", d.getId().toString(),
                    "status", d.getStatus(),
                    "pushedAt", d.getPushedAt(),
                    "pushedBy", d.getPushedBy() == null ? null : d.getPushedBy().toString()));
        } catch (ScheduleService.ConcurrencyException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(502).body(Map.of("error", e.getMessage()));
        }
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
                readOnly);
    }
}
