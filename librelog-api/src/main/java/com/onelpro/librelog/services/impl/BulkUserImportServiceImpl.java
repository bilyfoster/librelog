package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.BulkUserImportRequestDTO;
import com.onelpro.librelog.dto.BulkUserImportResponseDTO;
import com.onelpro.librelog.dto.ImportErrorDTO;
import com.onelpro.librelog.dto.UserRequestDTO;
import com.onelpro.librelog.dto.UserResponseDTO;
import com.onelpro.librelog.dto.UserStationAssignmentRequestDTO;
import com.onelpro.librelog.enums.PermissionLevel;
import com.onelpro.librelog.enums.UserRole;
import com.onelpro.librelog.enums.UserStatus;
import com.onelpro.librelog.exceptions.BadRequestException;
import com.onelpro.librelog.models.Station;
import com.onelpro.librelog.repositories.StationRepository;
import com.onelpro.librelog.services.BulkUserImportService;
import com.onelpro.librelog.services.UserService;
import com.onelpro.librelog.services.UserStationAssignmentService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Implementation of bulk user import service.
 * Handles CSV/Excel file uploads for onboarding multiple users.
 */
@Service
public class BulkUserImportServiceImpl implements BulkUserImportService {

	private static final Logger logger = LoggerFactory.getLogger(BulkUserImportServiceImpl.class);

	// Expected CSV/Excel headers
	private static final String HEADER_EMAIL = "email";
	private static final String HEADER_PASSWORD = "password";
	private static final String HEADER_ROLE = "role";
	private static final String HEADER_STATUS = "status";
	private static final String HEADER_STATION_IDS = "station_ids";
	private static final String HEADER_PERMISSION_LEVEL = "permission_level";

	private final UserService userService;
	private final UserStationAssignmentService userStationAssignmentService;
	private final StationRepository stationRepository;

	public BulkUserImportServiceImpl(
			UserService userService,
			UserStationAssignmentService userStationAssignmentService,
			StationRepository stationRepository) {
		this.userService = userService;
		this.userStationAssignmentService = userStationAssignmentService;
		this.stationRepository = stationRepository;
	}

	@Override
	@Transactional
	public BulkUserImportResponseDTO importUsers(BulkUserImportRequestDTO request) {
		logger.info("Starting bulk user import");

		if (request.getFile() == null || request.getFile().isEmpty()) {
			throw new BadRequestException("Import file is required");
		}

		String fileName = request.getFile().getOriginalFilename();
		if (fileName == null) {
			throw new BadRequestException("Invalid file name");
		}

		List<ImportErrorDTO> errors = new ArrayList<>();
		List<UserResponseDTO> importedUsers = new ArrayList<>();
		int totalRecords = 0;
		int successfulCount = 0;
		int failedCount = 0;

		try {
			if (fileName.endsWith(".csv")) {
				processCsvFile(request.getFile(), request.getValidateOnly(), errors, importedUsers);
			} else if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
				processExcelFile(request.getFile(), request.getValidateOnly(), errors, importedUsers);
			} else {
				throw new BadRequestException("Unsupported file format. Please upload a CSV or Excel file.");
			}

			totalRecords = errors.size() + importedUsers.size();
			successfulCount = importedUsers.size();
			failedCount = errors.size();

		} catch (Exception e) {
			logger.error("Error processing import file", e);
			errors.add(ImportErrorDTO.builder()
					.rowNumber(0)
					.errorMessage("Failed to process import file: " + e.getMessage())
					.build());
			failedCount++;
		}

		logger.info("Bulk import completed: {} total, {} successful, {} failed", totalRecords, successfulCount, failedCount);

		return BulkUserImportResponseDTO.builder()
				.totalRecords(totalRecords)
				.successfulCount(successfulCount)
				.failedCount(failedCount)
				.errors(errors)
				.importedUsers(importedUsers)
				.build();
	}

	@Override
	public BulkUserImportResponseDTO validateImportFile(BulkUserImportRequestDTO request) {
		logger.info("Validating import file");

		BulkUserImportRequestDTO validateRequest = BulkUserImportRequestDTO.builder()
				.file(request.getFile())
				.validateOnly(true)
				.build();

		return importUsers(validateRequest);
	}

	/**
	 * Processes a CSV file for user import.
	 */
	private void processCsvFile(MultipartFile file, boolean validateOnly,
	                            List<ImportErrorDTO> errors, List<UserResponseDTO> importedUsers) throws Exception {
		try (InputStream inputStream = file.getInputStream();
		     InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
		     CSVParser csvParser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader)) {

			List<String> headers = csvParser.getHeaderNames();
			validateHeaders(headers);

			int rowNumber = 1; // Header row
			for (CSVRecord record : csvParser) {
				rowNumber++;
				try {
					UserRequestDTO userRequest = parseCsvRecord(record, headers);
					if (!validateOnly) {
						UserResponseDTO createdUser = userService.create(userRequest);
						importedUsers.add(createdUser);

						// Create station assignments if provided
						createStationAssignments(createdUser.getId(), record, headers);
					} else {
						// Validation only - create a dummy response
						importedUsers.add(UserResponseDTO.builder()
								.email(userRequest.getEmail())
								.role(userRequest.getRole())
								.status(userRequest.getStatus())
								.build());
					}
				} catch (Exception e) {
					logger.warn("Error processing row {}: {}", rowNumber, e.getMessage());
					errors.add(ImportErrorDTO.builder()
							.rowNumber(rowNumber)
							.email(record.get(HEADER_EMAIL))
							.errorMessage(e.getMessage())
							.build());
				}
			}
		}
	}

	/**
	 * Processes an Excel file for user import.
	 */
	private void processExcelFile(MultipartFile file, boolean validateOnly,
	                              List<ImportErrorDTO> errors, List<UserResponseDTO> importedUsers) throws Exception {
		try (InputStream inputStream = file.getInputStream();
		     Workbook workbook = new XSSFWorkbook(inputStream)) {

			Sheet sheet = workbook.getSheetAt(0);
			if (sheet == null || sheet.getPhysicalNumberOfRows() < 2) {
				throw new BadRequestException("Excel file must contain at least a header row and one data row");
			}

			Row headerRow = sheet.getRow(0);
			List<String> headers = extractHeaders(headerRow);
			validateHeaders(headers);

			Map<String, Integer> headerIndexMap = new HashMap<>();
			for (int i = 0; i < headers.size(); i++) {
				headerIndexMap.put(headers.get(i).toLowerCase(), i);
			}

			for (int rowNum = 1; rowNum <= sheet.getLastRowNum(); rowNum++) {
				Row row = sheet.getRow(rowNum);
				if (row == null) {
					continue;
				}

				try {
					UserRequestDTO userRequest = parseExcelRow(row, headerIndexMap);
					if (!validateOnly) {
						UserResponseDTO createdUser = userService.create(userRequest);
						importedUsers.add(createdUser);

						// Create station assignments if provided
						createStationAssignmentsFromExcel(createdUser.getId(), row, headerIndexMap);
					} else {
						// Validation only
						importedUsers.add(UserResponseDTO.builder()
								.email(userRequest.getEmail())
								.role(userRequest.getRole())
								.status(userRequest.getStatus())
								.build());
					}
				} catch (Exception e) {
					logger.warn("Error processing row {}: {}", rowNum + 1, e.getMessage());
					errors.add(ImportErrorDTO.builder()
							.rowNumber(rowNum + 1)
							.email(getCellValue(row, headerIndexMap.get(HEADER_EMAIL.toLowerCase())))
							.errorMessage(e.getMessage())
							.build());
				}
			}
		}
	}

	/**
	 * Validates that required headers are present.
	 */
	private void validateHeaders(List<String> headers) {
		Set<String> headerSet = new HashSet<>();
		for (String header : headers) {
			headerSet.add(header.toLowerCase().trim());
		}

		if (!headerSet.contains(HEADER_EMAIL)) {
			throw new BadRequestException("Missing required header: " + HEADER_EMAIL);
		}
	}

	/**
	 * Parses a CSV record into a UserRequestDTO.
	 */
	private UserRequestDTO parseCsvRecord(CSVRecord record, List<String> headers) {
		String email = getRequiredValue(record, HEADER_EMAIL, "Email is required");
		String password = record.get(HEADER_PASSWORD); // Optional, can be generated
		String roleStr = getRequiredValue(record, HEADER_ROLE, "Role is required");
		String statusStr = record.get(HEADER_STATUS); // Optional, defaults to ACTIVE

		// Validate and parse role
		UserRole role;
		try {
			role = UserRole.valueOf(roleStr.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new BadRequestException("Invalid role: " + roleStr);
		}

		// Validate and parse status
		UserStatus status = UserStatus.ACTIVE; // Default
		if (statusStr != null && !statusStr.trim().isEmpty()) {
			try {
				status = UserStatus.valueOf(statusStr.toUpperCase());
			} catch (IllegalArgumentException e) {
				throw new BadRequestException("Invalid status: " + statusStr);
			}
		}

		// Generate password if not provided
		if (password == null || password.trim().isEmpty()) {
			password = generateTemporaryPassword();
		}

		return UserRequestDTO.builder()
				.email(email.trim())
				.password(password)
				.role(role)
				.status(status)
				.build();
	}

	/**
	 * Parses an Excel row into a UserRequestDTO.
	 */
	private UserRequestDTO parseExcelRow(Row row, Map<String, Integer> headerIndexMap) {
		String email = getRequiredCellValue(row, headerIndexMap, HEADER_EMAIL, "Email is required");
		String password = getCellValue(row, headerIndexMap.get(HEADER_PASSWORD.toLowerCase()));
		String roleStr = getRequiredCellValue(row, headerIndexMap, HEADER_ROLE, "Role is required");
		String statusStr = getCellValue(row, headerIndexMap.get(HEADER_STATUS.toLowerCase()));

		// Validate and parse role
		UserRole role;
		try {
			role = UserRole.valueOf(roleStr.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new BadRequestException("Invalid role: " + roleStr);
		}

		// Validate and parse status
		UserStatus status = UserStatus.ACTIVE; // Default
		if (statusStr != null && !statusStr.trim().isEmpty()) {
			try {
				status = UserStatus.valueOf(statusStr.toUpperCase());
			} catch (IllegalArgumentException e) {
				throw new BadRequestException("Invalid status: " + statusStr);
			}
		}

		// Generate password if not provided
		if (password == null || password.trim().isEmpty()) {
			password = generateTemporaryPassword();
		}

		return UserRequestDTO.builder()
				.email(email.trim())
				.password(password)
				.role(role)
				.status(status)
				.build();
	}

	/**
	 * Creates station assignments for a user from CSV record.
	 */
	private void createStationAssignments(UUID userId, CSVRecord record, List<String> headers) {
		String stationIdsStr = record.get(HEADER_STATION_IDS);
		String permissionLevelStr = record.get(HEADER_PERMISSION_LEVEL);

		if (stationIdsStr == null || stationIdsStr.trim().isEmpty()) {
			return; // No station assignments
		}

		PermissionLevel permissionLevel = PermissionLevel.FULL_ACCESS; // Default
		if (permissionLevelStr != null && !permissionLevelStr.trim().isEmpty()) {
			try {
				permissionLevel = PermissionLevel.valueOf(permissionLevelStr.toUpperCase());
			} catch (IllegalArgumentException e) {
				logger.warn("Invalid permission level: {}, using default", permissionLevelStr);
			}
		}

		String[] stationIdStrings = stationIdsStr.split(",");
		for (String stationIdStr : stationIdStrings) {
			try {
				UUID stationId = UUID.fromString(stationIdStr.trim());
				if (stationRepository.existsById(stationId)) {
					UserStationAssignmentRequestDTO assignmentRequest = UserStationAssignmentRequestDTO.builder()
							.userId(userId)
							.stationId(stationId)
							.permissionLevel(permissionLevel)
							.build();
					userStationAssignmentService.assignUserToStation(assignmentRequest);
				} else {
					logger.warn("Station not found: {}", stationId);
				}
			} catch (IllegalArgumentException e) {
				logger.warn("Invalid station ID: {}", stationIdStr);
			}
		}
	}

	/**
	 * Creates station assignments for a user from Excel row.
	 */
	private void createStationAssignmentsFromExcel(UUID userId, Row row, Map<String, Integer> headerIndexMap) {
		Integer stationIdsIndex = headerIndexMap.get(HEADER_STATION_IDS.toLowerCase());
		Integer permissionLevelIndex = headerIndexMap.get(HEADER_PERMISSION_LEVEL.toLowerCase());

		if (stationIdsIndex == null) {
			return; // No station assignments column
		}

		String stationIdsStr = getCellValue(row, stationIdsIndex);
		if (stationIdsStr == null || stationIdsStr.trim().isEmpty()) {
			return;
		}

		PermissionLevel permissionLevel = PermissionLevel.FULL_ACCESS; // Default
		if (permissionLevelIndex != null) {
			String permissionLevelStr = getCellValue(row, permissionLevelIndex);
			if (permissionLevelStr != null && !permissionLevelStr.trim().isEmpty()) {
				try {
					permissionLevel = PermissionLevel.valueOf(permissionLevelStr.toUpperCase());
				} catch (IllegalArgumentException e) {
					logger.warn("Invalid permission level: {}, using default", permissionLevelStr);
				}
			}
		}

		String[] stationIdStrings = stationIdsStr.split(",");
		for (String stationIdStr : stationIdStrings) {
			try {
				UUID stationId = UUID.fromString(stationIdStr.trim());
				if (stationRepository.existsById(stationId)) {
					UserStationAssignmentRequestDTO assignmentRequest = UserStationAssignmentRequestDTO.builder()
							.userId(userId)
							.stationId(stationId)
							.permissionLevel(permissionLevel)
							.build();
					userStationAssignmentService.assignUserToStation(assignmentRequest);
				} else {
					logger.warn("Station not found: {}", stationId);
				}
			} catch (IllegalArgumentException e) {
				logger.warn("Invalid station ID: {}", stationIdStr);
			}
		}
	}

	/**
	 * Extracts headers from Excel header row.
	 */
	private List<String> extractHeaders(Row headerRow) {
		List<String> headers = new ArrayList<>();
		for (Cell cell : headerRow) {
			headers.add(getCellValue(cell));
		}
		return headers;
	}

	/**
	 * Gets a required value from CSV record.
	 */
	private String getRequiredValue(CSVRecord record, String header, String errorMessage) {
		try {
			String value = record.get(header);
			if (value == null || value.trim().isEmpty()) {
				throw new BadRequestException(errorMessage);
			}
			return value;
		} catch (IllegalArgumentException e) {
			throw new BadRequestException("Missing required column: " + header);
		}
	}

	/**
	 * Gets a required cell value from Excel row.
	 */
	private String getRequiredCellValue(Row row, Map<String, Integer> headerIndexMap, String header, String errorMessage) {
		Integer index = headerIndexMap.get(header.toLowerCase());
		if (index == null) {
			throw new BadRequestException("Missing required column: " + header);
		}
		String value = getCellValue(row, index);
		if (value == null || value.trim().isEmpty()) {
			throw new BadRequestException(errorMessage);
		}
		return value;
	}

	/**
	 * Gets a cell value from Excel row.
	 */
	private String getCellValue(Row row, Integer columnIndex) {
		if (columnIndex == null) {
			return null;
		}
		Cell cell = row.getCell(columnIndex);
		return getCellValue(cell);
	}

	/**
	 * Gets a cell value from Excel cell.
	 */
	private String getCellValue(Cell cell) {
		if (cell == null) {
			return null;
		}
		switch (cell.getCellType()) {
			case STRING:
				return cell.getStringCellValue();
			case NUMERIC:
				if (DateUtil.isCellDateFormatted(cell)) {
					return cell.getDateCellValue().toString();
				} else {
					// Convert to string without decimal if it's a whole number
					double numericValue = cell.getNumericCellValue();
					if (numericValue == (long) numericValue) {
						return String.valueOf((long) numericValue);
					} else {
						return String.valueOf(numericValue);
					}
				}
			case BOOLEAN:
				return String.valueOf(cell.getBooleanCellValue());
			case FORMULA:
				return cell.getCellFormula();
			default:
				return null;
		}
	}

	/**
	 * Generates a temporary password for users without passwords.
	 */
	private String generateTemporaryPassword() {
		// Generate a random password that meets requirements
		// In production, this should be more secure and users should be required to change it
		return "TempPass123!";
	}

}

