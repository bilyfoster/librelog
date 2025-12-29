package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.AvailTypeRequestDTO;
import com.onelpro.librelog.exceptions.ConflictException;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.models.AvailType;
import com.onelpro.librelog.repositories.AvailTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvailTypeServiceImplTest {

	@Mock
	private AvailTypeRepository availTypeRepository;

	@InjectMocks
	private AvailTypeServiceImpl availTypeService;

	private AvailType availType;
	private UUID availTypeId;

	@BeforeEach
	void setUp() {
		availTypeId = UUID.randomUUID();

		availType = AvailType.builder()
				.id(availTypeId)
				.name("General")
				.description("General commercial break")
				.isActive(true)
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();
	}

	@Test
	void create_When_ValidRequest_Expect_Success() {
		AvailTypeRequestDTO request = AvailTypeRequestDTO.builder()
				.name("General")
				.description("General commercial break")
				.isActive(true)
				.build();

		when(availTypeRepository.findByNameIgnoreCase("General")).thenReturn(Optional.empty());
		when(availTypeRepository.save(any(AvailType.class))).thenReturn(availType);

		var result = availTypeService.create(request);

		assertNotNull(result);
		assertEquals(availTypeId, result.getId());
		assertEquals("General", result.getName());
		verify(availTypeRepository).save(any(AvailType.class));
	}

	@Test
	void create_When_NameAlreadyExists_Expect_ConflictException() {
		AvailTypeRequestDTO request = AvailTypeRequestDTO.builder()
				.name("General")
				.description("General commercial break")
				.isActive(true)
				.build();

		when(availTypeRepository.findByNameIgnoreCase("General")).thenReturn(Optional.of(availType));

		assertThrows(ConflictException.class, () -> availTypeService.create(request));
		verify(availTypeRepository, never()).save(any(AvailType.class));
	}

	@Test
	void getById_When_Exists_Expect_Success() {
		when(availTypeRepository.findById(availTypeId)).thenReturn(Optional.of(availType));

		var result = availTypeService.getById(availTypeId);

		assertNotNull(result);
		assertEquals(availTypeId, result.getId());
		assertEquals("General", result.getName());
	}

	@Test
	void getById_When_NotFound_Expect_NotFoundException() {
		when(availTypeRepository.findById(availTypeId)).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () -> availTypeService.getById(availTypeId));
	}

	@Test
	void getAll_When_Exists_Expect_Success() {
		when(availTypeRepository.findAll()).thenReturn(List.of(availType));

		var result = availTypeService.getAll();

		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals(availTypeId, result.get(0).getId());
	}

	@Test
	void getActive_When_Exists_Expect_Success() {
		when(availTypeRepository.findByIsActiveTrue()).thenReturn(List.of(availType));

		var result = availTypeService.getActive();

		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals(availTypeId, result.get(0).getId());
	}

	@Test
	void update_When_ValidRequest_Expect_Success() {
		AvailTypeRequestDTO request = AvailTypeRequestDTO.builder()
				.name("Premium")
				.description("Premium commercial break")
				.isActive(true)
				.build();

		when(availTypeRepository.findById(availTypeId)).thenReturn(Optional.of(availType));
		when(availTypeRepository.findByNameIgnoreCase("Premium")).thenReturn(Optional.empty());
		when(availTypeRepository.save(any(AvailType.class))).thenReturn(availType);

		var result = availTypeService.update(availTypeId, request);

		assertNotNull(result);
		verify(availTypeRepository).save(any(AvailType.class));
	}

	@Test
	void update_When_NotFound_Expect_NotFoundException() {
		AvailTypeRequestDTO request = AvailTypeRequestDTO.builder()
				.name("Premium")
				.description("Premium commercial break")
				.isActive(true)
				.build();

		when(availTypeRepository.findById(availTypeId)).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () -> availTypeService.update(availTypeId, request));
		verify(availTypeRepository, never()).save(any(AvailType.class));
	}

	@Test
	void update_When_NameAlreadyExists_Expect_ConflictException() {
		AvailType existingAvailType = AvailType.builder()
				.id(UUID.randomUUID())
				.name("Premium")
				.build();

		AvailTypeRequestDTO request = AvailTypeRequestDTO.builder()
				.name("Premium")
				.description("Premium commercial break")
				.isActive(true)
				.build();

		when(availTypeRepository.findById(availTypeId)).thenReturn(Optional.of(availType));
		when(availTypeRepository.findByNameIgnoreCase("Premium")).thenReturn(Optional.of(existingAvailType));

		assertThrows(ConflictException.class, () -> availTypeService.update(availTypeId, request));
		verify(availTypeRepository, never()).save(any(AvailType.class));
	}

	@Test
	void delete_When_Exists_Expect_Success() {
		when(availTypeRepository.existsById(availTypeId)).thenReturn(true);

		availTypeService.delete(availTypeId);

		verify(availTypeRepository).deleteById(availTypeId);
	}

	@Test
	void delete_When_NotFound_Expect_NotFoundException() {
		when(availTypeRepository.existsById(availTypeId)).thenReturn(false);

		assertThrows(NotFoundException.class, () -> availTypeService.delete(availTypeId));
		verify(availTypeRepository, never()).deleteById(availTypeId);
	}

}

