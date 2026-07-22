package com.onelpro.librelog.playback;

import com.onelpro.librelog.librtime.LibreTimeConnection;
import com.onelpro.librelog.librtime.LibreTimeConnectionRepository;
import com.onelpro.librelog.station.StationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the nightly reconciliation job: it iterates every configured
 * LibreTime connection and a failure on one station does not stop the others.
 */
class ReconciliationJobTest {

    private LibreTimeConnectionRepository connections;
    private StationRepository stations;
    private PlaybackService playback;
    private ReconciliationJob job;

    private final UUID stationA = UUID.randomUUID();
    private final UUID stationB = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        connections = mock(LibreTimeConnectionRepository.class);
        stations = mock(StationRepository.class);
        playback = mock(PlaybackService.class);
        job = new ReconciliationJob(connections, stations, playback);

        when(connections.findAll()).thenReturn(List.of(
                LibreTimeConnection.builder().stationId(stationA).baseUrl("http://a").apiKeyEncrypted("x").build(),
                LibreTimeConnection.builder().stationId(stationB).baseUrl("http://b").apiKeyEncrypted("x").build()));
        when(stations.findById(any())).thenReturn(Optional.empty()); // -> UTC zone
    }

    @Test
    void importYesterday_importsEveryConfiguredStation() {
        when(playback.importDay(any(), any())).thenReturn(new PlaybackService.ImportResult(10, 5, 1));
        when(playback.fulfillment(any(), any())).thenReturn(List.of());

        job.importYesterday();

        LocalDate yesterday = LocalDate.now(java.time.ZoneOffset.UTC).minusDays(1);
        verify(playback).importDay(eq(stationA), eq(yesterday));
        verify(playback).importDay(eq(stationB), eq(yesterday));
        verify(playback).fulfillment(eq(stationA), eq(yesterday));
        verify(playback).fulfillment(eq(stationB), eq(yesterday));
    }

    @Test
    void importYesterday_continuesAfterPerStationFailure() {
        when(playback.importDay(eq(stationA), any())).thenThrow(new RuntimeException("LibreTime down"));
        when(playback.importDay(eq(stationB), any())).thenReturn(new PlaybackService.ImportResult(3, 3, 0));
        when(playback.fulfillment(eq(stationB), any())).thenReturn(List.of());

        job.importYesterday();

        verify(playback).importDay(eq(stationA), any());
        verify(playback).importDay(eq(stationB), any());
        // fulfillment is only computed for the station whose import succeeded
        verify(playback, never()).fulfillment(eq(stationA), any());
        verify(playback).fulfillment(eq(stationB), any());
    }

    @Test
    void importYesterday_noConnectionsIsANoOp() {
        when(connections.findAll()).thenReturn(List.of());

        job.importYesterday();

        verify(playback, never()).importDay(any(), any());
    }
}
