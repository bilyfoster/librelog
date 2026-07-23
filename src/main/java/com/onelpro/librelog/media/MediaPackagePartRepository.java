package com.onelpro.librelog.media;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MediaPackagePartRepository extends JpaRepository<MediaPackagePart, UUID> {
    List<MediaPackagePart> findByPackageIdOrderBySequenceAsc(UUID packageId);
    void deleteByPackageId(UUID packageId);
}
