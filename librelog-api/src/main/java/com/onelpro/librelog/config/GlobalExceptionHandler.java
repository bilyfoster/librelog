package com.onelpro.librelog.config;

import com.onelpro.librelog.dto.ErrorResponseDTO;
import com.onelpro.librelog.exceptions.BadRequestException;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.exceptions.UnauthorizedException;
import com.onelpro.librelog.exceptions.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for centralized exception handling across all controllers.
 * 
 * Provides standardized error responses and proper HTTP status codes.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(NotFoundException.class)
	public ResponseEntity<ErrorResponseDTO> handleNotFoundException(
			NotFoundException ex, WebRequest request) {
		logger.warn("Resource not found: {}", ex.getMessage());
		ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
				.message(ex.getMessage())
				.error("Not Found")
				.status(HttpStatus.NOT_FOUND.value())
				.timestamp(LocalDateTime.now())
				.path(request.getDescription(false).replace("uri=", ""))
				.build();
		return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
	}

	@ExceptionHandler(ValidationException.class)
	public ResponseEntity<ErrorResponseDTO> handleValidationException(
			ValidationException ex, WebRequest request) {
		logger.warn("Validation error: {}", ex.getMessage());
		ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
				.message(ex.getMessage())
				.error("Validation Failed")
				.status(HttpStatus.BAD_REQUEST.value())
				.timestamp(LocalDateTime.now())
				.path(request.getDescription(false).replace("uri=", ""))
				.build();
		return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(UnauthorizedException.class)
	public ResponseEntity<ErrorResponseDTO> handleUnauthorizedException(
			UnauthorizedException ex, WebRequest request) {
		logger.warn("Unauthorized access: {}", ex.getMessage());
		ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
				.message(ex.getMessage())
				.error("Unauthorized")
				.status(HttpStatus.UNAUTHORIZED.value())
				.timestamp(LocalDateTime.now())
				.path(request.getDescription(false).replace("uri=", ""))
				.build();
		return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
	}

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<ErrorResponseDTO> handleBadRequestException(
			BadRequestException ex, WebRequest request) {
		logger.warn("Bad request: {}", ex.getMessage());
		ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
				.message(ex.getMessage())
				.error("Bad Request")
				.status(HttpStatus.BAD_REQUEST.value())
				.timestamp(LocalDateTime.now())
				.path(request.getDescription(false).replace("uri=", ""))
				.build();
		return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValidException(
			MethodArgumentNotValidException ex, WebRequest request) {
		logger.warn("Method argument validation failed");
		Map<String, String> errors = new HashMap<>();
		ex.getBindingResult().getAllErrors().forEach(error -> {
			String fieldName = ((FieldError) error).getField();
			String errorMessage = error.getDefaultMessage();
			errors.put(fieldName, errorMessage);
		});

		Map<String, Object> errorResponse = new HashMap<>();
		errorResponse.put("message", "Validation failed");
		errorResponse.put("errors", errors);
		errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
		errorResponse.put("timestamp", LocalDateTime.now());
		errorResponse.put("path", request.getDescription(false).replace("uri=", ""));

		return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponseDTO> handleGenericException(
			Exception ex, WebRequest request) {
		logger.error("Unexpected error occurred", ex);
		ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
				.message("An unexpected error occurred")
				.error("Internal Server Error")
				.status(HttpStatus.INTERNAL_SERVER_ERROR.value())
				.timestamp(LocalDateTime.now())
				.path(request.getDescription(false).replace("uri=", ""))
				.build();
		return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
	}

}

