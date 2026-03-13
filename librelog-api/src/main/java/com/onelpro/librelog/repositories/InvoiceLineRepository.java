package com.onelpro.librelog.repositories;

import com.onelpro.librelog.models.InvoiceLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for InvoiceLine entity.
 */
@Repository
public interface InvoiceLineRepository extends JpaRepository<InvoiceLine, UUID> {

	/**
	 * Find invoice lines by invoice ID.
	 */
	List<InvoiceLine> findByInvoiceId(UUID invoiceId);

}
