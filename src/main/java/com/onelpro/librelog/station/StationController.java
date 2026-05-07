package com.onelpro.librelog.station;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/stations")
@RequiredArgsConstructor
public class StationController {

    private final StationRepository stations;

    public record StationDto(String id, String name, String callLetters, String timeZone) {
        static StationDto from(Station s) {
            return new StationDto(s.getId().toString(), s.getName(), s.getCallLetters(), s.getTimeZone());
        }
    }

    public record StationRequest(@NotBlank String name, String callLetters, String timeZone) {}

    @GetMapping
    public List<StationDto> list() {
        return stations.findAll().stream().map(StationDto::from).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<StationDto> get(@PathVariable UUID id) {
        return stations.findById(id).map(StationDto::from)
                .map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public StationDto create(@Valid @RequestBody StationRequest req) {
        Station s = Station.builder()
                .name(req.name())
                .callLetters(req.callLetters())
                .timeZone(req.timeZone() != null ? req.timeZone() : "UTC")
                .build();
        return StationDto.from(stations.save(s));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StationDto> update(@PathVariable UUID id, @Valid @RequestBody StationRequest req) {
        var s = stations.findById(id).orElse(null);
        if (s == null) return ResponseEntity.notFound().build();
        s.setName(req.name());
        s.setCallLetters(req.callLetters());
        if (req.timeZone() != null) s.setTimeZone(req.timeZone());
        return ResponseEntity.ok(StationDto.from(stations.save(s)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (!stations.existsById(id)) return ResponseEntity.notFound().build();
        stations.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
