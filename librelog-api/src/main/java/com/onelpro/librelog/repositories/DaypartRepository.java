package com.onelpro.librelog.repositories;

import com.onelpro.librelog.models.Daypart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository interface for Daypart entity operations.
 */
@Repository
public interface DaypartRepository extends JpaRepository<Daypart, UUID> {

}

