package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.SpotRequestDTO;
import com.onelpro.librelog.dto.SpotResponseDTO;
import com.onelpro.librelog.enums.SpotStatus;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.models.Campaign;
import com.onelpro.librelog.models.Spot;
import com.onelpro.librelog.models.Station;
import com.onelpro.librelog.repositories.CampaignRepository;
import com.onelpro.librelog.repositories.SpotRepository;
import com.onelpro.librelog.repositories.StationRepository;
import com.onelpro.librelog.services.SpotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of spot management service.
 */
@Service
public class SpotServiceImpl implements SpotService {

	private static final Logger logger = LoggerFactory.getLogger(SpotServiceImpl.class);

	private final SpotRepository spotRepository;
	private final CampaignRepository campaignRepository;
	private final StationRepository stationRepository;

	public SpotServiceImpl(SpotRepository spotRepository,
	                       CampaignRepository campaignRepository,
	                       StationRepository stationRepository) {
		this.spotRepository = spotRepository;
		this.campaignRepository = campaignRepository;
		this.stationRepository = stationRepository;
	}

	@Override
	@Transactional
	public SpotResponseDTO create(SpotRequestDTO request) {
		logger.info("Creating spot for campaign ID: {}", request.getCampaignId());

		Campaign campaign = campaignRepository.findById(request.getCampaignId())
				.orElseThrow(() -> new NotFoundException("Campaign not found with id: " + request.getCampaignId()));

		Station station = stationRepository.findById(request.getStationId())
				.orElseThrow(() -> new NotFoundException("Station not found with id: " + request.getStationId()));

		Spot spot = Spot.builder()
				.campaign(campaign)
				.station(station)
				.scheduledDate(request.getScheduledDate())
				.scheduledTime(request.getScheduledTime())
				.spotLength(request.getSpotLength())
				.status(request.getStatus() != null ? request.getStatus() : SpotStatus.SCHEDULED)
				.daypart(request.getDaypart())
				.breakName(request.getBreakName())
				.breakPosition(request.getBreakPosition())
				.assetId(request.getAssetId())
				.assetName(request.getAssetName())
				.notes(request.getNotes())
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();

		Spot saved = spotRepository.save(spot);

		// Update campaign spot count
		updateCampaignSpotCounts(campaign);

		logger.info("Spot created successfully with ID: {}", saved.getId());
		return mapToResponseDTO(saved);
	}

	@Override
	@Transactional(readOnly = true)
	public SpotResponseDTO getById(UUID id) {
		logger.debug("Fetching spot with ID: {}", id);
		Spot spot = spotRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Spot not found with id: " + id));
		return mapToResponseDTO(spot);
	}

	@Override
	@Transactional(readOnly = true)
	public List<SpotResponseDTO> getByCampaignId(UUID campaignId) {
		logger.debug("Fetching spots for campaign ID: {}", campaignId);
		return spotRepository.findByCampaignId(campaignId).stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public List<SpotResponseDTO> getByStationAndDate(UUID stationId, LocalDate date) {
		logger.debug("Fetching spots for station ID: {} on date: {}", stationId, date);
		return spotRepository.findByStationIdAndScheduledDate(stationId, date).stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public List<SpotResponseDTO> getByStationAndDateRange(UUID stationId, LocalDate startDate, LocalDate endDate) {
		logger.debug("Fetching spots for station ID: {} from {} to {}", stationId, startDate, endDate);
		return spotRepository.findByStationIdAndScheduledDateBetween(stationId, startDate, endDate).stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public SpotResponseDTO update(UUID id, SpotRequestDTO request) {
		logger.info("Updating spot with ID: {}", id);

		Spot spot = spotRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Spot not found with id: " + id));

		if (request.getStationId() != null && !spot.getStation().getId().equals(request.getStationId())) {
			Station station = stationRepository.findById(request.getStationId())
					.orElseThrow(() -> new NotFoundException("Station not found with id: " + request.getStationId()));
			spot.setStation(station);
		}

		spot.setScheduledDate(request.getScheduledDate());
		spot.setScheduledTime(request.getScheduledTime());
		spot.setSpotLength(request.getSpotLength());
		spot.setDaypart(request.getDaypart());
		spot.setBreakName(request.getBreakName());
		spot.setBreakPosition(request.getBreakPosition());
		spot.setAssetId(request.getAssetId());
		spot.setAssetName(request.getAssetName());
		spot.setNotes(request.getNotes());
		spot.setUpdatedAt(LocalDateTime.now());

		Spot updated = spotRepository.save(spot);
		logger.info("Spot updated successfully with ID: {}", updated.getId());

		return mapToResponseDTO(updated);
	}

	@Override
	@Transactional
	public void delete(UUID id) {
		logger.info("Deleting spot with ID: {}", id);

		Spot spot = spotRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Spot not found with id: " + id));

		UUID campaignId = spot.getCampaign().getId();
		spotRepository.deleteById(id);

		// Update campaign spot count
		campaignRepository.findById(campaignId).ifPresent(this::updateCampaignSpotCounts);

		logger.info("Spot deleted successfully with ID: {}", id);
	}

	@Override
	@Transactional
	public SpotResponseDTO updateStatus(UUID id, String status) {
		logger.info("Updating spot status for ID: {} to {}", id, status);

		Spot spot = spotRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Spot not found with id: " + id));

		SpotStatus newStatus = SpotStatus.valueOf(status.toUpperCase());
		spot.setStatus(newStatus);
		spot.setUpdatedAt(LocalDateTime.now());

		Spot updated = spotRepository.save(spot);

		// Update campaign counts if status affects scheduling
		updateCampaignSpotCounts(spot.getCampaign());

		return mapToResponseDTO(updated);
	}

	@Override
	@Transactional
	public SpotResponseDTO markAsAired(UUID id) {
		logger.info("Marking spot as aired with ID: {}", id);

		Spot spot = spotRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Spot not found with id: " + id));

		spot.setStatus(SpotStatus.AIRED);
		spot.setActualAirTime(LocalDateTime.now());
		spot.setUpdatedAt(LocalDateTime.now());

		Spot updated = spotRepository.save(spot);

		// Update campaign counts
		updateCampaignSpotCounts(spot.getCampaign());

		return mapToResponseDTO(updated);
	}

	@Override
	@Transactional
	public SpotResponseDTO createMakegood(UUID missedSpotId, SpotRequestDTO request) {
		logger.info("Creating makegood for missed spot ID: {}", missedSpotId);

		Spot missedSpot = spotRepository.findById(missedSpotId)
				.orElseThrow(() -> new NotFoundException("Missed spot not found with id: " + missedSpotId));

		// Mark original spot as missed
		missedSpot.setStatus(SpotStatus.MISSED);
		missedSpot.setUpdatedAt(LocalDateTime.now());
		spotRepository.save(missedSpot);

		// Create makegood spot
		Spot makegood = Spot.builder()
				.campaign(missedSpot.getCampaign())
				.station(missedSpot.getStation())
				.scheduledDate(request.getScheduledDate())
				.scheduledTime(request.getScheduledTime())
				.spotLength(request.getSpotLength() != null ? request.getSpotLength() : missedSpot.getSpotLength())
				.status(SpotStatus.MAKEGOOD_SCHEDULED)
				.daypart(request.getDaypart() != null ? request.getDaypart() : missedSpot.getDaypart())
				.breakName(request.getBreakName() != null ? request.getBreakName() : missedSpot.getBreakName())
				.assetId(request.getAssetId() != null ? request.getAssetId() : missedSpot.getAssetId())
				.assetName(request.getAssetName() != null ? request.getAssetName() : missedSpot.getAssetName())
				.makegoodOfId(missedSpotId)
				.notes("Makegood for missed spot on " + missedSpot.getScheduledDate() + " at " + missedSpot.getScheduledTime())
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();

		Spot saved = spotRepository.save(makegood);

		// Update campaign counts
		updateCampaignSpotCounts(missedSpot.getCampaign());

		logger.info("Makegood created successfully with ID: {}", saved.getId());
		return mapToResponseDTO(saved);
	}

	private void updateCampaignSpotCounts(Campaign campaign) {
		long scheduled = spotRepository.countByCampaignIdAndStatus(campaign.getId(), SpotStatus.SCHEDULED);
		long aired = spotRepository.countByCampaignIdAndStatus(campaign.getId(), SpotStatus.AIRED);

		campaign.setSpotsScheduled((int) scheduled);
		campaign.setSpotsAired((int) aired);
		campaign.setUpdatedAt(LocalDateTime.now());
		campaignRepository.save(campaign);
	}

	private SpotResponseDTO mapToResponseDTO(Spot spot) {
		return SpotResponseDTO.builder()
				.id(spot.getId())
				.campaignId(spot.getCampaign() != null ? spot.getCampaign().getId() : null)
				.campaignName(spot.getCampaign() != null ? spot.getCampaign().getName() : null)
				.stationId(spot.getStation() != null ? spot.getStation().getId() : null)
				.stationName(spot.getStation() != null ? spot.getStation().getName() : null)
				.scheduledDate(spot.getScheduledDate())
				.scheduledTime(spot.getScheduledTime())
				.spotLength(spot.getSpotLength())
				.status(spot.getStatus())
				.actualAirTime(spot.getActualAirTime())
				.daypart(spot.getDaypart())
				.breakName(spot.getBreakName())
				.breakPosition(spot.getBreakPosition())
				.assetId(spot.getAssetId())
				.assetName(spot.getAssetName())
				.notes(spot.getNotes())
				.createdAt(spot.getCreatedAt())
				.updatedAt(spot.getUpdatedAt())
				.build();
	}

}
