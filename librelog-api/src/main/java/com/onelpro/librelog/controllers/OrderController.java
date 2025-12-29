package com.onelpro.librelog.controllers;

import com.onelpro.librelog.dto.OrderRequestDTO;
import com.onelpro.librelog.dto.OrderResponseDTO;
import com.onelpro.librelog.enums.OrderStatus;
import com.onelpro.librelog.services.OrderService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for order management endpoints.
 */
@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Order management endpoints")
public class OrderController {

	private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

	private final OrderService orderService;

	public OrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	@PostMapping
	@Operation(
			summary = "Create a new order",
			description = "Creates a new broadcast advertising order"
	)
	@ApiResponse(responseCode = "201", description = "Order created successfully")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	@ApiResponse(responseCode = "404", description = "Station not found")
	public ResponseEntity<OrderResponseDTO> create(@Valid @RequestBody OrderRequestDTO request) {
		logger.info("POST /api/orders - Creating order for advertiser: {}", request.getAdvertiserName());
		OrderResponseDTO response = orderService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/{id}")
	@Operation(
			summary = "Get order by ID",
			description = "Retrieves an order by its UUID"
	)
	@ApiResponse(responseCode = "200", description = "Order found")
	@ApiResponse(responseCode = "404", description = "Order not found")
	public ResponseEntity<OrderResponseDTO> getById(@PathVariable UUID id) {
		logger.debug("GET /api/orders/{} - Fetching order", id);
		OrderResponseDTO response = orderService.getById(id);
		return ResponseEntity.ok(response);
	}

	@GetMapping
	@Operation(
			summary = "Get all orders",
			description = "Retrieves all orders, optionally filtered by station ID or status"
	)
	@ApiResponse(responseCode = "200", description = "Orders retrieved successfully")
	public ResponseEntity<List<OrderResponseDTO>> getAll(
			@RequestParam(required = false) UUID stationId,
			@RequestParam(required = false) OrderStatus status) {
		logger.debug("GET /api/orders - Fetching orders");
		List<OrderResponseDTO> response;
		if (stationId != null) {
			response = orderService.getByStationId(stationId);
			if (status != null) {
				response = response.stream()
						.filter(order -> order.getStatus() == status)
						.toList();
			}
		} else if (status != null) {
			response = orderService.getByStatus(status);
		} else {
			response = orderService.getAll();
		}
		return ResponseEntity.ok(response);
	}

	@PutMapping("/{id}")
	@Operation(
			summary = "Update order",
			description = "Updates an existing order"
	)
	@ApiResponse(responseCode = "200", description = "Order updated successfully")
	@ApiResponse(responseCode = "404", description = "Order not found")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	public ResponseEntity<OrderResponseDTO> update(
			@PathVariable UUID id,
			@Valid @RequestBody OrderRequestDTO request) {
		logger.info("PUT /api/orders/{} - Updating order", id);
		OrderResponseDTO response = orderService.update(id, request);
		return ResponseEntity.ok(response);
	}

	@PatchMapping("/{id}/status")
	@Operation(
			summary = "Update order status",
			description = "Updates the status of an existing order"
	)
	@ApiResponse(responseCode = "200", description = "Order status updated successfully")
	@ApiResponse(responseCode = "404", description = "Order not found")
	public ResponseEntity<OrderResponseDTO> updateStatus(
			@PathVariable UUID id,
			@RequestParam OrderStatus status) {
		logger.info("PATCH /api/orders/{}/status - Updating order status to: {}", id, status);
		OrderResponseDTO response = orderService.updateStatus(id, status);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{id}")
	@Operation(
			summary = "Delete order",
			description = "Deletes an order by its UUID"
	)
	@ApiResponse(responseCode = "204", description = "Order deleted successfully")
	@ApiResponse(responseCode = "404", description = "Order not found")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		logger.info("DELETE /api/orders/{} - Deleting order", id);
		orderService.delete(id);
		return ResponseEntity.noContent().build();
	}

}

