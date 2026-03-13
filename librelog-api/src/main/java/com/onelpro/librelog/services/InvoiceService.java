package com.onelpro.librelog.services;

import com.onelpro.librelog.dto.InvoiceRequestDTO;
import com.onelpro.librelog.dto.InvoiceResponseDTO;
import com.onelpro.librelog.models.Invoice.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Service interface for invoice management.
 */
public interface InvoiceService {

	/**
	 * Create a new invoice.
	 */
	InvoiceResponseDTO create(InvoiceRequestDTO request);

	/**
	 * Get invoice by ID.
	 */
	InvoiceResponseDTO getById(UUID id);

	/**
	 * Get invoice by number.
	 */
	InvoiceResponseDTO getByNumber(String invoiceNumber);

	/**
	 * Get invoices by advertiser.
	 */
	List<InvoiceResponseDTO> getByAdvertiserId(UUID advertiserId);

	/**
	 * Get invoices by campaign.
	 */
	List<InvoiceResponseDTO> getByCampaignId(UUID campaignId);

	/**
	 * Get invoices by status.
	 */
	List<InvoiceResponseDTO> getByStatus(InvoiceStatus status);

	/**
	 * Get overdue invoices.
	 */
	List<InvoiceResponseDTO> getOverdue();

	/**
	 * Update invoice status.
	 */
	InvoiceResponseDTO updateStatus(UUID id, InvoiceStatus status);

	/**
	 * Generate invoice from campaign spots.
	 */
	InvoiceResponseDTO generateFromCampaign(UUID campaignId, LocalDate invoiceDate, LocalDate dueDate);

	/**
	 * Delete invoice.
	 */
	void delete(UUID id);

	/**
	 * Get outstanding balance for advertiser.
	 */
	BigDecimal getOutstandingBalance(UUID advertiserId);

}
