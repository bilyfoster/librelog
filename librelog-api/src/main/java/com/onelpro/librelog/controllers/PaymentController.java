package com.onelpro.librelog.controllers;

import com.onelpro.librelog.dto.PaymentRequestDTO;
import com.onelpro.librelog.dto.PaymentResponseDTO;
import com.onelpro.librelog.services.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for payment management endpoints.
 */
@RestController
@RequestMapping("/api/payments")
@Tag(name = "Billing - Payments", description = "Payment recording and management")
public class PaymentController {

	private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

	private final PaymentService paymentService;

	public PaymentController(PaymentService paymentService) {
		this.paymentService = paymentService;
	}

	@PostMapping
	@Operation(summary = "Record payment", description = "Records a payment for an invoice")
	@ApiResponse(responseCode = "201", description = "Payment recorded successfully")
	public ResponseEntity<PaymentResponseDTO> create(@Valid @RequestBody PaymentRequestDTO request) {
		logger.info("POST /api/payments - Recording payment for invoice: {}", request.getInvoiceId());
		PaymentResponseDTO response = paymentService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get payment by ID", description = "Retrieves a payment by its UUID")
	@ApiResponse(responseCode = "200", description = "Payment found")
	@ApiResponse(responseCode = "404", description = "Payment not found")
	public ResponseEntity<PaymentResponseDTO> getById(@PathVariable UUID id) {
		logger.debug("GET /api/payments/{} - Fetching payment", id);
		PaymentResponseDTO response = paymentService.getById(id);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/invoice/{invoiceId}")
	@Operation(summary = "Get payments by invoice", description = "Retrieves all payments for an invoice")
	@ApiResponse(responseCode = "200", description = "Payments retrieved")
	public ResponseEntity<List<PaymentResponseDTO>> getByInvoiceId(@PathVariable UUID invoiceId) {
		logger.debug("GET /api/payments/invoice/{} - Fetching payments", invoiceId);
		List<PaymentResponseDTO> response = paymentService.getByInvoiceId(invoiceId);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/total")
	@Operation(summary = "Get total payments for invoice", description = "Gets the total amount paid for an invoice")
	@ApiResponse(responseCode = "200", description = "Total retrieved")
	public ResponseEntity<Map<String, Object>> getTotalForInvoice(@RequestParam UUID invoiceId) {
		logger.debug("GET /api/payments/total - Getting total for invoice: {}", invoiceId);
		BigDecimal total = paymentService.getTotalPaymentsForInvoice(invoiceId);
		return ResponseEntity.ok(Map.of("invoiceId", invoiceId, "totalPayments", total));
	}

	@GetMapping("/range")
	@Operation(summary = "Get payments by date range", description = "Retrieves payments within a date range")
	@ApiResponse(responseCode = "200", description = "Payments retrieved")
	public ResponseEntity<List<PaymentResponseDTO>> getByDateRange(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
		logger.debug("GET /api/payments/range - From {} to {}", startDate, endDate);
		List<PaymentResponseDTO> response = paymentService.getByDateRange(startDate, endDate);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete payment", description = "Deletes a payment and updates invoice balance")
	@ApiResponse(responseCode = "204", description = "Payment deleted")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		logger.info("DELETE /api/payments/{} - Deleting payment", id);
		paymentService.delete(id);
		return ResponseEntity.noContent().build();
	}

}
