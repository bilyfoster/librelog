package com.onelpro.librelog.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onelpro.librelog.LibreLogApplication;
import com.onelpro.librelog.TestcontainersConfiguration;
import com.onelpro.librelog.dto.UserRequestDTO;
import com.onelpro.librelog.dto.UserResponseDTO;
import com.onelpro.librelog.enums.UserRole;
import com.onelpro.librelog.enums.UserStatus;
import com.onelpro.librelog.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for UserController.
 */
@SpringBootTest(classes = LibreLogApplication.class)
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {
	"spring.liquibase.enabled=true",
	"spring.jpa.hibernate.ddl-auto=none",
	"spring.jpa.properties.hibernate.hbm2ddl.auto=none",
	"spring.jpa.properties.hibernate.schema_update=false",
	"spring.jpa.properties.hibernate.format_sql=false"
})
class UserControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void createUser_When_ValidRequest_Expect_UserCreated() throws Exception {
        UserRequestDTO request = UserRequestDTO.builder()
                .username("testuser")
                .password("password123")
                .roles(java.util.Set.of(UserRole.ADMIN))
                .status(UserStatus.ACTIVE)
                .build();

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.roles[0]").value("ADMIN"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        assertEquals(1, userRepository.count());
    }

    @Test
    void createUser_When_InvalidRequest_Expect_BadRequest() throws Exception {
        UserRequestDTO request = UserRequestDTO.builder()
                .username("ab") // Too short
                .password("pass") // Too short
                .roles(java.util.Set.of(UserRole.ADMIN))
                .build();

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        assertEquals(0, userRepository.count());
    }

    @Test
    void getUserById_When_UserExists_Expect_UserReturned() throws Exception {
        UserRequestDTO createRequest = UserRequestDTO.builder()
                .username("testuser")
                .password("password123")
                .roles(java.util.Set.of(UserRole.ADMIN))
                .status(UserStatus.ACTIVE)
                .build();

        String responseJson = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UserResponseDTO createdUser = objectMapper.readValue(responseJson, UserResponseDTO.class);
        UUID userId = createdUser.getId();

        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.roles[0]").value("ADMIN"));
    }

    @Test
    void getUserById_When_UserNotFound_Expect_NotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/api/users/{id}", nonExistentId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllUsers_When_UsersExist_Expect_ListReturned() throws Exception {
        // Create multiple users
        UserRequestDTO user1 = UserRequestDTO.builder()
                .username("user1")
                .password("password123")
                .roles(java.util.Set.of(UserRole.ADMIN))
                .build();

        UserRequestDTO user2 = UserRequestDTO.builder()
                .username("user2")
                .password("password123")
                .roles(java.util.Set.of(UserRole.DJ))
                .build();

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user2)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void updateUser_When_ValidRequest_Expect_UserUpdated() throws Exception {
        // Create user
        UserRequestDTO createRequest = UserRequestDTO.builder()
                .username("testuser")
                .password("password123")
                .roles(java.util.Set.of(UserRole.ADMIN))
                .status(UserStatus.ACTIVE)
                .build();

        String responseJson = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UserResponseDTO createdUser = objectMapper.readValue(responseJson, UserResponseDTO.class);
        UUID userId = createdUser.getId();

        // Update user
        UserRequestDTO updateRequest = UserRequestDTO.builder()
                .username("updateduser")
                .password("newpassword123")
                .roles(java.util.Set.of(UserRole.PRODUCER))
                .status(UserStatus.INACTIVE)
                .build();

        mockMvc.perform(put("/api/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("updateduser"))
                .andExpect(jsonPath("$.roles[0]").value("PRODUCER"))
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void deleteUser_When_UserExists_Expect_UserDeleted() throws Exception {
        // Create user
        UserRequestDTO createRequest = UserRequestDTO.builder()
                .username("testuser")
                .password("password123")
                .roles(java.util.Set.of(UserRole.ADMIN))
                .status(UserStatus.ACTIVE)
                .build();

        String responseJson = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UserResponseDTO createdUser = objectMapper.readValue(responseJson, UserResponseDTO.class);
        UUID userId = createdUser.getId();

        assertEquals(1, userRepository.count());

        // Delete user
        mockMvc.perform(delete("/api/users/{id}", userId))
                .andExpect(status().isNoContent());

        assertEquals(0, userRepository.count());
    }
}

