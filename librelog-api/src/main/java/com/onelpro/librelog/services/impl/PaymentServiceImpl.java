package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.PaymentRequestDTO;
import com.onelpro.librelog.dto.PaymentResponseDTO;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.models.Invoice;
import com.onelpro.librelog.models.Payment;
import com.onelpro.librelog.repositories.InvoiceRepository;
import com.onelpro.librelog.repositories.PaymentRepository;
import com.onelpro.librelog.services.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of payment management service.
 */
@Service
public class PaymentServiceImpl implements PaymentService {

	private static final Logger logger = LoggerFactory.getLogger(PaymentServiceImpl.class);

	private final PaymentRepository paymentRepository;
	private final InvoiceRepository invoiceRepository;

	public PaymentServiceImpl(PaymentRepository paymentRepository,
	                          InvoiceRepository invoiceRepository) {
		this.paymentRepository = paymentRepository;
		this.invoiceRepository = invoiceRepository;
	}

	@Override
	@Transactional
	public PaymentResponseDTO create(PaymentRequestDTO request) {
		logger.info("Recording payment of {} for invoice: {}", request.getAmount(), request.getInvoiceId());

		Invoice invoice = invoiceRepository.findById(request.getInvoiceId())
				.orElseThrow(() -> new NotFoundException("Invoice not found: " + request.getInvoiceId()));

		Payment payment = Payment.builder()
				.invoice(invoice)
				.amount(request.getAmount())
				.paymentDate(request.getPaymentDate())
				.paymentMethod(request.getPaymentMethod())
				.referenceNumber(request.getReferenceNumber())
				.notes(request.getNotes())
				.createdAt(LocalDateTime.now())
				.build();

		Payment saved = paymentRepository.save(payment);

		// Update invoice payment status
		BigDecimal totalPayments = paymentRepository.getTotalPaymentsByInvoiceId(invoice.getId());
		invoice.setAmountPaid(totalPayments);
		invoice.calculateTotals(); // This updates status and balance
		invoiceRepository.save(invoice);

		logger.info("Payment recorded successfully: {}", saved.getId());
		return mapToResponseDTO(saved);
	}

	@Override
	@Transactional(readOnly = true)
	public PaymentResponseDTO getById(UUID id) {
		logger.debug("Fetching payment: {}", id);
		Payment payment = paymentRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Payment not found: " + id));
		return mapToResponseDTO(payment);
	}

	@Override
	@Transactional(readOnly = true)
	public List<PaymentResponseDTO> getByInvoiceId(UUID invoiceId) {
		logger.debug("Fetching payments for invoice: {}", invoiceId);
		return paymentRepository.findByInvoiceId(invoiceId).stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public List<PaymentResponseDTO> getByDateRange(LocalDate startDate, LocalDate endDate) {
		logger.debug("Fetching payments between {} and {}", startDate, endDate);
		return paymentRepository.findByPaymentDateBetween(startDate, endDate).stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public void delete(UUID id) {
		logger.info("Deleting payment: {}", id);
		Payment payment = paymentRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Payment not found: " + id));
		
		UUID invoiceId = payment.getInvoice().getId();
		paymentRepository.deleteById(id);
		
		// Recalculate invoice totals
		Invoice invoice = invoiceRepository.findById(invoiceId)
				.orElseThrow(() -> new NotFoundException("Invoice not found: " + invoiceId));
		BigDecimal totalPayments = paymentRepository.getTotalPaymentsByInvoiceId(invoiceId);
		invoice.setAmountPaid(totalPayments != null ? totalPayments : BigDecimal.ZERO);
		invoice.calculateTotals();
		invoiceRepository.save(invoice);
		
		logger.info("Payment deleted and invoice updated: {}", id);
	}

	@Override
	@Transactional(readOnly = true)
	public BigDecimal getTotalPaymentsForInvoice(UUID invoiceId) {
		BigDecimal total = paymentRepository.getTotalPaymentsByInvoiceId(invoiceId);
		return total != null ? total : BigDecimal.ZERO;
	}

	private PaymentResponseDTO mapToResponseDTO(Payment payment) {
		return PaymentResponseDTO.builder()
				.id(payment.getId())
				.invoiceId(payment.getInvoice() != null ? payment.getInvoice().getId() : null)
				.invoiceNumber(payment.getInvoice() != null ? payment.getInvoice().getInvoiceNumber() : null)
				.amount(payment.getAmount())
				.paymentDate(payment.getPaymentDate())
				.paymentMethod(payment.getPaymentMethod())
				.referenceNumber(payment.getReferenceNumber())
				.notes(payment.getNotes())
				.createdAt(payment.getCreatedAt())
				.build();
	}

}
