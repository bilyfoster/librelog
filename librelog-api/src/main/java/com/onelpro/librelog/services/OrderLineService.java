package com.onelpro.librelog.services;

import com.onelpro.librelog.dto.OrderLineRequestDTO;
import com.onelpro.librelog.dto.OrderLineResponseDTO;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for order line operations.
 */
public interface OrderLineService {

	OrderLineResponseDTO create(OrderLineRequestDTO request);

	OrderLineResponseDTO getById(UUID id);

	List<OrderLineResponseDTO> getByOrderId(UUID orderId);

	OrderLineResponseDTO update(UUID id, OrderLineRequestDTO request);

	void delete(UUID id);

}

