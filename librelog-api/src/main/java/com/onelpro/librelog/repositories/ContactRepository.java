package com.onelpro.librelog.repositories;

import com.onelpro.librelog.models.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for Contact entity operations.
 */
@Repository
public interface ContactRepository extends JpaRepository<Contact, UUID> {

	List<Contact> findByAdvertiserId(UUID advertiserId);

	List<Contact> findByAgencyId(UUID agencyId);

	List<Contact> findByAdvertiserIdAndIsPrimary(UUID advertiserId, Boolean isPrimary);

	List<Contact> findByAgencyIdAndIsPrimary(UUID agencyId, Boolean isPrimary);

}

