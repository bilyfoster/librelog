package com.onelpro.librelog.controllers;

import com.onelpro.librelog.dto.OrderLineRequestDTO;
import com.onelpro.librelog.dto.OrderLineResponseDTO;
import com.onelpro.librelog.services.OrderLineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for order line management endpoints.
 */
@RestController
@RequestMapping("/api/order-lines")
@Tag(name = "Order Lines", description = "Order line management endpoints")
public class OrderLineController {

	private static final Logger logger = LoggerFactory.getLogger(OrderLineController.class);

	private final OrderLineService orderLineService;

	public OrderLineController(OrderLineService orderLineService) {
		this.orderLineService = orderLineService;
	}

	@PostMapping
	@Operation(
			summary = "Create a new order line",
			description = "Creates a new order line (spot requirement) for an order"
	)
	@ApiResponse(responseCode = "201", description = "Order line created successfully")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	@ApiResponse(responseCode = "404", description = "Order or daypart not found")
	public ResponseEntity<OrderLineResponseDTO> create(@Valid @RequestBody OrderLineRequestDTO request) {
		logger.info("POST /api/order-lines - Creating order line for order: {}", request.getOrderId());
		OrderLineResponseDTO response = orderLineService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/{id}")
	@Operation(
			summary = "Get order line by ID",
			description = "Retrieves an order line by its UUID"
	)
	@ApiResponse(responseCode = "200", description = "Order line found")
	@ApiResponse(responseCode = "404", description = "Order line not found")
	public ResponseEntity<OrderLineResponseDTO> getById(@PathVariable UUID id) {
		logger.debug("GET /api/order-lines/{} - Fetching order line", id);
		OrderLineResponseDTO response = orderLineService.getById(id);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/order/{orderId}")
	@Operation(
			summary = "Get order lines by order ID",
			description = "Retrieves all order lines for a specific order"
	)
	@ApiResponse(responseCode = "200", description = "Order lines retrieved successfully")
	public ResponseEntity<List<OrderLineResponseDTO>> getByOrderId(@PathVariable UUID orderId) {
		logger.debug("GET /api/order-lines/order/{} - Fetching order lines", orderId);
		List<OrderLineResponseDTO> response = orderLineService.getByOrderId(orderId);
		return ResponseEntity.ok(response);
	}

	@PutMapping("/{id}")
	@Operation(
			summary = "Update order line",
			description = "Updates an existing order line"
	)
	@ApiResponse(responseCode = "200", description = "Order line updated successfully")
	@ApiResponse(responseCode = "404", description = "Order line not found")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	public ResponseEntity<OrderLineResponseDTO> update(
			@PathVariable UUID id,
			@Valid @RequestBody OrderLineRequestDTO request) {
		logger.info("PUT /api/order-lines/{} - Updating order line", id);
		OrderLineResponseDTO response = orderLineService.update(id, request);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{id}")
	@Operation(
			summary = "Delete order line",
			description = "Deletes an order line by its UUID"
	)
	@ApiResponse(responseCode = "204", description = "Order line deleted successfully")
	@ApiResponse(responseCode = "404", description = "Order line not found")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		logger.info("DELETE /api/order-lines/{} - Deleting order line", id);
		orderLineService.delete(id);
		return ResponseEntity.noContent().build();
	}

}

