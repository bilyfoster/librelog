package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.InvoiceLineResponseDTO;
import com.onelpro.librelog.dto.InvoiceRequestDTO;
import com.onelpro.librelog.dto.InvoiceResponseDTO;
import com.onelpro.librelog.dto.PaymentResponseDTO;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.models.Advertiser;
import com.onelpro.librelog.models.Campaign;
import com.onelpro.librelog.models.Invoice;
import com.onelpro.librelog.models.Invoice.InvoiceStatus;
import com.onelpro.librelog.models.InvoiceLine;
import com.onelpro.librelog.models.Spot;
import com.onelpro.librelog.repositories.AdvertiserRepository;
import com.onelpro.librelog.repositories.CampaignRepository;
import com.onelpro.librelog.repositories.InvoiceLineRepository;
import com.onelpro.librelog.repositories.InvoiceRepository;
import com.onelpro.librelog.repositories.SpotRepository;
import com.onelpro.librelog.services.InvoiceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of invoice management service.
 */
@Service
public class InvoiceServiceImpl implements InvoiceService {

	private static final Logger logger = LoggerFactory.getLogger(InvoiceServiceImpl.class);

	private final InvoiceRepository invoiceRepository;
	private final InvoiceLineRepository invoiceLineRepository;
	private final CampaignRepository campaignRepository;
	private final AdvertiserRepository advertiserRepository;
	private final SpotRepository spotRepository;

	public InvoiceServiceImpl(InvoiceRepository invoiceRepository,
	                          InvoiceLineRepository invoiceLineRepository,
	                          CampaignRepository campaignRepository,
	                          AdvertiserRepository advertiserRepository,
	                          SpotRepository spotRepository) {
		this.invoiceRepository = invoiceRepository;
		this.invoiceLineRepository = invoiceLineRepository;
		this.campaignRepository = campaignRepository;
		this.advertiserRepository = advertiserRepository;
		this.spotRepository = spotRepository;
	}

	@Override
	@Transactional
	public InvoiceResponseDTO create(InvoiceRequestDTO request) {
		logger.info("Creating invoice for campaign: {}", request.getCampaignId());

		Campaign campaign = campaignRepository.findById(request.getCampaignId())
				.orElseThrow(() -> new NotFoundException("Campaign not found: " + request.getCampaignId()));

		Advertiser advertiser = advertiserRepository.findById(request.getAdvertiserId())
				.orElseThrow(() -> new NotFoundException("Advertiser not found: " + request.getAdvertiserId()));

		Invoice invoice = Invoice.builder()
				.invoiceNumber(generateInvoiceNumber())
				.campaign(campaign)
				.advertiser(advertiser)
				.invoiceDate(request.getInvoiceDate())
				.dueDate(request.getDueDate())
				.taxRate(request.getTaxRate())
				.notes(request.getNotes())
				.createdAt(LocalDateTime.now())
				.build();

		// Add invoice lines
		if (request.getLines() != null) {
			for (var lineDTO : request.getLines()) {
				InvoiceLine line = InvoiceLine.builder()
						.invoice(invoice)
						.description(lineDTO.getDescription())
						.spotDate(lineDTO.getSpotDate())
						.daypart(lineDTO.getDaypart())
						.spotLengthSeconds(lineDTO.getSpotLengthSeconds())
						.quantity(lineDTO.getQuantity())
						.unitPrice(lineDTO.getUnitPrice())
						.spotId(lineDTO.getSpotId())
						.build();
				line.calculateLineTotal();
				invoice.getLines().add(line);
			}
		}

		invoice.calculateTotals();

		Invoice saved = invoiceRepository.save(invoice);
		logger.info("Invoice created successfully: {}", saved.getInvoiceNumber());

		return mapToResponseDTO(saved);
	}

	@Override
	@Transactional
	public InvoiceResponseDTO generateFromCampaign(UUID campaignId, LocalDate invoiceDate, LocalDate dueDate) {
		logger.info("Generating invoice from campaign: {}", campaignId);

		Campaign campaign = campaignRepository.findById(campaignId)
				.orElseThrow(() -> new NotFoundException("Campaign not found: " + campaignId));

		// Get aired spots for this campaign
		List<Spot> airedSpots = spotRepository.findByCampaignId(campaignId).stream()
				.filter(s -> s.getStatus().toString().equals("AIRED"))
				.collect(Collectors.toList());

		if (airedSpots.isEmpty()) {
			throw new IllegalStateException("No aired spots found for campaign: " + campaignId);
		}

		Invoice invoice = Invoice.builder()
				.invoiceNumber(generateInvoiceNumber())
				.campaign(campaign)
				.advertiser(campaign.getAdvertiser())
				.invoiceDate(invoiceDate)
				.dueDate(dueDate)
				.createdAt(LocalDateTime.now())
				.build();

		// Group spots by daypart for line items
		// For simplicity, creating one line per spot
		for (Spot spot : airedSpots) {
			InvoiceLine line = InvoiceLine.builder()
					.invoice(invoice)
					.description(String.format("%s - %s spot on %s", 
							campaign.getName(), 
							spot.getDaypart() != null ? spot.getDaypart() : "Standard",
							spot.getScheduledDate()))
					.spotDate(spot.getScheduledDate())
					.daypart(spot.getDaypart())
					.spotLengthSeconds(spot.getSpotLength())
					.quantity(1)
					.unitPrice(calculateSpotRate(campaign, spot))
					.spotId(spot.getId())
					.build();
			line.calculateLineTotal();
			invoice.getLines().add(line);
		}

		invoice.calculateTotals();

		Invoice saved = invoiceRepository.save(invoice);
		logger.info("Invoice generated from campaign: {} with {} lines", saved.getInvoiceNumber(), saved.getLines().size());

		return mapToResponseDTO(saved);
	}

	@Override
	@Transactional(readOnly = true)
	public InvoiceResponseDTO getById(UUID id) {
		logger.debug("Fetching invoice: {}", id);
		Invoice invoice = invoiceRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Invoice not found: " + id));
		return mapToResponseDTO(invoice);
	}

	@Override
	@Transactional(readOnly = true)
	public InvoiceResponseDTO getByNumber(String invoiceNumber) {
		logger.debug("Fetching invoice by number: {}", invoiceNumber);
		Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
				.orElseThrow(() -> new NotFoundException("Invoice not found: " + invoiceNumber));
		return mapToResponseDTO(invoice);
	}

	@Override
	@Transactional(readOnly = true)
	public List<InvoiceResponseDTO> getByAdvertiserId(UUID advertiserId) {
		logger.debug("Fetching invoices for advertiser: {}", advertiserId);
		return invoiceRepository.findByAdvertiserId(advertiserId).stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public List<InvoiceResponseDTO> getByCampaignId(UUID campaignId) {
		logger.debug("Fetching invoices for campaign: {}", campaignId);
		return invoiceRepository.findByCampaignId(campaignId).stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public List<InvoiceResponseDTO> getByStatus(InvoiceStatus status) {
		logger.debug("Fetching invoices by status: {}", status);
		return invoiceRepository.findByStatus(status).stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public List<InvoiceResponseDTO> getOverdue() {
		logger.debug("Fetching overdue invoices");
		return invoiceRepository.findByStatusAndDueDateBefore(InvoiceStatus.SENT, LocalDate.now()).stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public InvoiceResponseDTO updateStatus(UUID id, InvoiceStatus status) {
		logger.info("Updating invoice status: {} to {}", id, status);
		Invoice invoice = invoiceRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Invoice not found: " + id));
		
		invoice.setStatus(status);
		invoice.setUpdatedAt(LocalDateTime.now());
		
		Invoice updated = invoiceRepository.save(invoice);
		return mapToResponseDTO(updated);
	}

	@Override
	@Transactional
	public void delete(UUID id) {
		logger.info("Deleting invoice: {}", id);
		if (!invoiceRepository.existsById(id)) {
			throw new NotFoundException("Invoice not found: " + id);
		}
		invoiceRepository.deleteById(id);
	}

	@Override
	@Transactional(readOnly = true)
	public BigDecimal getOutstandingBalance(UUID advertiserId) {
		BigDecimal balance = invoiceRepository.getOutstandingBalance(advertiserId);
		return balance != null ? balance : BigDecimal.ZERO;
	}

	private String generateInvoiceNumber() {
		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
		String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
		return "INV-" + timestamp + "-" + random;
	}

	private BigDecimal calculateSpotRate(Campaign campaign, Spot spot) {
		// Simple rate calculation - in production, this would come from campaign daypart rates
		// Default $50 per 30-second spot
		int baseRate = 50;
		if (spot.getDaypart() != null) {
			switch (spot.getDaypart().toUpperCase()) {
				case "MORNING DRIVE", "DRIVE TIME" -> baseRate = 100;
				case "AFTERNOON DRIVE" -> baseRate = 80;
				case "EVENING" -> baseRate = 60;
				default -> baseRate = 50;
			}
		}
		return new BigDecimal(baseRate);
	}

	private InvoiceResponseDTO mapToResponseDTO(Invoice invoice) {
		return InvoiceResponseDTO.builder()
				.id(invoice.getId())
				.invoiceNumber(invoice.getInvoiceNumber())
				.campaignId(invoice.getCampaign() != null ? invoice.getCampaign().getId() : null)
				.campaignName(invoice.getCampaign() != null ? invoice.getCampaign().getName() : null)
				.advertiserId(invoice.getAdvertiser() != null ? invoice.getAdvertiser().getId() : null)
				.advertiserName(invoice.getAdvertiser() != null ? invoice.getAdvertiser().getName() : null)
				.invoiceDate(invoice.getInvoiceDate())
				.dueDate(invoice.getDueDate())
				.status(invoice.getStatus())
				.subtotal(invoice.getSubtotal())
				.taxRate(invoice.getTaxRate())
				.taxAmount(invoice.getTaxAmount())
				.totalAmount(invoice.getTotalAmount())
				.amountPaid(invoice.getAmountPaid())
				.balanceDue(invoice.getBalanceDue())
				.notes(invoice.getNotes())
				.lines(invoice.getLines().stream()
						.map(this::mapLineToDTO)
						.collect(Collectors.toList()))
				.createdAt(invoice.getCreatedAt())
				.updatedAt(invoice.getUpdatedAt())
				.build();
	}

	private InvoiceLineResponseDTO mapLineToDTO(InvoiceLine line) {
		return InvoiceLineResponseDTO.builder()
				.id(line.getId())
				.description(line.getDescription())
				.spotDate(line.getSpotDate())
				.daypart(line.getDaypart())
				.spotLengthSeconds(line.getSpotLengthSeconds())
				.quantity(line.getQuantity())
				.unitPrice(line.getUnitPrice())
				.lineTotal(line.getLineTotal())
				.spotId(line.getSpotId())
				.build();
	}

}
