package com.onelpro.librelog.carts;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SeparationPolicyRepository extends JpaRepository<SeparationPolicy, UUID> {
}
