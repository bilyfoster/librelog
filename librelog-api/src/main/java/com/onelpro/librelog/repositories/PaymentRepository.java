package com.onelpro.librelog.repositories;

import com.onelpro.librelog.models.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Payment entity.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

	/**
	 * Find payments by invoice.
	 */
	List<Payment> findByInvoiceId(UUID invoiceId);

	/**
	 * Find payments by date range.
	 */
	List<Payment> findByPaymentDateBetween(LocalDate startDate, LocalDate endDate);

	/**
	 * Get total payments for invoice.
	 */
	@Query("SELECT SUM(p.amount) FROM Payment p WHERE p.invoice.id = :invoiceId")
	BigDecimal getTotalPaymentsByInvoiceId(@Param("invoiceId") UUID invoiceId);

	/**
	 * Get total payments by date range.
	 */
	@Query("SELECT SUM(p.amount) FROM Payment p WHERE p.paymentDate BETWEEN :startDate AND :endDate")
	BigDecimal getTotalPaymentsByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

}
