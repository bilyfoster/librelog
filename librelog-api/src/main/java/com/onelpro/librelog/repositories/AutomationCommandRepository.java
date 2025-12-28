package com.onelpro.librelog.repositories;

import com.onelpro.librelog.models.AutomationCommand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for AutomationCommand entity operations.
 */
@Repository
public interface AutomationCommandRepository extends JpaRepository<AutomationCommand, UUID> {

	List<AutomationCommand> findByClockTemplateId(UUID clockTemplateId);

}

