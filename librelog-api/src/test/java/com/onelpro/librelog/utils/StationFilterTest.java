package com.onelpro.librelog.utils;

import com.onelpro.librelog.models.Station;
import com.onelpro.librelog.services.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for StationFilter utility class.
 */
@ExtendWith(MockitoExtension.class)
class StationFilterTest {

	@Mock
	private PermissionService permissionService;

	private StationFilter stationFilter;
	private UUID userId;
	private UUID stationId1;
	private UUID stationId2;
	private UUID stationId3;

	@BeforeEach
	void setUp() throws Exception {
		// Reset static service to null before each test
		try {
			java.lang.reflect.Field field = StationFilter.class.getDeclaredField("permissionService");
			field.setAccessible(true);
			field.set(null, null);
		} catch (Exception e) {
			// Ignore
		}

		stationFilter = new StationFilter();
		stationFilter.setPermissionService(permissionService);
		userId = UUID.randomUUID();
		stationId1 = UUID.randomUUID();
		stationId2 = UUID.randomUUID();
		stationId3 = UUID.randomUUID();
	}

	@Test
	void testFilterByUserStations_WhenUserHasStations_ExpectFilteredList() {
		Station station1 = createStation(stationId1, "Station 1");
		Station station2 = createStation(stationId2, "Station 2");
		Station station3 = createStation(stationId3, "Station 3");
		List<Station> stations = Arrays.asList(station1, station2, station3);

		List<UUID> userStations = Arrays.asList(stationId1, stationId2);
		when(permissionService.getUserStations(userId)).thenReturn(userStations);

		List<Station> result = StationFilter.filterByUserStations(stations, userId, Station::getId);

		assertEquals(2, result.size());
		assertTrue(result.contains(station1));
		assertTrue(result.contains(station2));
		assertFalse(result.contains(station3));
		verify(permissionService).getUserStations(userId);
	}

	@Test
	void testFilterByUserStations_WhenUserHasNoStations_ExpectEmptyList() {
		Station station1 = createStation(stationId1, "Station 1");
		List<Station> stations = Arrays.asList(station1);

		when(permissionService.getUserStations(userId)).thenReturn(new ArrayList<>());

		List<Station> result = StationFilter.filterByUserStations(stations, userId, Station::getId);

		assertTrue(result.isEmpty());
		verify(permissionService).getUserStations(userId);
	}

	@Test
	void testFilterByUserStations_WhenStationIdIsNull_ExpectExcluded() {
		Station station1 = createStation(stationId1, "Station 1");
		Station station2 = createStation(null, "Station 2");
		List<Station> stations = Arrays.asList(station1, station2);

		List<UUID> userStations = Arrays.asList(stationId1);
		when(permissionService.getUserStations(userId)).thenReturn(userStations);

		List<Station> result = StationFilter.filterByUserStations(stations, userId, Station::getId);

		assertEquals(1, result.size());
		assertTrue(result.contains(station1));
		assertFalse(result.contains(station2));
	}

	@Test
	void testFilterByUserStations_WhenServiceNotInitialized_ExpectThrowsException() throws Exception {
		// Reset static service to null
		java.lang.reflect.Field field = StationFilter.class.getDeclaredField("permissionService");
		field.setAccessible(true);
		field.set(null, null);

		List<Station> stations = Arrays.asList(createStation(stationId1, "Station 1"));

		assertThrows(IllegalStateException.class, () -> {
			StationFilter.filterByUserStations(stations, userId, Station::getId);
		});
	}

	@Test
	void testFilterByStationAccess_WhenUserCanAccess_ExpectFilteredList() {
		Station station1 = createStation(stationId1, "Station 1");
		Station station2 = createStation(stationId2, "Station 2");
		List<Station> stations = Arrays.asList(station1, station2);

		when(permissionService.canAccessStation(userId, stationId1)).thenReturn(true);
		when(permissionService.canAccessStation(userId, stationId2)).thenReturn(false);

		List<Station> result = StationFilter.filterByStationAccess(stations, userId, Station::getId);

		assertEquals(1, result.size());
		assertTrue(result.contains(station1));
		assertFalse(result.contains(station2));
		verify(permissionService).canAccessStation(userId, stationId1);
		verify(permissionService).canAccessStation(userId, stationId2);
	}

	@Test
	void testFilterByStationAccess_WhenStationIdIsNull_ExpectExcluded() {
		Station station1 = createStation(stationId1, "Station 1");
		Station station2 = createStation(null, "Station 2");
		List<Station> stations = Arrays.asList(station1, station2);

		when(permissionService.canAccessStation(userId, stationId1)).thenReturn(true);

		List<Station> result = StationFilter.filterByStationAccess(stations, userId, Station::getId);

		assertEquals(1, result.size());
		assertTrue(result.contains(station1));
		assertFalse(result.contains(station2));
		verify(permissionService, never()).canAccessStation(userId, null);
	}

	@Test
	void testFilterByStationAccess_WhenServiceNotInitialized_ExpectThrowsException() throws Exception {
		// Reset static service to null
		java.lang.reflect.Field field = StationFilter.class.getDeclaredField("permissionService");
		field.setAccessible(true);
		field.set(null, null);

		List<Station> stations = Arrays.asList(createStation(stationId1, "Station 1"));

		assertThrows(IllegalStateException.class, () -> {
			StationFilter.filterByStationAccess(stations, userId, Station::getId);
		});
	}

	@Test
	void testFilterByUserStations_WhenEmptyList_ExpectEmptyList() {
		List<Station> stations = new ArrayList<>();
		when(permissionService.getUserStations(userId)).thenReturn(Arrays.asList(stationId1));

		List<Station> result = StationFilter.filterByUserStations(stations, userId, Station::getId);

		assertTrue(result.isEmpty());
		verify(permissionService).getUserStations(userId);
	}

	@Test
	void testFilterByStationAccess_WhenEmptyList_ExpectEmptyList() {
		List<Station> stations = new ArrayList<>();

		List<Station> result = StationFilter.filterByStationAccess(stations, userId, Station::getId);

		assertTrue(result.isEmpty());
		verify(permissionService, never()).canAccessStation(any(), any());
	}

	private Station createStation(UUID id, String name) {
		Station station = new Station();
		station.setId(id);
		station.setName(name);
		return station;
	}

}

