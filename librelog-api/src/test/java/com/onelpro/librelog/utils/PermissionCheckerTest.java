package com.onelpro.librelog.utils;

import com.onelpro.librelog.enums.ActionType;
import com.onelpro.librelog.enums.ModuleType;
import com.onelpro.librelog.services.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PermissionChecker utility class.
 */
@ExtendWith(MockitoExtension.class)
class PermissionCheckerTest {

	@Mock
	private PermissionService permissionService;

	private PermissionChecker permissionChecker;
	private UUID userId;
	private UUID stationId;
	private ModuleType moduleType;
	private ActionType actionType;

	@BeforeEach
	void setUp() {
		permissionChecker = new PermissionChecker();
		permissionChecker.setPermissionService(permissionService);
		userId = UUID.randomUUID();
		stationId = UUID.randomUUID();
		moduleType = ModuleType.ORDERS;
		actionType = ActionType.VIEW;
	}

	@Test
	void testHasPermission_WhenServiceInitialized_ExpectCallsService() {
		when(permissionService.hasPermission(userId, stationId, moduleType, actionType)).thenReturn(true);

		boolean result = PermissionChecker.hasPermission(userId, stationId, moduleType, actionType);

		assertTrue(result);
		verify(permissionService).hasPermission(userId, stationId, moduleType, actionType);
	}

	@Test
	void testHasPermission_WhenServiceReturnsFalse_ExpectFalse() {
		when(permissionService.hasPermission(userId, stationId, moduleType, actionType)).thenReturn(false);

		boolean result = PermissionChecker.hasPermission(userId, stationId, moduleType, actionType);

		assertFalse(result);
		verify(permissionService).hasPermission(userId, stationId, moduleType, actionType);
	}

	@Test
	void testHasPermission_WhenServiceNotInitialized_ExpectThrowsException() throws Exception {
		// Reset static service to null
		java.lang.reflect.Field field = PermissionChecker.class.getDeclaredField("permissionService");
		field.setAccessible(true);
		field.set(null, null);

		assertThrows(IllegalStateException.class, () -> {
			PermissionChecker.hasPermission(userId, stationId, moduleType, actionType);
		});
	}

	@Test
	void testCanAccessStation_WhenServiceInitialized_ExpectCallsService() {
		when(permissionService.canAccessStation(userId, stationId)).thenReturn(true);

		boolean result = PermissionChecker.canAccessStation(userId, stationId);

		assertTrue(result);
		verify(permissionService).canAccessStation(userId, stationId);
	}

	@Test
	void testCanAccessStation_WhenServiceReturnsFalse_ExpectFalse() {
		when(permissionService.canAccessStation(userId, stationId)).thenReturn(false);

		boolean result = PermissionChecker.canAccessStation(userId, stationId);

		assertFalse(result);
		verify(permissionService).canAccessStation(userId, stationId);
	}

	@Test
	void testCanAccessStation_WhenServiceNotInitialized_ExpectThrowsException() throws Exception {
		// Reset static service to null
		java.lang.reflect.Field field = PermissionChecker.class.getDeclaredField("permissionService");
		field.setAccessible(true);
		field.set(null, null);

		assertThrows(IllegalStateException.class, () -> {
			PermissionChecker.canAccessStation(userId, stationId);
		});
	}

	@Test
	void testCanView_ExpectCallsHasPermissionWithView() {
		when(permissionService.hasPermission(userId, stationId, moduleType, ActionType.VIEW)).thenReturn(true);

		boolean result = PermissionChecker.canView(userId, stationId, moduleType);

		assertTrue(result);
		verify(permissionService).hasPermission(userId, stationId, moduleType, ActionType.VIEW);
	}

	@Test
	void testCanCreate_ExpectCallsHasPermissionWithCreate() {
		when(permissionService.hasPermission(userId, stationId, moduleType, ActionType.CREATE)).thenReturn(true);

		boolean result = PermissionChecker.canCreate(userId, stationId, moduleType);

		assertTrue(result);
		verify(permissionService).hasPermission(userId, stationId, moduleType, ActionType.CREATE);
	}

	@Test
	void testCanEdit_ExpectCallsHasPermissionWithEdit() {
		when(permissionService.hasPermission(userId, stationId, moduleType, ActionType.EDIT)).thenReturn(true);

		boolean result = PermissionChecker.canEdit(userId, stationId, moduleType);

		assertTrue(result);
		verify(permissionService).hasPermission(userId, stationId, moduleType, ActionType.EDIT);
	}

	@Test
	void testCanDelete_ExpectCallsHasPermissionWithDelete() {
		when(permissionService.hasPermission(userId, stationId, moduleType, ActionType.DELETE)).thenReturn(true);

		boolean result = PermissionChecker.canDelete(userId, stationId, moduleType);

		assertTrue(result);
		verify(permissionService).hasPermission(userId, stationId, moduleType, ActionType.DELETE);
	}

	@Test
	void testCanView_WithNullStationId() {
		when(permissionService.hasPermission(userId, null, moduleType, ActionType.VIEW)).thenReturn(true);

		boolean result = PermissionChecker.canView(userId, null, moduleType);

		assertTrue(result);
		verify(permissionService).hasPermission(userId, null, moduleType, ActionType.VIEW);
	}

	@Test
	void testCanCreate_WithNullStationId() {
		when(permissionService.hasPermission(userId, null, moduleType, ActionType.CREATE)).thenReturn(true);

		boolean result = PermissionChecker.canCreate(userId, null, moduleType);

		assertTrue(result);
		verify(permissionService).hasPermission(userId, null, moduleType, ActionType.CREATE);
	}

	@Test
	void testCanEdit_WithNullStationId() {
		when(permissionService.hasPermission(userId, null, moduleType, ActionType.EDIT)).thenReturn(true);

		boolean result = PermissionChecker.canEdit(userId, null, moduleType);

		assertTrue(result);
		verify(permissionService).hasPermission(userId, null, moduleType, ActionType.EDIT);
	}

	@Test
	void testCanDelete_WithNullStationId() {
		when(permissionService.hasPermission(userId, null, moduleType, ActionType.DELETE)).thenReturn(true);

		boolean result = PermissionChecker.canDelete(userId, null, moduleType);

		assertTrue(result);
		verify(permissionService).hasPermission(userId, null, moduleType, ActionType.DELETE);
	}

}

