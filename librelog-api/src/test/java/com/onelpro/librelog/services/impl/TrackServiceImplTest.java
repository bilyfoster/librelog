package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.TrackRequestDTO;
import com.onelpro.librelog.dto.TrackResponseDTO;
import com.onelpro.librelog.enums.TrackType;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.models.Station;
import com.onelpro.librelog.models.Track;
import com.onelpro.librelog.repositories.StationRepository;
import com.onelpro.librelog.repositories.TrackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrackServiceImplTest {

	@Mock
	private TrackRepository trackRepository;

	@Mock
	private StationRepository stationRepository;

	@InjectMocks
	private TrackServiceImpl trackService;

	private UUID trackId;
	private UUID stationId;
	private Station station;
	private Track track;
	private TrackRequestDTO requestDTO;

	@BeforeEach
	void setUp() {
		trackId = UUID.randomUUID();
		stationId = UUID.randomUUID();

		station = new Station();
		station.setId(stationId);
		station.setName("Test Station");

		track = Track.builder()
				.id(trackId)
				.title("Test Song")
				.artist("Test Artist")
				.album("Test Album")
				.type(TrackType.MUSIC)
				.genre("Rock")
				.durationSeconds(180)
				.station(station)
				.playCount(5)
				.build();

		requestDTO = TrackRequestDTO.builder()
				.title("Test Song")
				.artist("Test Artist")
				.album("Test Album")
				.type(TrackType.MUSIC)
				.genre("Rock")
				.durationSeconds(180)
				.stationId(stationId)
				.build();
	}

	@Test
	void create_Success() {
		when(stationRepository.findById(stationId)).thenReturn(Optional.of(station));
		when(trackRepository.save(any(Track.class))).thenReturn(track);

		TrackResponseDTO result = trackService.create(requestDTO);

		assertNotNull(result);
		assertEquals("Test Song", result.getTitle());
		assertEquals("Test Artist", result.getArtist());
		assertEquals(TrackType.MUSIC, result.getType());
		verify(trackRepository).save(any(Track.class));
	}

	@Test
	void create_WithoutStation_Success() {
		requestDTO.setStationId(null);
		track.setStation(null);
		when(trackRepository.save(any(Track.class))).thenReturn(track);

		TrackResponseDTO result = trackService.create(requestDTO);

		assertNotNull(result);
		assertEquals("Test Song", result.getTitle());
	}

	@Test
	void getById_Success() {
		when(trackRepository.findById(trackId)).thenReturn(Optional.of(track));

		TrackResponseDTO result = trackService.getById(trackId);

		assertNotNull(result);
		assertEquals(trackId, result.getId());
		assertEquals(5, result.getPlayCount());
	}

	@Test
	void getById_NotFound_ThrowsException() {
		UUID randomId = UUID.randomUUID();
		when(trackRepository.findById(randomId)).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () -> trackService.getById(randomId));
	}

	@Test
	void getByStationId_Success() {
		when(trackRepository.findByStationId(stationId)).thenReturn(Collections.singletonList(track));

		var results = trackService.getByStationId(stationId);

		assertNotNull(results);
		assertEquals(1, results.size());
		assertEquals("Test Song", results.get(0).getTitle());
	}

	@Test
	void getByType_Success() {
		when(trackRepository.findByStationIdAndType(stationId, TrackType.MUSIC))
				.thenReturn(Collections.singletonList(track));

		var results = trackService.getByType(stationId, TrackType.MUSIC);

		assertNotNull(results);
		assertEquals(1, results.size());
		assertEquals(TrackType.MUSIC, results.get(0).getType());
	}

	@Test
	void search_Success() {
		when(trackRepository.searchByTitleOrArtist("Test"))
				.thenReturn(Collections.singletonList(track));

		var results = trackService.search("Test");

		assertNotNull(results);
		assertEquals(1, results.size());
	}

	@Test
	void recordPlay_Success() {
		when(trackRepository.findById(trackId)).thenReturn(Optional.of(track));
		when(trackRepository.save(any(Track.class))).thenReturn(track);

		TrackResponseDTO result = trackService.recordPlay(trackId);

		assertNotNull(result);
		assertEquals(6, result.getPlayCount());
		assertNotNull(result.getLastPlayed());
	}

	@Test
	void delete_Success() {
		when(trackRepository.existsById(trackId)).thenReturn(true);
		doNothing().when(trackRepository).deleteById(trackId);

		assertDoesNotThrow(() -> trackService.delete(trackId));
		verify(trackRepository).deleteById(trackId);
	}

	@Test
	void delete_NotFound_ThrowsException() {
		UUID randomId = UUID.randomUUID();
		when(trackRepository.existsById(randomId)).thenReturn(false);

		assertThrows(NotFoundException.class, () -> trackService.delete(randomId));
	}

}
