package com.onelpro.librelog.repositories;

import com.onelpro.librelog.enums.TestStatus;
import com.onelpro.librelog.enums.TestType;
import com.onelpro.librelog.models.LibreTimeApiEndpoint;
import com.onelpro.librelog.models.LibreTimeApiTestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for LibreTimeApiTestResult entity operations.
 */
@Repository
public interface LibreTimeApiTestResultRepository extends JpaRepository<LibreTimeApiTestResult, UUID> {

	/**
	 * Finds all test results for a specific endpoint.
	 * 
	 * @param endpoint The endpoint
	 * @return List of test results
	 */
	List<LibreTimeApiTestResult> findByEndpoint(LibreTimeApiEndpoint endpoint);

	/**
	 * Finds all test results by test type.
	 * 
	 * @param testType The test type
	 * @return List of test results
	 */
	List<LibreTimeApiTestResult> findByTestType(TestType testType);

	/**
	 * Finds all test results by status.
	 * 
	 * @param status The test status
	 * @return List of test results
	 */
	List<LibreTimeApiTestResult> findByStatus(TestStatus status);

	/**
	 * Finds all test results for an endpoint by test type.
	 * 
	 * @param endpoint The endpoint
	 * @param testType The test type
	 * @return List of test results
	 */
	List<LibreTimeApiTestResult> findByEndpointAndTestType(LibreTimeApiEndpoint endpoint, TestType testType);

	/**
	 * Finds test results within a date range.
	 * 
	 * @param startDate Start date
	 * @param endDate End date
	 * @return List of test results
	 */
	List<LibreTimeApiTestResult> findByTestTimestampBetween(LocalDateTime startDate, LocalDateTime endDate);

}

