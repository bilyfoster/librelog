package com.onelpro.librelog.carts;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface CartMemberRepository extends JpaRepository<CartMember, UUID> {
    List<CartMember> findByCartIdOrderByPositionAsc(UUID cartId);

    @Transactional
    void deleteByCartId(UUID cartId);

    @Transactional
    void deleteBySpotId(UUID spotId);
}
