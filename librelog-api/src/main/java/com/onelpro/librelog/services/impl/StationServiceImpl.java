package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.StationResponseDTO;
import com.onelpro.librelog.dto.StationRequestDTO;
import com.onelpro.librelog.exceptions.BadRequestException;
import com.onelpro.librelog.exceptions.ForbiddenException;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.models.Cluster;
import com.onelpro.librelog.models.Market;
import com.onelpro.librelog.models.Organization;
import com.onelpro.librelog.models.Station;
import com.onelpro.librelog.repositories.ClusterRepository;
import com.onelpro.librelog.repositories.MarketRepository;
import com.onelpro.librelog.repositories.OrganizationRepository;
import com.onelpro.librelog.repositories.StationRepository;
import com.onelpro.librelog.services.PermissionService;
import com.onelpro.librelog.services.StationService;
import com.onelpro.librelog.utils.SecurityContextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of station service.
 */
@Service
public class StationServiceImpl implements StationService {

	private static final Logger logger = LoggerFactory.getLogger(StationServiceImpl.class);

	private final StationRepository stationRepository;
	private final OrganizationRepository organizationRepository;
	private final MarketRepository marketRepository;
	private final ClusterRepository clusterRepository;
	private final PermissionService permissionService;

	public StationServiceImpl(
			StationRepository stationRepository,
			OrganizationRepository organizationRepository,
			MarketRepository marketRepository,
			ClusterRepository clusterRepository,
			PermissionService permissionService) {
		this.stationRepository = stationRepository;
		this.organizationRepository = organizationRepository;
		this.marketRepository = marketRepository;
		this.clusterRepository = clusterRepository;
		this.permissionService = permissionService;
	}

	@Override
	@Transactional
	public StationResponseDTO create(StationRequestDTO request) {
		logger.info("Creating station with call sign: {}", request.getCallSign());

		if (stationRepository.existsByCallSign(request.getCallSign())) {
			logger.warn("Station creation failed: call sign already exists - {}", request.getCallSign());
			throw new BadRequestException("Station with call sign " + request.getCallSign() + " already exists");
		}

		Organization organization = organizationRepository.findById(request.getOrganizationId())
				.orElseThrow(() -> {
					logger.warn("Organization not found with ID: {}", request.getOrganizationId());
					return new NotFoundException("Organization not found with ID: " + request.getOrganizationId());
				});

		Market market = null;
		if (request.getMarketId() != null) {
			market = marketRepository.findById(request.getMarketId())
					.orElseThrow(() -> {
						logger.warn("Market not found with ID: {}", request.getMarketId());
						return new NotFoundException("Market not found with ID: " + request.getMarketId());
					});
		}

		Cluster cluster = null;
		if (request.getClusterId() != null) {
			cluster = clusterRepository.findById(request.getClusterId())
					.orElseThrow(() -> {
						logger.warn("Cluster not found with ID: {}", request.getClusterId());
						return new NotFoundException("Cluster not found with ID: " + request.getClusterId());
					});
		}

		Station station = Station.builder()
				.organization(organization)
				.market(market)
				.cluster(cluster)
				.callSign(request.getCallSign())
				.name(request.getName())
				.frequency(request.getFrequency())
				.stationType(request.getStationType())
				.isActive(request.getIsActive())
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();

		station = stationRepository.save(station);
		logger.info("Station created successfully with ID: {}", station.getId());

		return mapToResponseDTO(station);
	}

	@Override
	public StationResponseDTO getById(UUID id) {
		logger.debug("Fetching station with ID: {}", id);
		UUID userId = SecurityContextUtils.getCurrentUserId();
		if (userId == null) {
			logger.warn("Station retrieval failed: user not authenticated");
			throw new ForbiddenException("User not authenticated");
		}

		Station station = stationRepository.findById(id)
				.orElseThrow(() -> {
					logger.warn("Station not found with ID: {}", id);
					return new NotFoundException("Station not found with ID: " + id);
				});

		// Check if user has access to this station
		if (!permissionService.canAccessStation(userId, id)) {
			logger.warn("Station retrieval failed: user {} does not have access to station {}", userId, id);
			throw new ForbiddenException("Insufficient permissions to access this station");
		}

		return mapToResponseDTO(station);
	}

	@Override
	public List<StationResponseDTO> getAll() {
		logger.debug("Fetching all stations");
		UUID userId = SecurityContextUtils.getCurrentUserId();
		if (userId == null) {
			logger.warn("Station retrieval failed: user not authenticated");
			throw new ForbiddenException("User not authenticated");
		}

		// Get user's station assignments and filter stations
		List<UUID> userStationIds = permissionService.getUserStations(userId);
		if (userStationIds.isEmpty()) {
			logger.debug("User {} has no station assignments, returning empty list", userId);
			return List.of();
		}

		return stationRepository.findAll().stream()
				.filter(station -> userStationIds.contains(station.getId()))
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public StationResponseDTO update(UUID id, StationRequestDTO request) {
		logger.info("Updating station with ID: {}", id);
		Station station = stationRepository.findById(id)
				.orElseThrow(() -> {
					logger.warn("Station not found with ID: {}", id);
					return new NotFoundException("Station not found with ID: " + id);
				});

		if (!station.getCallSign().equals(request.getCallSign()) &&
				stationRepository.existsByCallSign(request.getCallSign())) {
			logger.warn("Station update failed: call sign already exists - {}", request.getCallSign());
			throw new BadRequestException("Station with call sign " + request.getCallSign() + " already exists");
		}

		Organization organization = organizationRepository.findById(request.getOrganizationId())
				.orElseThrow(() -> {
					logger.warn("Organization not found with ID: {}", request.getOrganizationId());
					return new NotFoundException("Organization not found with ID: " + request.getOrganizationId());
				});

		Market market = null;
		if (request.getMarketId() != null) {
			market = marketRepository.findById(request.getMarketId())
					.orElseThrow(() -> {
						logger.warn("Market not found with ID: {}", request.getMarketId());
						return new NotFoundException("Market not found with ID: " + request.getMarketId());
					});
		}

		Cluster cluster = null;
		if (request.getClusterId() != null) {
			cluster = clusterRepository.findById(request.getClusterId())
					.orElseThrow(() -> {
						logger.warn("Cluster not found with ID: {}", request.getClusterId());
						return new NotFoundException("Cluster not found with ID: " + request.getClusterId());
					});
		}

		station.setOrganization(organization);
		station.setMarket(market);
		station.setCluster(cluster);
		station.setCallSign(request.getCallSign());
		station.setName(request.getName());
		station.setFrequency(request.getFrequency());
		station.setStationType(request.getStationType());
		station.setUpdatedAt(LocalDateTime.now());

		station = stationRepository.save(station);
		logger.info("Station updated successfully with ID: {}", station.getId());

		return mapToResponseDTO(station);
	}

	@Override
	@Transactional
	public void delete(UUID id) {
		logger.info("Deleting station with ID: {}", id);
		if (!stationRepository.existsById(id)) {
			logger.warn("Station not found with ID: {}", id);
			throw new NotFoundException("Station not found with ID: " + id);
		}
		stationRepository.deleteById(id);
		logger.info("Station deleted successfully with ID: {}", id);
	}

	private StationResponseDTO mapToResponseDTO(Station station) {
		String organizationName = station.getOrganization() != null ? station.getOrganization().getName() : null;
		String marketName = station.getMarket() != null ? station.getMarket().getName() : null;
		String clusterName = station.getCluster() != null ? station.getCluster().getName() : null;

		return StationResponseDTO.builder()
				.id(station.getId())
				.organizationId(station.getOrganization() != null ? station.getOrganization().getId() : null)
				.organizationName(organizationName)
				.marketId(station.getMarket() != null ? station.getMarket().getId() : null)
				.marketName(marketName)
				.clusterId(station.getCluster() != null ? station.getCluster().getId() : null)
				.clusterName(clusterName)
				.callSign(station.getCallSign())
				.name(station.getName())
				.frequency(station.getFrequency())
				.stationType(station.getStationType())
				.isActive(station.getIsActive())
				.createdAt(station.getCreatedAt())
				.updatedAt(station.getUpdatedAt())
				.build();
	}

}

