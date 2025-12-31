package com.onelpro.librelog.repositories;

import com.onelpro.librelog.enums.EndpointStatus;
import com.onelpro.librelog.models.LibreTimeApiEndpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for LibreTimeApiEndpoint entity operations.
 */
@Repository
public interface LibreTimeApiEndpointRepository extends JpaRepository<LibreTimeApiEndpoint, UUID> {

	/**
	 * Finds endpoint by path and HTTP method.
	 * 
	 * @param endpointPath The endpoint path
	 * @param httpMethod The HTTP method
	 * @return Optional containing the endpoint if found
	 */
	Optional<LibreTimeApiEndpoint> findByEndpointPathAndHttpMethod(String endpointPath, String httpMethod);

	/**
	 * Finds all endpoints by status.
	 * 
	 * @param status The endpoint status
	 * @return List of endpoints with the specified status
	 */
	List<LibreTimeApiEndpoint> findByStatus(EndpointStatus status);

	/**
	 * Finds all endpoints by resource type.
	 * 
	 * @param resourceType The resource type
	 * @return List of endpoints for the resource type
	 */
	List<LibreTimeApiEndpoint> findByResourceType(String resourceType);

	/**
	 * Finds all endpoints that require authentication.
	 * 
	 * @param requiresAuthentication Whether authentication is required
	 * @return List of endpoints
	 */
	List<LibreTimeApiEndpoint> findByRequiresAuthentication(Boolean requiresAuthentication);

}

