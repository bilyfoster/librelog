package com.onelpro.librelog.repositories;

import com.onelpro.librelog.models.OrderAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for OrderAttachment entity operations.
 */
@Repository
public interface OrderAttachmentRepository extends JpaRepository<OrderAttachment, UUID> {

	List<OrderAttachment> findByOrderId(UUID orderId);

}

