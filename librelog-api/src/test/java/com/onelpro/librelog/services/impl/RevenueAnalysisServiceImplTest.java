package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.BreakStructureResponseDTO;
import com.onelpro.librelog.dto.RevenueAnalysisDTO;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.models.ClockTemplate;
import com.onelpro.librelog.repositories.ClockTemplateRepository;
import com.onelpro.librelog.services.BreakStructureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RevenueAnalysisServiceImplTest {

	@Mock
	private BreakStructureService breakStructureService;

	@Mock
	private ClockTemplateRepository clockTemplateRepository;

	@InjectMocks
	private RevenueAnalysisServiceImpl revenueAnalysisService;

	private UUID clockTemplateId;
	private ClockTemplate clockTemplate;

	@BeforeEach
	void setUp() {
		clockTemplateId = UUID.randomUUID();
		clockTemplate = ClockTemplate.builder()
				.id(clockTemplateId)
				.name("Test Clock")
				.build();
	}

	@Test
	void calculateRevenueImpact_When_ClockTemplateExistsWithBreaks_Expect_RevenueCalculated() {
		// Arrange
		UUID break1Id = UUID.randomUUID();
		UUID break2Id = UUID.randomUUID();

		BreakStructureResponseDTO break1 = BreakStructureResponseDTO.builder()
				.id(break1Id)
				.name("Break 1")
				.durationSeconds(60) // 1 minute
				.availTypeName("Prime")
				.build();

		BreakStructureResponseDTO break2 = BreakStructureResponseDTO.builder()
				.id(break2Id)
				.name("Break 2")
				.durationSeconds(120) // 2 minutes
				.availTypeName("Prime")
				.build();

		List<BreakStructureResponseDTO> breaks = List.of(break1, break2);

		when(clockTemplateRepository.findById(clockTemplateId)).thenReturn(Optional.of(clockTemplate));
		when(breakStructureService.getByClockTemplateId(clockTemplateId)).thenReturn(breaks);

		// Act
		RevenueAnalysisDTO result = revenueAnalysisService.calculateRevenueImpact(clockTemplateId);

		// Assert
		assertNotNull(result);
		assertEquals(2, result.getBreakCount());
		assertEquals(3.0, result.getTotalInventoryMinutes(), 0.01); // 1 + 2 minutes
		assertNotNull(result.getPotentialRevenue());
		assertTrue(result.getPotentialRevenue().compareTo(BigDecimal.ZERO) > 0);
		assertEquals(2, result.getRevenueByBreak().size());
		assertTrue(result.getRevenueByBreak().containsKey(break1Id));
		assertTrue(result.getRevenueByBreak().containsKey(break2Id));
		assertTrue(result.getRevenueByAvailType().containsKey("Prime"));
		assertNotNull(result.getAverageRevenuePerMinute());
		assertNotNull(result.getAverageRevenuePerBreak());
	}

	@Test
	void calculateRevenueImpact_When_ClockTemplateNotFound_Expect_NotFoundException() {
		// Arrange
		when(clockTemplateRepository.findById(clockTemplateId)).thenReturn(Optional.empty());

		// Act & Assert
		assertThrows(NotFoundException.class, () -> revenueAnalysisService.calculateRevenueImpact(clockTemplateId));
		verify(breakStructureService, never()).getByClockTemplateId(any());
	}

	@Test
	void calculateRevenueImpact_When_NoBreaks_Expect_ZeroRevenueAndWarning() {
		// Arrange
		List<BreakStructureResponseDTO> breaks = new ArrayList<>();

		when(clockTemplateRepository.findById(clockTemplateId)).thenReturn(Optional.of(clockTemplate));
		when(breakStructureService.getByClockTemplateId(clockTemplateId)).thenReturn(breaks);

		// Act
		RevenueAnalysisDTO result = revenueAnalysisService.calculateRevenueImpact(clockTemplateId);

		// Assert
		assertNotNull(result);
		assertEquals(0, result.getBreakCount());
		assertEquals(0.0, result.getTotalInventoryMinutes(), 0.01);
		assertEquals(BigDecimal.ZERO, result.getPotentialRevenue());
		assertEquals(BigDecimal.ZERO, result.getAverageRevenuePerMinute());
		assertEquals(BigDecimal.ZERO, result.getAverageRevenuePerBreak());
		assertTrue(result.getWarnings().stream()
				.anyMatch(w -> w.contains("No commercial breaks found")));
	}

	@Test
	void calculateRevenueImpact_When_LowInventory_Expect_Warning() {
		// Arrange
		BreakStructureResponseDTO break1 = BreakStructureResponseDTO.builder()
				.id(UUID.randomUUID())
				.name("Break 1")
				.durationSeconds(300) // 5 minutes
				.build();

		List<BreakStructureResponseDTO> breaks = List.of(break1);

		when(clockTemplateRepository.findById(clockTemplateId)).thenReturn(Optional.of(clockTemplate));
		when(breakStructureService.getByClockTemplateId(clockTemplateId)).thenReturn(breaks);

		// Act
		RevenueAnalysisDTO result = revenueAnalysisService.calculateRevenueImpact(clockTemplateId);

		// Assert
		assertNotNull(result);
		assertTrue(result.getWarnings().stream()
				.anyMatch(w -> w.contains("Low commercial inventory")));
	}

	@Test
	void calculateRevenueImpact_When_HighInventory_Expect_Warning() {
		// Arrange
		BreakStructureResponseDTO break1 = BreakStructureResponseDTO.builder()
				.id(UUID.randomUUID())
				.name("Break 1")
				.durationSeconds(1260) // 21 minutes (above 20.0 threshold)
				.build();

		List<BreakStructureResponseDTO> breaks = List.of(break1);

		when(clockTemplateRepository.findById(clockTemplateId)).thenReturn(Optional.of(clockTemplate));
		when(breakStructureService.getByClockTemplateId(clockTemplateId)).thenReturn(breaks);

		// Act
		RevenueAnalysisDTO result = revenueAnalysisService.calculateRevenueImpact(clockTemplateId);

		// Assert
		assertNotNull(result);
		assertTrue(result.getWarnings().stream()
				.anyMatch(w -> w.contains("High commercial inventory")));
	}

	@Test
	void calculateRevenueImpact_When_MultipleAvailTypes_Expect_RevenueByAvailType() {
		// Arrange
		BreakStructureResponseDTO break1 = BreakStructureResponseDTO.builder()
				.id(UUID.randomUUID())
				.name("Break 1")
				.durationSeconds(60) // 1 minute
				.availTypeName("Prime")
				.build();

		BreakStructureResponseDTO break2 = BreakStructureResponseDTO.builder()
				.id(UUID.randomUUID())
				.name("Break 2")
				.durationSeconds(60) // 1 minute
				.availTypeName("Secondary")
				.build();

		BreakStructureResponseDTO break3 = BreakStructureResponseDTO.builder()
				.id(UUID.randomUUID())
				.name("Break 3")
				.durationSeconds(60) // 1 minute
				.availTypeName("Prime")
				.build();

		List<BreakStructureResponseDTO> breaks = List.of(break1, break2, break3);

		when(clockTemplateRepository.findById(clockTemplateId)).thenReturn(Optional.of(clockTemplate));
		when(breakStructureService.getByClockTemplateId(clockTemplateId)).thenReturn(breaks);

		// Act
		RevenueAnalysisDTO result = revenueAnalysisService.calculateRevenueImpact(clockTemplateId);

		// Assert
		assertNotNull(result);
		assertTrue(result.getRevenueByAvailType().containsKey("Prime"));
		assertTrue(result.getRevenueByAvailType().containsKey("Secondary"));
		// Prime should have 2 breaks worth of revenue
		BigDecimal primeRevenue = result.getRevenueByAvailType().get("Prime");
		BigDecimal secondaryRevenue = result.getRevenueByAvailType().get("Secondary");
		assertNotNull(primeRevenue);
		assertNotNull(secondaryRevenue);
		// Prime should be approximately 2x secondary (2 breaks vs 1 break)
		assertTrue(primeRevenue.compareTo(secondaryRevenue) > 0);
	}

	@Test
	void calculateRevenueImpact_When_BreaksWithNullDuration_Expect_ZeroRevenueForNullBreaks() {
		// Arrange
		BreakStructureResponseDTO break1 = BreakStructureResponseDTO.builder()
				.id(UUID.randomUUID())
				.name("Break 1")
				.durationSeconds(60) // 1 minute
				.build();

		BreakStructureResponseDTO break2 = BreakStructureResponseDTO.builder()
				.id(UUID.randomUUID())
				.name("Break 2")
				.durationSeconds(null) // null duration
				.build();

		List<BreakStructureResponseDTO> breaks = List.of(break1, break2);

		when(clockTemplateRepository.findById(clockTemplateId)).thenReturn(Optional.of(clockTemplate));
		when(breakStructureService.getByClockTemplateId(clockTemplateId)).thenReturn(breaks);

		// Act
		RevenueAnalysisDTO result = revenueAnalysisService.calculateRevenueImpact(clockTemplateId);

		// Assert
		assertNotNull(result);
		assertEquals(1.0, result.getTotalInventoryMinutes(), 0.01); // Only break1 counts
		assertTrue(result.getPotentialRevenue().compareTo(BigDecimal.ZERO) > 0);
	}

	@Test
	void calculateRevenueImpact_When_BreaksWithNoAvailType_Expect_RevenueCalculated() {
		// Arrange
		BreakStructureResponseDTO break1 = BreakStructureResponseDTO.builder()
				.id(UUID.randomUUID())
				.name("Break 1")
				.durationSeconds(60) // 1 minute
				.availTypeName(null) // No avail type
				.build();

		List<BreakStructureResponseDTO> breaks = List.of(break1);

		when(clockTemplateRepository.findById(clockTemplateId)).thenReturn(Optional.of(clockTemplate));
		when(breakStructureService.getByClockTemplateId(clockTemplateId)).thenReturn(breaks);

		// Act
		RevenueAnalysisDTO result = revenueAnalysisService.calculateRevenueImpact(clockTemplateId);

		// Assert
		assertNotNull(result);
		assertTrue(result.getPotentialRevenue().compareTo(BigDecimal.ZERO) > 0);
		assertTrue(result.getRevenueByAvailType().isEmpty()); // No avail type aggregation
	}

}

