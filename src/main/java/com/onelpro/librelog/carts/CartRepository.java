package com.onelpro.librelog.carts;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends JpaRepository<Cart, UUID> {
    List<Cart> findByStationIdOrderByKindAscNameAsc(UUID stationId);
    List<Cart> findByStationIdAndKindOrderByNameAsc(UUID stationId, String kind);

    List<Cart> findByStationIdAndKindAndCategoryOrderByNameAsc(UUID stationId, String kind, String category);
    Optional<Cart> findByStationIdAndName(UUID stationId, String name);
    List<Cart> findByOrderId(UUID orderId);
}
