package com.onelpro.librelog.controllers;

import com.onelpro.librelog.dto.InvoiceRequestDTO;
import com.onelpro.librelog.dto.InvoiceResponseDTO;
import com.onelpro.librelog.models.Invoice.InvoiceStatus;
import com.onelpro.librelog.services.InvoiceService;
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
 * REST controller for invoice management endpoints.
 */
@RestController
@RequestMapping("/api/invoices")
@Tag(name = "Billing - Invoices", description = "Invoice generation and management")
public class InvoiceController {

	private static final Logger logger = LoggerFactory.getLogger(InvoiceController.class);

	private final InvoiceService invoiceService;

	public InvoiceController(InvoiceService invoiceService) {
		this.invoiceService = invoiceService;
	}

	@PostMapping
	@Operation(summary = "Create invoice", description = "Creates a new invoice")
	@ApiResponse(responseCode = "201", description = "Invoice created successfully")
	public ResponseEntity<InvoiceResponseDTO> create(@Valid @RequestBody InvoiceRequestDTO request) {
		logger.info("POST /api/invoices - Creating invoice for campaign: {}", request.getCampaignId());
		InvoiceResponseDTO response = invoiceService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PostMapping("/generate")
	@Operation(summary = "Generate invoice from campaign", description = "Auto-generates invoice from aired spots")
	@ApiResponse(responseCode = "201", description = "Invoice generated successfully")
	public ResponseEntity<InvoiceResponseDTO> generateFromCampaign(
			@RequestParam UUID campaignId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate invoiceDate,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate) {
		logger.info("POST /api/invoices/generate - Generating invoice for campaign: {}", campaignId);
		InvoiceResponseDTO response = invoiceService.generateFromCampaign(campaignId, invoiceDate, dueDate);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get invoice by ID", description = "Retrieves an invoice by its UUID")
	@ApiResponse(responseCode = "200", description = "Invoice found")
	@ApiResponse(responseCode = "404", description = "Invoice not found")
	public ResponseEntity<InvoiceResponseDTO> getById(@PathVariable UUID id) {
		logger.debug("GET /api/invoices/{} - Fetching invoice", id);
		InvoiceResponseDTO response = invoiceService.getById(id);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/number/{invoiceNumber}")
	@Operation(summary = "Get invoice by number", description = "Retrieves an invoice by its invoice number")
	@ApiResponse(responseCode = "200", description = "Invoice found")
	@ApiResponse(responseCode = "404", description = "Invoice not found")
	public ResponseEntity<InvoiceResponseDTO> getByNumber(@PathVariable String invoiceNumber) {
		logger.debug("GET /api/invoices/number/{} - Fetching invoice", invoiceNumber);
		InvoiceResponseDTO response = invoiceService.getByNumber(invoiceNumber);
		return ResponseEntity.ok(response);
	}

	@GetMapping
	@Operation(summary = "Get all invoices", description = "Retrieves all invoices, optionally filtered")
	@ApiResponse(responseCode = "200", description = "Invoices retrieved successfully")
	public ResponseEntity<List<InvoiceResponseDTO>> getAll(
			@RequestParam(required = false) UUID advertiserId,
			@RequestParam(required = false) UUID campaignId,
			@RequestParam(required = false) InvoiceStatus status) {
		logger.debug("GET /api/invoices - Fetching invoices");
		List<InvoiceResponseDTO> response;
		if (advertiserId != null) {
			response = invoiceService.getByAdvertiserId(advertiserId);
		} else if (campaignId != null) {
			response = invoiceService.getByCampaignId(campaignId);
		} else if (status != null) {
			response = invoiceService.getByStatus(status);
		} else {
			// Get all - might want to limit this in production
			response = invoiceService.getByStatus(InvoiceStatus.SENT);
			response.addAll(invoiceService.getByStatus(InvoiceStatus.DRAFT));
			response.addAll(invoiceService.getByStatus(InvoiceStatus.PARTIALLY_PAID));
		}
		return ResponseEntity.ok(response);
	}

	@GetMapping("/overdue")
	@Operation(summary = "Get overdue invoices", description = "Retrieves all overdue invoices")
	@ApiResponse(responseCode = "200", description = "Overdue invoices retrieved")
	public ResponseEntity<List<InvoiceResponseDTO>> getOverdue() {
		logger.debug("GET /api/invoices/overdue - Fetching overdue invoices");
		List<InvoiceResponseDTO> response = invoiceService.getOverdue();
		return ResponseEntity.ok(response);
	}

	@GetMapping("/advertiser/{advertiserId}/balance")
	@Operation(summary = "Get outstanding balance", description = "Gets the total outstanding balance for an advertiser")
	@ApiResponse(responseCode = "200", description = "Balance retrieved")
	public ResponseEntity<Map<String, Object>> getOutstandingBalance(@PathVariable UUID advertiserId) {
		logger.debug("GET /api/invoices/advertiser/{}/balance", advertiserId);
		BigDecimal balance = invoiceService.getOutstandingBalance(advertiserId);
		return ResponseEntity.ok(Map.of("advertiserId", advertiserId, "outstandingBalance", balance));
	}

	@PatchMapping("/{id}/status")
	@Operation(summary = "Update invoice status", description = "Updates the status of an invoice")
	@ApiResponse(responseCode = "200", description = "Status updated")
	public ResponseEntity<InvoiceResponseDTO> updateStatus(
			@PathVariable UUID id,
			@RequestParam InvoiceStatus status) {
		logger.info("PATCH /api/invoices/{}/status - Updating to {}", id, status);
		InvoiceResponseDTO response = invoiceService.updateStatus(id, status);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete invoice", description = "Deletes an invoice")
	@ApiResponse(responseCode = "204", description = "Invoice deleted")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		logger.info("DELETE /api/invoices/{} - Deleting invoice", id);
		invoiceService.delete(id);
		return ResponseEntity.noContent().build();
	}

}
