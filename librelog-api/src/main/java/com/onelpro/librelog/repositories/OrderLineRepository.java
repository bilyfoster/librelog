package com.onelpro.librelog.repositories;

import com.onelpro.librelog.models.OrderLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for OrderLine entity operations.
 */
@Repository
public interface OrderLineRepository extends JpaRepository<OrderLine, UUID> {

	List<OrderLine> findByOrderId(UUID orderId);

	void deleteByOrderId(UUID orderId);

}

