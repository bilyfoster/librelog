package com.onelpro.librelog.orders;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByStationIdOrderByStartDateDesc(UUID stationId);
    List<Order> findByCustomerIdOrderByStartDateDesc(UUID customerId);
    List<Order> findByStationIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(UUID stationId, LocalDate end, LocalDate start);
}
