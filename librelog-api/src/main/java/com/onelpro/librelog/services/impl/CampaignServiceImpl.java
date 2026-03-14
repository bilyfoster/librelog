package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.CampaignRequestDTO;
import com.onelpro.librelog.dto.CampaignResponseDTO;
import com.onelpro.librelog.enums.CampaignStatus;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.models.Advertiser;
import com.onelpro.librelog.models.Campaign;
import com.onelpro.librelog.models.Order;
import com.onelpro.librelog.models.Station;
import com.onelpro.librelog.repositories.AdvertiserRepository;
import com.onelpro.librelog.repositories.CampaignRepository;
import com.onelpro.librelog.repositories.OrderRepository;
import com.onelpro.librelog.repositories.StationRepository;
import com.onelpro.librelog.services.CampaignService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of campaign management service.
 */
@Service
public class CampaignServiceImpl implements CampaignService {

	private static final Logger logger = LoggerFactory.getLogger(CampaignServiceImpl.class);

	private final CampaignRepository campaignRepository;
	private final StationRepository stationRepository;
	private final AdvertiserRepository advertiserRepository;
	private final OrderRepository orderRepository;

	public CampaignServiceImpl(CampaignRepository campaignRepository,
	                           StationRepository stationRepository,
	                           AdvertiserRepository advertiserRepository,
	                           OrderRepository orderRepository) {
		this.campaignRepository = campaignRepository;
		this.stationRepository = stationRepository;
		this.advertiserRepository = advertiserRepository;
		this.orderRepository = orderRepository;
	}

	@Override
	@Transactional
	public CampaignResponseDTO create(CampaignRequestDTO request) {
		logger.info("Creating campaign: {}", request.getName());

		Station station = stationRepository.findById(request.getStationId())
				.orElseThrow(() -> new NotFoundException("Station not found with id: " + request.getStationId()));

		Advertiser advertiser = null;
		if (request.getAdvertiserId() != null) {
			advertiser = advertiserRepository.findById(request.getAdvertiserId())
					.orElseThrow(() -> new NotFoundException("Advertiser not found with id: " + request.getAdvertiserId()));
		}

		Order order = null;
		if (request.getOrderId() != null) {
			order = orderRepository.findById(request.getOrderId())
					.orElseThrow(() -> new NotFoundException("Order not found with id: " + request.getOrderId()));
		}

		String advertiserName = request.getAdvertiserName();
		if (advertiserName == null && advertiser != null) {
			advertiserName = advertiser.getName();
		}
		if (advertiserName == null && order != null) {
			advertiserName = order.getAdvertiserName();
		}
		if (advertiserName == null) {
			advertiserName = "Unknown";
		}

		Campaign campaign = Campaign.builder()
				.name(request.getName())
				.station(station)
				.advertiser(advertiser)
				.order(order)
				.advertiserName(advertiserName)
				.startDate(request.getStartDate())
				.endDate(request.getEndDate())
				.status(request.getStatus() != null ? request.getStatus() : CampaignStatus.DRAFT)
				.totalSpots(request.getTotalSpots() != null ? request.getTotalSpots() : 0)
				.spotsScheduled(0)
				.spotsAired(0)
				.budget(request.getBudget())
				.notes(request.getNotes())
				.copyInstructions(request.getCopyInstructions())
				.salesRepName(request.getSalesRepName())
				.priority(request.getPriority())
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();

		Campaign saved = campaignRepository.save(campaign);
		logger.info("Campaign created successfully with ID: {}", saved.getId());

		return mapToResponseDTO(saved);
	}

	@Override
	@Transactional(readOnly = true)
	public CampaignResponseDTO getById(UUID id) {
		logger.debug("Fetching campaign with ID: {}", id);
		Campaign campaign = campaignRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Campaign not found with id: " + id));
		return mapToResponseDTO(campaign);
	}

	@Override
	@Transactional(readOnly = true)
	public List<CampaignResponseDTO> getByStationId(UUID stationId) {
		logger.debug("Fetching campaigns for station ID: {}", stationId);
		return campaignRepository.findByStationId(stationId).stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public List<CampaignResponseDTO> getAll() {
		logger.debug("Fetching all campaigns");
		return campaignRepository.findAll().stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public CampaignResponseDTO update(UUID id, CampaignRequestDTO request) {
		logger.info("Updating campaign with ID: {}", id);

		Campaign campaign = campaignRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Campaign not found with id: " + id));

		if (request.getStationId() != null && !campaign.getStation().getId().equals(request.getStationId())) {
			Station station = stationRepository.findById(request.getStationId())
					.orElseThrow(() -> new NotFoundException("Station not found with id: " + request.getStationId()));
			campaign.setStation(station);
		}

		if (request.getAdvertiserId() != null) {
			Advertiser advertiser = advertiserRepository.findById(request.getAdvertiserId())
					.orElseThrow(() -> new NotFoundException("Advertiser not found with id: " + request.getAdvertiserId()));
			campaign.setAdvertiser(advertiser);
		}

		if (request.getAdvertiserName() != null) {
			campaign.setAdvertiserName(request.getAdvertiserName());
		}

		campaign.setName(request.getName());
		campaign.setStartDate(request.getStartDate());
		campaign.setEndDate(request.getEndDate());
		campaign.setBudget(request.getBudget());
		campaign.setNotes(request.getNotes());
		campaign.setCopyInstructions(request.getCopyInstructions());
		campaign.setSalesRepName(request.getSalesRepName());
		campaign.setPriority(request.getPriority());
		campaign.setUpdatedAt(LocalDateTime.now());

		Campaign updated = campaignRepository.save(campaign);
		logger.info("Campaign updated successfully with ID: {}", updated.getId());

		return mapToResponseDTO(updated);
	}

	@Override
	@Transactional
	public void delete(UUID id) {
		logger.info("Deleting campaign with ID: {}", id);
		if (!campaignRepository.existsById(id)) {
			throw new NotFoundException("Campaign not found with id: " + id);
		}
		campaignRepository.deleteById(id);
		logger.info("Campaign deleted successfully with ID: {}", id);
	}

	@Override
	@Transactional
	public CampaignResponseDTO updateStatus(UUID id, String status) {
		logger.info("Updating campaign status for ID: {} to {}", id, status);

		Campaign campaign = campaignRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Campaign not found with id: " + id));

		CampaignStatus newStatus = CampaignStatus.valueOf(status.toUpperCase());
		campaign.setStatus(newStatus);
		campaign.setUpdatedAt(LocalDateTime.now());

		Campaign updated = campaignRepository.save(campaign);
		return mapToResponseDTO(updated);
	}

	@Override
	@Transactional(readOnly = true)
	public List<CampaignResponseDTO> getActiveCampaigns(UUID stationId) {
		logger.debug("Fetching active campaigns for station ID: {}", stationId);
		return campaignRepository.findByStationIdAndStatus(stationId, CampaignStatus.ACTIVE).stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public CampaignResponseDTO createFromOrder(UUID orderId) {
		logger.info("Creating campaign from order ID: {}", orderId);

		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new NotFoundException("Order not found with id: " + orderId));

		// Check if campaign already exists for this order
		if (campaignRepository.existsByOrderId(orderId)) {
			throw new IllegalStateException("Campaign already exists for order: " + orderId);
		}

		Campaign campaign = Campaign.builder()
				.name(order.getAdvertiserName() + " - " + order.getOrderNumber())
				.station(order.getStation())
				.order(order)
				.advertiserName(order.getAdvertiserName())
				.salesRepName(order.getSalesRepName())
				.startDate(order.getStartDate())
				.endDate(order.getEndDate())
				.totalSpots(order.getTotalSpots() != null ? order.getTotalSpots() : 0)
				.spotsScheduled(0)
				.spotsAired(0)
				.status(CampaignStatus.DRAFT)
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();

		Campaign saved = campaignRepository.save(campaign);
		logger.info("Campaign created from order successfully with ID: {}", saved.getId());

		return mapToResponseDTO(saved);
	}

	@Override
	@Transactional(readOnly = true)
	public List<CampaignResponseDTO> getByOrderId(UUID orderId) {
		logger.debug("Fetching campaigns for order ID: {}", orderId);
		return campaignRepository.findByOrderId(orderId).stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	private CampaignResponseDTO mapToResponseDTO(Campaign campaign) {
		return CampaignResponseDTO.builder()
				.id(campaign.getId())
				.name(campaign.getName())
				.stationId(campaign.getStation() != null ? campaign.getStation().getId() : null)
				.stationName(campaign.getStation() != null ? campaign.getStation().getName() : null)
				.advertiserId(campaign.getAdvertiser() != null ? campaign.getAdvertiser().getId() : null)
				.advertiserName(campaign.getAdvertiserName())
				.orderId(campaign.getOrder() != null ? campaign.getOrder().getId() : campaign.getOrderId())
				.startDate(campaign.getStartDate())
				.endDate(campaign.getEndDate())
				.status(campaign.getStatus())
				.totalSpots(campaign.getTotalSpots())
				.spotsScheduled(campaign.getSpotsScheduled())
				.spotsAired(campaign.getSpotsAired())
				.budget(campaign.getBudget())
				.notes(campaign.getNotes())
				.copyInstructions(campaign.getCopyInstructions())
				.salesRepName(campaign.getSalesRepName())
				.priority(campaign.getPriority())
				.createdAt(campaign.getCreatedAt())
				.updatedAt(campaign.getUpdatedAt())
				.build();
	}

}
