package com.onelpro.librelog.playback;

import com.onelpro.librelog.librtime.LibreTimeConnection;
import com.onelpro.librelog.librtime.LibreTimeConnectionRepository;
import com.onelpro.librelog.station.StationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Nightly as-run reconciliation (PRD §7.3): for every station with a configured
 * LibreTime connection, pulls yesterday's playout history, reconciles it against the
 * schedule, and logs the per-order fulfillment summary (missed spots = make-ups owed).
 * A failure on one station never aborts the rest.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReconciliationJob {

    private final LibreTimeConnectionRepository connections;
    private final StationRepository stations;
    private final PlaybackService playback;

    @Scheduled(cron = "${librelog.reconciliation.cron:0 30 3 * * *}")
    public void importYesterday() {
        var conns = connections.findAll();
        int ok = 0, failed = 0, totalMissed = 0;
        for (LibreTimeConnection conn : conns) {
            try {
                LocalDate yesterday = LocalDate.now(zoneFor(conn)).minusDays(1);
                var result = playback.importDay(conn.getStationId(), yesterday);
                var rows = playback.fulfillment(conn.getStationId(), yesterday);
                int missed = rows.stream().mapToInt(PlaybackService.FulfillmentRow::missed).sum();
                totalMissed += missed;
                ok++;
                log.info("Reconciliation station={} date={}: {} entries saved, {} matched, {} missed; "
                                + "fulfillment: {} orders, {} make-ups owed",
                        conn.getStationId(), yesterday, result.entriesSaved(), result.matched(),
                        result.missed(), rows.size(), missed);
            } catch (Exception e) {
                failed++;
                log.warn("Reconciliation failed for station {}: {}", conn.getStationId(), e.getMessage());
            }
        }
        log.info("Nightly reconciliation done: {} station(s) ok, {} failed, {} make-ups owed in total",
                ok, failed, totalMissed);
    }

    private ZoneId zoneFor(LibreTimeConnection conn) {
        try {
            return stations.findById(conn.getStationId())
                    .map(s -> ZoneId.of(s.getTimeZone()))
                    .orElse(ZoneOffset.UTC);
        } catch (Exception e) {
            return ZoneOffset.UTC;
        }
    }
}
