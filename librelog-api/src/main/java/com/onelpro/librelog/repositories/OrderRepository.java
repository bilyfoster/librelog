package com.onelpro.librelog.repositories;

import com.onelpro.librelog.enums.OrderStatus;
import com.onelpro.librelog.models.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Order entity operations.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

	Optional<Order> findByOrderNumber(String orderNumber);

	boolean existsByOrderNumber(String orderNumber);

	List<Order> findByStationId(UUID stationId);

	List<Order> findByStatus(OrderStatus status);

	List<Order> findByStationIdAndStatus(UUID stationId, OrderStatus status);

	List<Order> findByStartDateBetween(LocalDate startDate, LocalDate endDate);

	List<Order> findByStationIdAndStartDateBetween(UUID stationId, LocalDate startDate, LocalDate endDate);

}

