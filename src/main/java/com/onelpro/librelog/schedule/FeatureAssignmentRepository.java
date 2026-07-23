package com.onelpro.librelog.schedule;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FeatureAssignmentRepository extends JpaRepository<FeatureAssignment, FeatureAssignment.Key> {
    Optional<FeatureAssignment> findByScheduleDayIdAndShowInstanceId(UUID scheduleDayId, Long showInstanceId);
    List<FeatureAssignment> findByScheduleDayId(UUID scheduleDayId);
}
