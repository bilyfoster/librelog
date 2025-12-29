package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.OrderRequestDTO;
import com.onelpro.librelog.dto.OrderResponseDTO;
import com.onelpro.librelog.enums.OrderStatus;
import com.onelpro.librelog.exceptions.BadRequestException;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.models.Order;
import com.onelpro.librelog.models.Station;
import com.onelpro.librelog.repositories.OrderRepository;
import com.onelpro.librelog.repositories.StationRepository;
import com.onelpro.librelog.services.OrderService;
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

	public OrderServiceImpl(
			OrderRepository orderRepository,
			StationRepository stationRepository) {
		this.orderRepository = orderRepository;
		this.stationRepository = stationRepository;
	}

	@Override
	@Transactional
	public OrderResponseDTO create(OrderRequestDTO request) {
		logger.info("Creating order for advertiser: {} on station: {}", request.getAdvertiserName(), request.getStationId());

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
		return mapToResponseDTO(order);
	}

	@Override
	public List<OrderResponseDTO> getAll() {
		logger.debug("Fetching all orders");
		return orderRepository.findAll().stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	public List<OrderResponseDTO> getByStationId(UUID stationId) {
		logger.debug("Fetching orders for station: {}", stationId);
		return orderRepository.findByStationId(stationId).stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	public List<OrderResponseDTO> getByStatus(OrderStatus status) {
		logger.debug("Fetching orders with status: {}", status);
		return orderRepository.findByStatus(status).stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public OrderResponseDTO update(UUID id, OrderRequestDTO request) {
		logger.info("Updating order with ID: {}", id);
		Order order = orderRepository.findById(id)
				.orElseThrow(() -> {
					logger.warn("Order not found with ID: {}", id);
					return new NotFoundException("Order not found with ID: " + id);
				});

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
		Order order = orderRepository.findById(id)
				.orElseThrow(() -> {
					logger.warn("Order not found with ID: {}", id);
					return new NotFoundException("Order not found with ID: " + id);
				});

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
		if (!orderRepository.existsById(id)) {
			logger.warn("Order not found with ID: {}", id);
			throw new NotFoundException("Order not found with ID: " + id);
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

