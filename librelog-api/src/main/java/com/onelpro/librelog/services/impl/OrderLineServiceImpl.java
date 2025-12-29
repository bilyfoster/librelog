package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.OrderLineRequestDTO;
import com.onelpro.librelog.dto.OrderLineResponseDTO;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.models.Daypart;
import com.onelpro.librelog.models.Order;
import com.onelpro.librelog.models.OrderLine;
import com.onelpro.librelog.repositories.DaypartRepository;
import com.onelpro.librelog.repositories.OrderLineRepository;
import com.onelpro.librelog.repositories.OrderRepository;
import com.onelpro.librelog.services.OrderLineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of order line service.
 */
@Service
public class OrderLineServiceImpl implements OrderLineService {

	private static final Logger logger = LoggerFactory.getLogger(OrderLineServiceImpl.class);

	private final OrderLineRepository orderLineRepository;
	private final OrderRepository orderRepository;
	private final DaypartRepository daypartRepository;

	public OrderLineServiceImpl(
			OrderLineRepository orderLineRepository,
			OrderRepository orderRepository,
			DaypartRepository daypartRepository) {
		this.orderLineRepository = orderLineRepository;
		this.orderRepository = orderRepository;
		this.daypartRepository = daypartRepository;
	}

	@Override
	@Transactional
	public OrderLineResponseDTO create(OrderLineRequestDTO request) {
		logger.info("Creating order line for order: {}", request.getOrderId());

		Order order = orderRepository.findById(request.getOrderId())
				.orElseThrow(() -> {
					logger.warn("Order not found with ID: {}", request.getOrderId());
					return new NotFoundException("Order not found with ID: " + request.getOrderId());
				});

		Daypart daypart = null;
		if (request.getDaypartId() != null) {
			daypart = daypartRepository.findById(request.getDaypartId())
					.orElseThrow(() -> {
						logger.warn("Daypart not found with ID: {}", request.getDaypartId());
						return new NotFoundException("Daypart not found with ID: " + request.getDaypartId());
					});
		}

		OrderLine orderLine = OrderLine.builder()
				.order(order)
				.daypart(daypart)
				.spotLengthSeconds(request.getSpotLengthSeconds())
				.quantity(request.getQuantity())
				.rate(request.getRate())
				.startDate(request.getStartDate())
				.endDate(request.getEndDate())
				.startTime(request.getStartTime())
				.endTime(request.getEndTime())
				.notes(request.getNotes())
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();

		orderLine = orderLineRepository.save(orderLine);
		logger.info("Order line created successfully with ID: {}", orderLine.getId());

		return mapToResponseDTO(orderLine);
	}

	@Override
	public OrderLineResponseDTO getById(UUID id) {
		logger.debug("Fetching order line with ID: {}", id);
		OrderLine orderLine = orderLineRepository.findById(id)
				.orElseThrow(() -> {
					logger.warn("Order line not found with ID: {}", id);
					return new NotFoundException("Order line not found with ID: " + id);
				});
		return mapToResponseDTO(orderLine);
	}

	@Override
	public List<OrderLineResponseDTO> getByOrderId(UUID orderId) {
		logger.debug("Fetching order lines for order: {}", orderId);
		return orderLineRepository.findByOrderId(orderId).stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public OrderLineResponseDTO update(UUID id, OrderLineRequestDTO request) {
		logger.info("Updating order line with ID: {}", id);
		OrderLine orderLine = orderLineRepository.findById(id)
				.orElseThrow(() -> {
					logger.warn("Order line not found with ID: {}", id);
					return new NotFoundException("Order line not found with ID: " + id);
				});

		Order order = orderRepository.findById(request.getOrderId())
				.orElseThrow(() -> {
					logger.warn("Order not found with ID: {}", request.getOrderId());
					return new NotFoundException("Order not found with ID: " + request.getOrderId());
				});

		Daypart daypart = null;
		if (request.getDaypartId() != null) {
			daypart = daypartRepository.findById(request.getDaypartId())
					.orElseThrow(() -> {
						logger.warn("Daypart not found with ID: {}", request.getDaypartId());
						return new NotFoundException("Daypart not found with ID: " + request.getDaypartId());
					});
		}

		orderLine.setOrder(order);
		orderLine.setDaypart(daypart);
		orderLine.setSpotLengthSeconds(request.getSpotLengthSeconds());
		orderLine.setQuantity(request.getQuantity());
		orderLine.setRate(request.getRate());
		orderLine.setStartDate(request.getStartDate());
		orderLine.setEndDate(request.getEndDate());
		orderLine.setStartTime(request.getStartTime());
		orderLine.setEndTime(request.getEndTime());
		orderLine.setNotes(request.getNotes());
		orderLine.setUpdatedAt(LocalDateTime.now());

		orderLine = orderLineRepository.save(orderLine);
		logger.info("Order line updated successfully with ID: {}", orderLine.getId());

		return mapToResponseDTO(orderLine);
	}

	@Override
	@Transactional
	public void delete(UUID id) {
		logger.info("Deleting order line with ID: {}", id);
		if (!orderLineRepository.existsById(id)) {
			logger.warn("Order line not found with ID: {}", id);
			throw new NotFoundException("Order line not found with ID: " + id);
		}
		orderLineRepository.deleteById(id);
		logger.info("Order line deleted successfully with ID: {}", id);
	}

	private OrderLineResponseDTO mapToResponseDTO(OrderLine orderLine) {
		return OrderLineResponseDTO.builder()
				.id(orderLine.getId())
				.orderId(orderLine.getOrder().getId())
				.orderNumber(orderLine.getOrder().getOrderNumber())
				.daypartId(orderLine.getDaypart() != null ? orderLine.getDaypart().getId() : null)
				.daypartName(orderLine.getDaypart() != null ? orderLine.getDaypart().getName() : null)
				.spotLengthSeconds(orderLine.getSpotLengthSeconds())
				.quantity(orderLine.getQuantity())
				.rate(orderLine.getRate())
				.startDate(orderLine.getStartDate())
				.endDate(orderLine.getEndDate())
				.startTime(orderLine.getStartTime())
				.endTime(orderLine.getEndTime())
				.notes(orderLine.getNotes())
				.createdAt(orderLine.getCreatedAt())
				.updatedAt(orderLine.getUpdatedAt())
				.build();
	}

}

