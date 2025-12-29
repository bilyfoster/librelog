package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.AuthResponseDTO;
import com.onelpro.librelog.dto.LoginRequestDTO;
import com.onelpro.librelog.dto.RegisterRequestDTO;
import com.onelpro.librelog.enums.UserRole;
import com.onelpro.librelog.enums.UserStatus;
import com.onelpro.librelog.exceptions.BadRequestException;
import com.onelpro.librelog.exceptions.UnauthorizedException;
import com.onelpro.librelog.exceptions.ValidationException;
import com.onelpro.librelog.models.User;
import com.onelpro.librelog.repositories.UserRepository;
import com.onelpro.librelog.services.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private JwtService jwtService;

	@Mock
	private PasswordEncoder passwordEncoder;

	@InjectMocks
	private AuthServiceImpl authService;

	private RegisterRequestDTO registerRequest;
	private LoginRequestDTO loginRequest;
	private User testUser;

	@BeforeEach
	void setUp() {
		registerRequest = RegisterRequestDTO.builder()
				.email("test@example.com")
				.password("ValidPass123!")
				.build();

		loginRequest = LoginRequestDTO.builder()
				.email("test@example.com")
				.password("ValidPass123!")
				.build();

		testUser = User.builder()
				.id(UUID.randomUUID())
				.email("test@example.com")
				.password("encodedPassword")
				.status(UserStatus.ACTIVE)
				.role(UserRole.OPERATIONS)
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();
	}

	@Test
	void register_When_ValidRequest_Expect_Success() {
		when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
		when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
		when(userRepository.save(any(User.class))).thenReturn(testUser);
		when(jwtService.generateToken(any(UUID.class), anyString(), anyString()))
				.thenReturn("test-token");

		AuthResponseDTO response = authService.register(registerRequest);

		assertNotNull(response);
		assertEquals("test-token", response.getToken());
		assertEquals(testUser.getId(), response.getUserId());
		assertEquals(testUser.getEmail(), response.getEmail());
		verify(userRepository).save(any(User.class));
		verify(jwtService).generateToken(any(UUID.class), anyString(), anyString());
	}

	@Test
	void register_When_EmailExists_Expect_BadRequestException() {
		when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

		assertThrows(BadRequestException.class, () -> authService.register(registerRequest));
		verify(userRepository, never()).save(any(User.class));
	}

	@Test
	void register_When_InvalidPassword_Expect_ValidationException() {
		registerRequest.setPassword("short");
		when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);

		assertThrows(ValidationException.class, () -> authService.register(registerRequest));
		verify(userRepository, never()).save(any(User.class));
	}

	@Test
	void login_When_ValidCredentials_Expect_Success() {
		when(userRepository.findByEmail(loginRequest.getEmail()))
				.thenReturn(Optional.of(testUser));
		when(passwordEncoder.matches(loginRequest.getPassword(), testUser.getPassword()))
				.thenReturn(true);
		when(jwtService.generateToken(any(UUID.class), anyString(), anyString()))
				.thenReturn("test-token");

		AuthResponseDTO response = authService.login(loginRequest);

		assertNotNull(response);
		assertEquals("test-token", response.getToken());
		assertEquals(testUser.getId(), response.getUserId());
		verify(jwtService).generateToken(any(UUID.class), anyString(), anyString());
	}

	@Test
	void login_When_UserNotFound_Expect_UnauthorizedException() {
		when(userRepository.findByEmail(loginRequest.getEmail()))
				.thenReturn(Optional.empty());

		assertThrows(UnauthorizedException.class, () -> authService.login(loginRequest));
		verify(jwtService, never()).generateToken(any(UUID.class), anyString(), anyString());
	}

	@Test
	void login_When_InvalidPassword_Expect_UnauthorizedException() {
		when(userRepository.findByEmail(loginRequest.getEmail()))
				.thenReturn(Optional.of(testUser));
		when(passwordEncoder.matches(loginRequest.getPassword(), testUser.getPassword()))
				.thenReturn(false);

		assertThrows(UnauthorizedException.class, () -> authService.login(loginRequest));
		verify(jwtService, never()).generateToken(any(UUID.class), anyString(), anyString());
	}

	@Test
	void login_When_InactiveUser_Expect_UnauthorizedException() {
		testUser.setStatus(UserStatus.INACTIVE);
		when(userRepository.findByEmail(loginRequest.getEmail()))
				.thenReturn(Optional.of(testUser));
		when(passwordEncoder.matches(loginRequest.getPassword(), testUser.getPassword()))
				.thenReturn(true);

		assertThrows(UnauthorizedException.class, () -> authService.login(loginRequest));
		verify(jwtService, never()).generateToken(any(UUID.class), anyString(), anyString());
	}

}

