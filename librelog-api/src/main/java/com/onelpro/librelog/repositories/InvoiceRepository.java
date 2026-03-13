package com.onelpro.librelog.repositories;

import com.onelpro.librelog.models.Invoice;
import com.onelpro.librelog.models.Invoice.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Invoice entity.
 */
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

	/**
	 * Find invoices by advertiser.
	 */
	List<Invoice> findByAdvertiserId(UUID advertiserId);

	/**
	 * Find invoices by campaign.
	 */
	List<Invoice> findByCampaignId(UUID campaignId);

	/**
	 * Find invoices by status.
	 */
	List<Invoice> findByStatus(InvoiceStatus status);

	/**
	 * Find overdue invoices.
	 */
	List<Invoice> findByStatusAndDueDateBefore(InvoiceStatus status, LocalDate date);

	/**
	 * Find invoices by date range.
	 */
	List<Invoice> findByInvoiceDateBetween(LocalDate startDate, LocalDate endDate);

	/**
	 * Find invoice by invoice number.
	 */
	Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

	/**
	 * Get total revenue by status.
	 */
	@Query("SELECT SUM(i.totalAmount) FROM Invoice i WHERE i.status = :status")
	BigDecimal getTotalRevenueByStatus(@Param("status") InvoiceStatus status);

	/**
	 * Count invoices by status.
	 */
	long countByStatus(InvoiceStatus status);

	/**
	 * Get outstanding balance for advertiser.
	 */
	@Query("SELECT SUM(i.balanceDue) FROM Invoice i WHERE i.advertiser.id = :advertiserId AND i.balanceDue > 0")
	BigDecimal getOutstandingBalance(@Param("advertiserId") UUID advertiserId);

}
