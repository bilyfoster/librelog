package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.FixedAssetRequestDTO;
import com.onelpro.librelog.dto.FixedAssetResponseDTO;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.models.ClockTemplate;
import com.onelpro.librelog.models.FixedAsset;
import com.onelpro.librelog.repositories.ClockTemplateRepository;
import com.onelpro.librelog.repositories.FixedAssetRepository;
import com.onelpro.librelog.services.FixedAssetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of fixed asset service.
 */
@Service
public class FixedAssetServiceImpl implements FixedAssetService {

	private static final Logger logger = LoggerFactory.getLogger(FixedAssetServiceImpl.class);

	private final FixedAssetRepository fixedAssetRepository;
	private final ClockTemplateRepository clockTemplateRepository;

	public FixedAssetServiceImpl(
			FixedAssetRepository fixedAssetRepository,
			ClockTemplateRepository clockTemplateRepository) {
		this.fixedAssetRepository = fixedAssetRepository;
		this.clockTemplateRepository = clockTemplateRepository;
	}

	@Override
	@Transactional
	public FixedAssetResponseDTO create(FixedAssetRequestDTO request) {
		logger.info("Creating fixed asset with name: {} for clock template: {}", request.getName(), request.getClockTemplateId());

		// Validate clock template exists
		ClockTemplate clockTemplate = clockTemplateRepository.findById(request.getClockTemplateId())
				.orElseThrow(() -> {
					logger.warn("Clock template not found with ID: {}", request.getClockTemplateId());
					return new NotFoundException("Clock template not found with ID: " + request.getClockTemplateId());
				});

		// Fixed assets default to HARD_START if not specified
		com.onelpro.librelog.enums.TimingType timingType = request.getTimingType();
		if (timingType == null) {
			timingType = com.onelpro.librelog.enums.TimingType.HARD_START;
		}

		FixedAsset fixedAsset = FixedAsset.builder()
				.clockTemplate(clockTemplate)
				.name(request.getName())
				.assetType(request.getAssetType())
				.startTime(request.getStartTime())
				.assetIdentifier(request.getAssetIdentifier())
				.timingType(timingType)
				.musicCategory(request.getMusicCategory())
				.showSegmentName(request.getShowSegmentName())
				// LibreTime fields
				.libreTimeCartId(request.getLibreTimeCartId())
				.cueInMs(request.getCueInMs())
				.cueOutMs(request.getCueOutMs())
				.fadeInMs(request.getFadeInMs())
				.fadeOutMs(request.getFadeOutMs())
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();

		fixedAsset = fixedAssetRepository.save(fixedAsset);
		logger.info("Fixed asset created successfully with ID: {}", fixedAsset.getId());

		return mapToResponseDTO(fixedAsset);
	}

	@Override
	public FixedAssetResponseDTO getById(UUID id) {
		logger.debug("Fetching fixed asset with ID: {}", id);
		FixedAsset fixedAsset = fixedAssetRepository.findById(id)
				.orElseThrow(() -> {
					logger.warn("Fixed asset not found with ID: {}", id);
					return new NotFoundException("Fixed asset not found with ID: " + id);
				});
		return mapToResponseDTO(fixedAsset);
	}

	@Override
	public List<FixedAssetResponseDTO> getByClockTemplateId(UUID clockTemplateId) {
		logger.debug("Fetching fixed assets for clock template: {}", clockTemplateId);
		
		// Validate clock template exists
		if (!clockTemplateRepository.existsById(clockTemplateId)) {
			logger.warn("Clock template not found with ID: {}", clockTemplateId);
			throw new NotFoundException("Clock template not found with ID: " + clockTemplateId);
		}

		return fixedAssetRepository.findByClockTemplateId(clockTemplateId).stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public FixedAssetResponseDTO update(UUID id, FixedAssetRequestDTO request) {
		logger.info("Updating fixed asset with ID: {}", id);
		FixedAsset fixedAsset = fixedAssetRepository.findById(id)
				.orElseThrow(() -> {
					logger.warn("Fixed asset not found with ID: {}", id);
					return new NotFoundException("Fixed asset not found with ID: " + id);
				});

		// Validate clock template exists if changed
		if (!fixedAsset.getClockTemplate().getId().equals(request.getClockTemplateId())) {
			ClockTemplate clockTemplate = clockTemplateRepository.findById(request.getClockTemplateId())
					.orElseThrow(() -> {
						logger.warn("Clock template not found with ID: {}", request.getClockTemplateId());
						return new NotFoundException("Clock template not found with ID: " + request.getClockTemplateId());
					});
			fixedAsset.setClockTemplate(clockTemplate);
		}

		// Fixed assets default to HARD_START if not specified
		com.onelpro.librelog.enums.TimingType timingType = request.getTimingType();
		if (timingType == null) {
			timingType = com.onelpro.librelog.enums.TimingType.HARD_START;
		}

		fixedAsset.setName(request.getName());
		fixedAsset.setAssetType(request.getAssetType());
		fixedAsset.setStartTime(request.getStartTime());
		fixedAsset.setAssetIdentifier(request.getAssetIdentifier());
		fixedAsset.setTimingType(timingType);
		fixedAsset.setMusicCategory(request.getMusicCategory());
		fixedAsset.setShowSegmentName(request.getShowSegmentName());
		// LibreTime fields
		fixedAsset.setLibreTimeCartId(request.getLibreTimeCartId());
		fixedAsset.setCueInMs(request.getCueInMs());
		fixedAsset.setCueOutMs(request.getCueOutMs());
		fixedAsset.setFadeInMs(request.getFadeInMs());
		fixedAsset.setFadeOutMs(request.getFadeOutMs());
		fixedAsset.setUpdatedAt(LocalDateTime.now());

		fixedAsset = fixedAssetRepository.save(fixedAsset);
		logger.info("Fixed asset updated successfully with ID: {}", fixedAsset.getId());

		return mapToResponseDTO(fixedAsset);
	}

	@Override
	@Transactional
	public void delete(UUID id) {
		logger.info("Deleting fixed asset with ID: {}", id);
		if (!fixedAssetRepository.existsById(id)) {
			logger.warn("Fixed asset not found with ID: {}", id);
			throw new NotFoundException("Fixed asset not found with ID: " + id);
		}
		fixedAssetRepository.deleteById(id);
		logger.info("Fixed asset deleted successfully with ID: {}", id);
	}

	private FixedAssetResponseDTO mapToResponseDTO(FixedAsset fixedAsset) {
		ClockTemplate clockTemplate = fixedAsset.getClockTemplate();

		return FixedAssetResponseDTO.builder()
				.id(fixedAsset.getId())
				.clockTemplateId(clockTemplate.getId())
				.clockTemplateName(clockTemplate.getName())
				.name(fixedAsset.getName())
				.assetType(fixedAsset.getAssetType())
				.startTime(fixedAsset.getStartTime())
				.assetIdentifier(fixedAsset.getAssetIdentifier())
				.timingType(fixedAsset.getTimingType())
				.musicCategory(fixedAsset.getMusicCategory())
				.showSegmentName(fixedAsset.getShowSegmentName())
				// LibreTime fields
				.libreTimeCartId(fixedAsset.getLibreTimeCartId())
				.cueInMs(fixedAsset.getCueInMs())
				.cueOutMs(fixedAsset.getCueOutMs())
				.fadeInMs(fixedAsset.getFadeInMs())
				.fadeOutMs(fixedAsset.getFadeOutMs())
				.createdAt(fixedAsset.getCreatedAt())
				.updatedAt(fixedAsset.getUpdatedAt())
				.build();
	}

}

