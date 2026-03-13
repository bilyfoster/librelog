package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.SpotCopyRequestDTO;
import com.onelpro.librelog.dto.SpotCopyResponseDTO;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.models.Campaign;
import com.onelpro.librelog.models.SpotCopy;
import com.onelpro.librelog.repositories.CampaignRepository;
import com.onelpro.librelog.repositories.SpotCopyRepository;
import com.onelpro.librelog.services.SpotCopyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of spot copy management service.
 */
@Service
public class SpotCopyServiceImpl implements SpotCopyService {

	private static final Logger logger = LoggerFactory.getLogger(SpotCopyServiceImpl.class);

	private final SpotCopyRepository spotCopyRepository;
	private final CampaignRepository campaignRepository;

	public SpotCopyServiceImpl(SpotCopyRepository spotCopyRepository,
	                           CampaignRepository campaignRepository) {
		this.spotCopyRepository = spotCopyRepository;
		this.campaignRepository = campaignRepository;
	}

	@Override
	@Transactional
	public SpotCopyResponseDTO create(SpotCopyRequestDTO request) {
		logger.info("Creating spot copy for campaign ID: {}", request.getCampaignId());

		Campaign campaign = campaignRepository.findById(request.getCampaignId())
				.orElseThrow(() -> new NotFoundException("Campaign not found with id: " + request.getCampaignId()));

		// Get next version number
		Integer maxVersion = spotCopyRepository.findMaxVersionNumberByCampaignId(request.getCampaignId())
				.orElse(0);

		SpotCopy spotCopy = SpotCopy.builder()
				.campaign(campaign)
				.versionNumber(maxVersion + 1)
				.title(request.getTitle())
				.scriptText(request.getScriptText())
				.instructions(request.getInstructions())
				.durationSeconds(request.getDurationSeconds())
				.status(request.getStatus() != null ? request.getStatus() : "DRAFT")
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();

		SpotCopy saved = spotCopyRepository.save(spotCopy);
		logger.info("Spot copy created successfully with ID: {} (version {})", saved.getId(), saved.getVersionNumber());

		return mapToResponseDTO(saved);
	}

	@Override
	@Transactional(readOnly = true)
	public SpotCopyResponseDTO getById(UUID id) {
		logger.debug("Fetching spot copy with ID: {}", id);
		SpotCopy spotCopy = spotCopyRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Spot copy not found with id: " + id));
		return mapToResponseDTO(spotCopy);
	}

	@Override
	@Transactional(readOnly = true)
	public List<SpotCopyResponseDTO> getByCampaignId(UUID campaignId) {
		logger.debug("Fetching spot copies for campaign ID: {}", campaignId);
		return spotCopyRepository.findByCampaignIdOrderByVersionNumberDesc(campaignId).stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public SpotCopyResponseDTO getLatestApprovedByCampaignId(UUID campaignId) {
		logger.debug("Fetching latest approved copy for campaign ID: {}", campaignId);
		SpotCopy spotCopy = spotCopyRepository.findFirstByCampaignIdAndStatusOrderByVersionNumberDesc(campaignId, "APPROVED")
				.orElseThrow(() -> new NotFoundException("No approved copy found for campaign: " + campaignId));
		return mapToResponseDTO(spotCopy);
	}

	@Override
	@Transactional
	public SpotCopyResponseDTO update(UUID id, SpotCopyRequestDTO request) {
		logger.info("Updating spot copy with ID: {}", id);

		SpotCopy spotCopy = spotCopyRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Spot copy not found with id: " + id));

		// Only allow updates if not approved
		if ("APPROVED".equals(spotCopy.getStatus())) {
			throw new IllegalStateException("Cannot update approved copy. Create a new version instead.");
		}

		spotCopy.setTitle(request.getTitle());
		spotCopy.setScriptText(request.getScriptText());
		spotCopy.setInstructions(request.getInstructions());
		spotCopy.setDurationSeconds(request.getDurationSeconds());
		spotCopy.setUpdatedAt(LocalDateTime.now());

		SpotCopy updated = spotCopyRepository.save(spotCopy);
		logger.info("Spot copy updated successfully with ID: {}", updated.getId());

		return mapToResponseDTO(updated);
	}

	@Override
	@Transactional
	public void delete(UUID id) {
		logger.info("Deleting spot copy with ID: {}", id);
		if (!spotCopyRepository.existsById(id)) {
			throw new NotFoundException("Spot copy not found with id: " + id);
		}
		spotCopyRepository.deleteById(id);
		logger.info("Spot copy deleted successfully with ID: {}", id);
	}

	@Override
	@Transactional
	public SpotCopyResponseDTO approve(UUID id) {
		logger.info("Approving spot copy with ID: {}", id);

		SpotCopy spotCopy = spotCopyRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Spot copy not found with id: " + id));

		spotCopy.setStatus("APPROVED");
		spotCopy.setApprovedAt(LocalDateTime.now());
		spotCopy.setRejectionReason(null);
		spotCopy.setUpdatedAt(LocalDateTime.now());

		SpotCopy updated = spotCopyRepository.save(spotCopy);
		return mapToResponseDTO(updated);
	}

	@Override
	@Transactional
	public SpotCopyResponseDTO reject(UUID id, String reason) {
		logger.info("Rejecting spot copy with ID: {}", id);

		SpotCopy spotCopy = spotCopyRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Spot copy not found with id: " + id));

		spotCopy.setStatus("REJECTED");
		spotCopy.setRejectionReason(reason);
		spotCopy.setUpdatedAt(LocalDateTime.now());

		SpotCopy updated = spotCopyRepository.save(spotCopy);
		return mapToResponseDTO(updated);
	}

	@Override
	@Transactional(readOnly = true)
	public List<SpotCopyResponseDTO> getPendingApproval() {
		logger.debug("Fetching spot copies pending approval");
		return spotCopyRepository.findByStatus("PENDING_APPROVAL").stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	private SpotCopyResponseDTO mapToResponseDTO(SpotCopy spotCopy) {
		return SpotCopyResponseDTO.builder()
				.id(spotCopy.getId())
				.campaignId(spotCopy.getCampaign() != null ? spotCopy.getCampaign().getId() : null)
				.campaignName(spotCopy.getCampaign() != null ? spotCopy.getCampaign().getName() : null)
				.versionNumber(spotCopy.getVersionNumber())
				.title(spotCopy.getTitle())
				.scriptText(spotCopy.getScriptText())
				.instructions(spotCopy.getInstructions())
				.durationSeconds(spotCopy.getDurationSeconds())
				.status(spotCopy.getStatus())
				.createdById(spotCopy.getCreatedBy() != null ? spotCopy.getCreatedBy().getId() : null)
				.createdByName(spotCopy.getCreatedBy() != null ? spotCopy.getCreatedBy().getEmail() : null)
				.approvedById(spotCopy.getApprovedBy() != null ? spotCopy.getApprovedBy().getId() : null)
				.approvedByName(spotCopy.getApprovedBy() != null ? spotCopy.getApprovedBy().getEmail() : null)
				.approvedAt(spotCopy.getApprovedAt())
				.rejectionReason(spotCopy.getRejectionReason())
				.createdAt(spotCopy.getCreatedAt())
				.updatedAt(spotCopy.getUpdatedAt())
				.build();
	}

}
