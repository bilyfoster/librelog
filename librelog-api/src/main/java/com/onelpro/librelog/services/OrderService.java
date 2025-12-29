package com.onelpro.librelog.services;

import com.onelpro.librelog.dto.OrderRequestDTO;
import com.onelpro.librelog.dto.OrderResponseDTO;
import com.onelpro.librelog.enums.OrderStatus;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for order operations.
 */
public interface OrderService {

	OrderResponseDTO create(OrderRequestDTO request);

	OrderResponseDTO getById(UUID id);

	List<OrderResponseDTO> getAll();

	List<OrderResponseDTO> getByStationId(UUID stationId);

	List<OrderResponseDTO> getByStatus(OrderStatus status);

	OrderResponseDTO update(UUID id, OrderRequestDTO request);

	OrderResponseDTO updateStatus(UUID id, OrderStatus status);

	void delete(UUID id);

}

