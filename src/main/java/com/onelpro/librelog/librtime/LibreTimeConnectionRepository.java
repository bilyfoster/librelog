package com.onelpro.librelog.librtime;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LibreTimeConnectionRepository extends JpaRepository<LibreTimeConnection, UUID> {
}
