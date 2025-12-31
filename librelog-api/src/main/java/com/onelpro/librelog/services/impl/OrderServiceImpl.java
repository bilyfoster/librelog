package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.OrderRequestDTO;
import com.onelpro.librelog.dto.OrderResponseDTO;
import com.onelpro.librelog.enums.ActionType;
import com.onelpro.librelog.enums.ModuleType;
import com.onelpro.librelog.enums.OrderStatus;
import com.onelpro.librelog.exceptions.BadRequestException;
import com.onelpro.librelog.exceptions.ForbiddenException;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.models.Order;
import com.onelpro.librelog.models.Station;
import com.onelpro.librelog.repositories.OrderRepository;
import com.onelpro.librelog.repositories.StationRepository;
import com.onelpro.librelog.services.OrderService;
import com.onelpro.librelog.services.PermissionService;
import com.onelpro.librelog.utils.SecurityContextUtils;
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
 * Implementation of order service.
 */
@Service
public class OrderServiceImpl implements OrderService {

	private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

	private final OrderRepository orderRepository;
	private final StationRepository stationRepository;
	private final PermissionService permissionService;

	public OrderServiceImpl(
			OrderRepository orderRepository,
			StationRepository stationRepository,
			PermissionService permissionService) {
		this.orderRepository = orderRepository;
		this.stationRepository = stationRepository;
		this.permissionService = permissionService;
	}

	@Override
	@Transactional
	public OrderResponseDTO create(OrderRequestDTO request) {
		logger.info("Creating order for advertiser: {} on station: {}", request.getAdvertiserName(), request.getStationId());

		UUID userId = SecurityContextUtils.getCurrentUserId();
		if (userId == null) {
			logger.warn("Order creation failed: user not authenticated");
			throw new ForbiddenException("User not authenticated");
		}

		// Check permission to create orders for this station
		if (!permissionService.hasPermission(userId, request.getStationId(), ModuleType.ORDERS, ActionType.CREATE)) {
			logger.warn("Order creation failed: insufficient permissions for user {} on station {}", userId, request.getStationId());
			throw new ForbiddenException("Insufficient permissions to create orders for this station");
		}

		if (request.getStartDate().isAfter(request.getEndDate())) {
			logger.warn("Order creation failed: invalid date range");
			throw new BadRequestException("Start date must be before or equal to end date");
		}

		Station station = stationRepository.findById(request.getStationId())
				.orElseThrow(() -> {
					logger.warn("Station not found with ID: {}", request.getStationId());
					return new NotFoundException("Station not found with ID: " + request.getStationId());
				});

		String orderNumber = generateOrderNumber(station);

		Order order = Order.builder()
				.orderNumber(orderNumber)
				.station(station)
				.advertiserName(request.getAdvertiserName())
				.agencyName(request.getAgencyName())
				.salesRepName(request.getSalesRepName())
				.status(OrderStatus.DRAFT)
				.startDate(request.getStartDate())
				.endDate(request.getEndDate())
				.totalSpots(request.getTotalSpots())
				.totalAmount(request.getTotalAmount())
				.notes(request.getNotes())
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();

		order = orderRepository.save(order);
		logger.info("Order created successfully with ID: {} and order number: {}", order.getId(), order.getOrderNumber());

		return mapToResponseDTO(order);
	}

	@Override
	public OrderResponseDTO getById(UUID id) {
		logger.debug("Fetching order with ID: {}", id);
		Order order = orderRepository.findById(id)
				.orElseThrow(() -> {
					logger.warn("Order not found with ID: {}", id);
					return new NotFoundException("Order not found with ID: " + id);
				});

		UUID userId = SecurityContextUtils.getCurrentUserId();
		if (userId == null) {
			logger.warn("Order retrieval failed: user not authenticated");
			throw new ForbiddenException("User not authenticated");
		}

		// Check permission to view orders for this station
		if (!permissionService.hasPermission(userId, order.getStation().getId(), ModuleType.ORDERS, ActionType.VIEW)) {
			logger.warn("Order retrieval failed: insufficient permissions for user {} on station {}", userId, order.getStation().getId());
			throw new ForbiddenException("Insufficient permissions to view this order");
		}

		return mapToResponseDTO(order);
	}

	@Override
	public List<OrderResponseDTO> getAll() {
		logger.debug("Fetching all orders");
		UUID userId = SecurityContextUtils.getCurrentUserId();
		if (userId == null) {
			logger.warn("Order retrieval failed: user not authenticated");
			throw new ForbiddenException("User not authenticated");
		}

		// Get user's station assignments and filter orders
		List<UUID> userStationIds = permissionService.getUserStations(userId);
		if (userStationIds.isEmpty()) {
			logger.debug("User {} has no station assignments, returning empty list", userId);
			return List.of();
		}

		return orderRepository.findAll().stream()
				.filter(order -> userStationIds.contains(order.getStation().getId()))
				.filter(order -> permissionService.hasPermission(userId, order.getStation().getId(), ModuleType.ORDERS, ActionType.VIEW))
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	public List<OrderResponseDTO> getByStationId(UUID stationId) {
		logger.debug("Fetching orders for station: {}", stationId);
		UUID userId = SecurityContextUtils.getCurrentUserId();
		if (userId == null) {
			logger.warn("Order retrieval failed: user not authenticated");
			throw new ForbiddenException("User not authenticated");
		}

		// Check permission to view orders for this station
		if (!permissionService.hasPermission(userId, stationId, ModuleType.ORDERS, ActionType.VIEW)) {
			logger.warn("Order retrieval failed: insufficient permissions for user {} on station {}", userId, stationId);
			throw new ForbiddenException("Insufficient permissions to view orders for this station");
		}

		return orderRepository.findByStationId(stationId).stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	public List<OrderResponseDTO> getByStatus(OrderStatus status) {
		logger.debug("Fetching orders with status: {}", status);
		UUID userId = SecurityContextUtils.getCurrentUserId();
		if (userId == null) {
			logger.warn("Order retrieval failed: user not authenticated");
			throw new ForbiddenException("User not authenticated");
		}

		// Get user's station assignments and filter orders
		List<UUID> userStationIds = permissionService.getUserStations(userId);
		if (userStationIds.isEmpty()) {
			logger.debug("User {} has no station assignments, returning empty list", userId);
			return List.of();
		}

		return orderRepository.findByStatus(status).stream()
				.filter(order -> userStationIds.contains(order.getStation().getId()))
				.filter(order -> permissionService.hasPermission(userId, order.getStation().getId(), ModuleType.ORDERS, ActionType.VIEW))
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public OrderResponseDTO update(UUID id, OrderRequestDTO request) {
		logger.info("Updating order with ID: {}", id);
		UUID userId = SecurityContextUtils.getCurrentUserId();
		if (userId == null) {
			logger.warn("Order update failed: user not authenticated");
			throw new ForbiddenException("User not authenticated");
		}

		Order order = orderRepository.findById(id)
				.orElseThrow(() -> {
					logger.warn("Order not found with ID: {}", id);
					return new NotFoundException("Order not found with ID: " + id);
				});

		// Check permission to edit orders for the order's current station
		if (!permissionService.hasPermission(userId, order.getStation().getId(), ModuleType.ORDERS, ActionType.EDIT)) {
			logger.warn("Order update failed: insufficient permissions for user {} on station {}", userId, order.getStation().getId());
			throw new ForbiddenException("Insufficient permissions to edit this order");
		}

		// If station is being changed, check permission for new station
		if (!order.getStation().getId().equals(request.getStationId())) {
			if (!permissionService.hasPermission(userId, request.getStationId(), ModuleType.ORDERS, ActionType.EDIT)) {
				logger.warn("Order update failed: insufficient permissions for user {} on new station {}", userId, request.getStationId());
				throw new ForbiddenException("Insufficient permissions to move order to this station");
			}
		}

		if (request.getStartDate().isAfter(request.getEndDate())) {
			logger.warn("Order update failed: invalid date range");
			throw new BadRequestException("Start date must be before or equal to end date");
		}

		Station station = stationRepository.findById(request.getStationId())
				.orElseThrow(() -> {
					logger.warn("Station not found with ID: {}", request.getStationId());
					return new NotFoundException("Station not found with ID: " + request.getStationId());
				});

		order.setStation(station);
		order.setAdvertiserName(request.getAdvertiserName());
		order.setAgencyName(request.getAgencyName());
		order.setSalesRepName(request.getSalesRepName());
		order.setStartDate(request.getStartDate());
		order.setEndDate(request.getEndDate());
		order.setTotalSpots(request.getTotalSpots());
		order.setTotalAmount(request.getTotalAmount());
		order.setNotes(request.getNotes());
		order.setUpdatedAt(LocalDateTime.now());

		order = orderRepository.save(order);
		logger.info("Order updated successfully with ID: {}", order.getId());

		return mapToResponseDTO(order);
	}

	@Override
	@Transactional
	public OrderResponseDTO updateStatus(UUID id, OrderStatus status) {
		logger.info("Updating order status with ID: {} to status: {}", id, status);
		UUID userId = SecurityContextUtils.getCurrentUserId();
		if (userId == null) {
			logger.warn("Order status update failed: user not authenticated");
			throw new ForbiddenException("User not authenticated");
		}

		Order order = orderRepository.findById(id)
				.orElseThrow(() -> {
					logger.warn("Order not found with ID: {}", id);
					return new NotFoundException("Order not found with ID: " + id);
				});

		// Check permission to edit orders for this station
		if (!permissionService.hasPermission(userId, order.getStation().getId(), ModuleType.ORDERS, ActionType.EDIT)) {
			logger.warn("Order status update failed: insufficient permissions for user {} on station {}", userId, order.getStation().getId());
			throw new ForbiddenException("Insufficient permissions to update this order");
		}

		order.setStatus(status);
		order.setUpdatedAt(LocalDateTime.now());

		order = orderRepository.save(order);
		logger.info("Order status updated successfully with ID: {} to status: {}", order.getId(), status);

		return mapToResponseDTO(order);
	}

	@Override
	@Transactional
	public void delete(UUID id) {
		logger.info("Deleting order with ID: {}", id);
		UUID userId = SecurityContextUtils.getCurrentUserId();
		if (userId == null) {
			logger.warn("Order deletion failed: user not authenticated");
			throw new ForbiddenException("User not authenticated");
		}

		Order order = orderRepository.findById(id)
				.orElseThrow(() -> {
					logger.warn("Order not found with ID: {}", id);
					return new NotFoundException("Order not found with ID: " + id);
				});

		// Check permission to delete orders for this station
		if (!permissionService.hasPermission(userId, order.getStation().getId(), ModuleType.ORDERS, ActionType.DELETE)) {
			logger.warn("Order deletion failed: insufficient permissions for user {} on station {}", userId, order.getStation().getId());
			throw new ForbiddenException("Insufficient permissions to delete this order");
		}

		orderRepository.deleteById(id);
		logger.info("Order deleted successfully with ID: {}", id);
	}

	private String generateOrderNumber(Station station) {
		String prefix = station.getCallSign().toUpperCase().replaceAll("[^A-Z0-9]", "");
		String year = String.valueOf(LocalDate.now().getYear());
		String sequence = String.format("%06d", orderRepository.count() + 1);
		return prefix + "-" + year + "-" + sequence;
	}

	private OrderResponseDTO mapToResponseDTO(Order order) {
		return OrderResponseDTO.builder()
				.id(order.getId())
				.orderNumber(order.getOrderNumber())
				.stationId(order.getStation().getId())
				.stationCallSign(order.getStation().getCallSign())
				.stationName(order.getStation().getName())
				.advertiserName(order.getAdvertiserName())
				.agencyName(order.getAgencyName())
				.salesRepName(order.getSalesRepName())
				.status(order.getStatus())
				.startDate(order.getStartDate())
				.endDate(order.getEndDate())
				.totalSpots(order.getTotalSpots())
				.totalAmount(order.getTotalAmount())
				.notes(order.getNotes())
				.createdAt(order.getCreatedAt())
				.updatedAt(order.getUpdatedAt())
				.build();
	}

}

