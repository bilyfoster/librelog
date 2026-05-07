package com.onelpro.librelog.orders;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpotRepository extends JpaRepository<Spot, UUID> {
    List<Spot> findByOrderIdOrderByCreatedAtAsc(UUID orderId);
    long countByOrderId(UUID orderId);
}
