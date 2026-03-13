package com.onelpro.librelog.services;

import com.onelpro.librelog.dto.PaymentRequestDTO;
import com.onelpro.librelog.dto.PaymentResponseDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Service interface for payment management.
 */
public interface PaymentService {

	/**
	 * Record a payment.
	 */
	PaymentResponseDTO create(PaymentRequestDTO request);

	/**
	 * Get payment by ID.
	 */
	PaymentResponseDTO getById(UUID id);

	/**
	 * Get payments by invoice.
	 */
	List<PaymentResponseDTO> getByInvoiceId(UUID invoiceId);

	/**
	 * Get payments by date range.
	 */
	List<PaymentResponseDTO> getByDateRange(LocalDate startDate, LocalDate endDate);

	/**
	 * Delete payment.
	 */
	void delete(UUID id);

	/**
	 * Get total payments for invoice.
	 */
	BigDecimal getTotalPaymentsForInvoice(UUID invoiceId);

}
