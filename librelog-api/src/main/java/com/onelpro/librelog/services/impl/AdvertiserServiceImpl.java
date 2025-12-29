package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.AdvertiserRequestDTO;
import com.onelpro.librelog.dto.AdvertiserResponseDTO;
import com.onelpro.librelog.exceptions.BadRequestException;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.models.Advertiser;
import com.onelpro.librelog.models.Agency;
import com.onelpro.librelog.models.SalesRep;
import com.onelpro.librelog.repositories.AdvertiserRepository;
import com.onelpro.librelog.repositories.AgencyRepository;
import com.onelpro.librelog.repositories.SalesRepRepository;
import com.onelpro.librelog.services.AdvertiserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of advertiser service.
 */
@Service
public class AdvertiserServiceImpl implements AdvertiserService {

	private static final Logger logger = LoggerFactory.getLogger(AdvertiserServiceImpl.class);

	private final AdvertiserRepository advertiserRepository;
	private final AgencyRepository agencyRepository;
	private final SalesRepRepository salesRepRepository;

	public AdvertiserServiceImpl(
			AdvertiserRepository advertiserRepository,
			AgencyRepository agencyRepository,
			SalesRepRepository salesRepRepository) {
		this.advertiserRepository = advertiserRepository;
		this.agencyRepository = agencyRepository;
		this.salesRepRepository = salesRepRepository;
	}

	@Override
	@Transactional
	public AdvertiserResponseDTO create(AdvertiserRequestDTO request) {
		logger.info("Creating advertiser with name: {}", request.getName());

		if (request.getAgencyId() != null && !agencyRepository.existsById(request.getAgencyId())) {
			logger.warn("Agency not found with ID: {}", request.getAgencyId());
			throw new NotFoundException("Agency not found with ID: " + request.getAgencyId());
		}

		if (request.getSalesRepId() != null && !salesRepRepository.existsById(request.getSalesRepId())) {
			logger.warn("Sales rep not found with ID: {}", request.getSalesRepId());
			throw new NotFoundException("Sales rep not found with ID: " + request.getSalesRepId());
		}

		Advertiser advertiser = Advertiser.builder()
				.name(request.getName())
				.legalName(request.getLegalName())
				.taxId(request.getTaxId())
				.creditLimit(request.getCreditLimit())
				.paymentTerms(request.getPaymentTerms())
				.addressLine1(request.getAddressLine1())
				.addressLine2(request.getAddressLine2())
				.city(request.getCity())
				.state(request.getState())
				.zipCode(request.getZipCode())
				.country(request.getCountry())
				.phone(request.getPhone())
				.email(request.getEmail())
				.website(request.getWebsite())
				.agencyId(request.getAgencyId())
				.salesRepId(request.getSalesRepId())
				.notes(request.getNotes())
				.isActive(request.getIsActive() != null ? request.getIsActive() : true)
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();

		advertiser = advertiserRepository.save(advertiser);
		logger.info("Advertiser created successfully with ID: {}", advertiser.getId());

		return mapToResponseDTO(advertiser);
	}

	@Override
	public AdvertiserResponseDTO getById(UUID id) {
		logger.debug("Fetching advertiser with ID: {}", id);
		Advertiser advertiser = advertiserRepository.findById(id)
				.orElseThrow(() -> {
					logger.warn("Advertiser not found with ID: {}", id);
					return new NotFoundException("Advertiser not found with ID: " + id);
				});
		return mapToResponseDTO(advertiser);
	}

	@Override
	public List<AdvertiserResponseDTO> getAll() {
		logger.debug("Fetching all advertisers");
		return advertiserRepository.findAll().stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	public List<AdvertiserResponseDTO> getByAgencyId(UUID agencyId) {
		logger.debug("Fetching advertisers for agency: {}", agencyId);
		return advertiserRepository.findByAgencyId(agencyId).stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	public List<AdvertiserResponseDTO> getBySalesRepId(UUID salesRepId) {
		logger.debug("Fetching advertisers for sales rep: {}", salesRepId);
		return advertiserRepository.findBySalesRepId(salesRepId).stream()
				.map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public AdvertiserResponseDTO update(UUID id, AdvertiserRequestDTO request) {
		logger.info("Updating advertiser with ID: {}", id);
		Advertiser advertiser = advertiserRepository.findById(id)
				.orElseThrow(() -> {
					logger.warn("Advertiser not found with ID: {}", id);
					return new NotFoundException("Advertiser not found with ID: " + id);
				});

		if (request.getAgencyId() != null && !agencyRepository.existsById(request.getAgencyId())) {
			logger.warn("Agency not found with ID: {}", request.getAgencyId());
			throw new NotFoundException("Agency not found with ID: " + request.getAgencyId());
		}

		if (request.getSalesRepId() != null && !salesRepRepository.existsById(request.getSalesRepId())) {
			logger.warn("Sales rep not found with ID: {}", request.getSalesRepId());
			throw new NotFoundException("Sales rep not found with ID: " + request.getSalesRepId());
		}

		advertiser.setName(request.getName());
		advertiser.setLegalName(request.getLegalName());
		advertiser.setTaxId(request.getTaxId());
		advertiser.setCreditLimit(request.getCreditLimit());
		advertiser.setPaymentTerms(request.getPaymentTerms());
		advertiser.setAddressLine1(request.getAddressLine1());
		advertiser.setAddressLine2(request.getAddressLine2());
		advertiser.setCity(request.getCity());
		advertiser.setState(request.getState());
		advertiser.setZipCode(request.getZipCode());
		advertiser.setCountry(request.getCountry());
		advertiser.setPhone(request.getPhone());
		advertiser.setEmail(request.getEmail());
		advertiser.setWebsite(request.getWebsite());
		advertiser.setAgencyId(request.getAgencyId());
		advertiser.setSalesRepId(request.getSalesRepId());
		advertiser.setNotes(request.getNotes());
		if (request.getIsActive() != null) {
			advertiser.setIsActive(request.getIsActive());
		}
		advertiser.setUpdatedAt(LocalDateTime.now());

		advertiser = advertiserRepository.save(advertiser);
		logger.info("Advertiser updated successfully with ID: {}", advertiser.getId());

		return mapToResponseDTO(advertiser);
	}

	@Override
	@Transactional
	public void delete(UUID id) {
		logger.info("Deleting advertiser with ID: {}", id);
		if (!advertiserRepository.existsById(id)) {
			logger.warn("Advertiser not found with ID: {}", id);
			throw new NotFoundException("Advertiser not found with ID: " + id);
		}
		advertiserRepository.deleteById(id);
		logger.info("Advertiser deleted successfully with ID: {}", id);
	}

	private AdvertiserResponseDTO mapToResponseDTO(Advertiser advertiser) {
		String agencyName = null;
		if (advertiser.getAgencyId() != null) {
			agencyName = agencyRepository.findById(advertiser.getAgencyId())
					.map(Agency::getName)
					.orElse(null);
		}

		String salesRepName = null;
		if (advertiser.getSalesRepId() != null) {
			salesRepName = salesRepRepository.findById(advertiser.getSalesRepId())
					.map(rep -> rep.getFirstName() + " " + rep.getLastName())
					.orElse(null);
		}

		return AdvertiserResponseDTO.builder()
				.id(advertiser.getId())
				.name(advertiser.getName())
				.legalName(advertiser.getLegalName())
				.taxId(advertiser.getTaxId())
				.creditLimit(advertiser.getCreditLimit())
				.paymentTerms(advertiser.getPaymentTerms())
				.addressLine1(advertiser.getAddressLine1())
				.addressLine2(advertiser.getAddressLine2())
				.city(advertiser.getCity())
				.state(advertiser.getState())
				.zipCode(advertiser.getZipCode())
				.country(advertiser.getCountry())
				.phone(advertiser.getPhone())
				.email(advertiser.getEmail())
				.website(advertiser.getWebsite())
				.agencyId(advertiser.getAgencyId())
				.agencyName(agencyName)
				.salesRepId(advertiser.getSalesRepId())
				.salesRepName(salesRepName)
				.notes(advertiser.getNotes())
				.isActive(advertiser.getIsActive())
				.createdAt(advertiser.getCreatedAt())
				.updatedAt(advertiser.getUpdatedAt())
				.build();
	}

}

