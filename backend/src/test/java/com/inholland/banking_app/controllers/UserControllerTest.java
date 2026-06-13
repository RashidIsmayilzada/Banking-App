package com.inholland.banking_app.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inholland.banking_app.dtos.ApproveCustomerRequest;
import com.inholland.banking_app.dtos.UserFilterRequest;
import com.inholland.banking_app.dtos.UserRequest;
import com.inholland.banking_app.dtos.UserResponse;
import com.inholland.banking_app.models.enums.CustomerStatus;
import com.inholland.banking_app.models.enums.Role;
import com.inholland.banking_app.security.JwtAuthenticationFilter;
import com.inholland.banking_app.services.AuthService;
import com.inholland.banking_app.services.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = UserController.class, excludeAutoConfiguration = { SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class }, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))

class UserControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private UserService userService;

        @MockitoBean
        private AuthService authService;

        private final ObjectMapper objectMapper = new ObjectMapper();

        private UserResponse userResponse;

        @BeforeEach
        void setUp() {
                userResponse = UserResponse.builder()
                                .id(1L)
                                .firstName("John")
                                .lastName("Doe")
                                .email("john@example.com")
                                .username("john_doe")
                                .phoneNumber("+31612345678")
                                .bsn("123456789")
                                .role(Role.CUSTOMER)
                                .status(CustomerStatus.PENDING_APPROVAL)
                                .hasAccounts(false)
                                .accountCount(0)
                                .accounts(Collections.emptyList())
                                .build();
        }

        // --- POST /users ---

        @Test
        @DisplayName("POST /users - should return 201 with user response when registration succeeds")
        void register_shouldReturn201_whenRegistrationSucceeds() throws Exception {
                UserRequest request = new UserRequest();
                request.setFirstName("John");
                request.setLastName("Doe");
                request.setEmail("john@example.com");
                request.setUsername("john_doe");
                request.setPassword("Secure@123");
                request.setRole(Role.CUSTOMER);
                request.setBsn("123456789");
                request.setPhoneNumber("+31612345678");

                when(authService.register(any(UserRequest.class))).thenReturn(userResponse);

                mockMvc.perform(post("/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").value(1))
                                .andExpect(jsonPath("$.email").value("john@example.com"))
                                .andExpect(jsonPath("$.username").value("john_doe"))
                                .andExpect(jsonPath("$.role").value("CUSTOMER"));
        }

        // --- GET /users ---

        @Test
        @DisplayName("GET /users - should return 200 with paginated user list")
        void getAllUsers_shouldReturn200_withUserList() throws Exception {
                Page<UserResponse> page = new PageImpl<>(List.of(userResponse), PageRequest.of(0, 10), 1);

                when(userService.getAllUsers(any(), any(UserFilterRequest.class))).thenReturn(page);

                mockMvc.perform(get("/users"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray())
                                .andExpect(jsonPath("$.content[0].id").value(1))
                                .andExpect(jsonPath("$.content[0].email").value("john@example.com"))
                                .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("GET /users - should return 200 with empty list when no users match")
        void getAllUsers_shouldReturn200_withEmptyList() throws Exception {
                Page<UserResponse> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);

                when(userService.getAllUsers(any(), any(UserFilterRequest.class))).thenReturn(emptyPage);

                mockMvc.perform(get("/users"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isEmpty())
                                .andExpect(jsonPath("$.totalElements").value(0));
        }

        // --- GET /users/{id} ---

        @Test
        @DisplayName("GET /users/{id} - should return 200 with user response when user exists")
        void getUser_shouldReturn200_whenUserExists() throws Exception {
                when(userService.getUserById(1L)).thenReturn(userResponse);

                mockMvc.perform(get("/users/1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(1))
                                .andExpect(jsonPath("$.email").value("john@example.com"))
                                .andExpect(jsonPath("$.username").value("john_doe"))
                                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"));
        }

        @Test
        @DisplayName("GET /users/{id} - should return 404 when user not found")
        void getUser_shouldReturn404_whenUserNotFound() throws Exception {
                when(userService.getUserById(99L)).thenThrow(new EntityNotFoundException("User with id 99 not found"));

                mockMvc.perform(get("/users/99"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }

        // --- PATCH /users/{id}/approval ---

        @Test
        @DisplayName("PATCH /users/{id}/approval - should return 200 with updated user when approval succeeds")
        void approveUser_shouldReturn200_whenApprovalSucceeds() throws Exception {
                ApproveCustomerRequest request = new ApproveCustomerRequest();

                UserResponse approvedResponse = UserResponse.builder()
                                .id(1L)
                                .email("john@example.com")
                                .username("john_doe")
                                .role(Role.CUSTOMER)
                                .status(CustomerStatus.APPROVED)
                                .hasAccounts(true)
                                .accountCount(2)
                                .accounts(Collections.emptyList())
                                .build();

                when(userService.getUserById(1L)).thenReturn(approvedResponse);

                mockMvc.perform(patch("/users/1/approval")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(1))
                                .andExpect(jsonPath("$.status").value("APPROVED"))
                                .andExpect(jsonPath("$.accountCount").value(2));
        }

        @Test
        @DisplayName("PATCH /users/{id}/approval - should return 404 when user not found")
        void approveUser_shouldReturn404_whenUserNotFound() throws Exception {
                ApproveCustomerRequest request = new ApproveCustomerRequest();

                doThrow(new EntityNotFoundException("user with id: 99 not found"))
                                .when(userService).approveCustomer(any(ApproveCustomerRequest.class), eq(99L));

                mockMvc.perform(patch("/users/99/approval")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }

        // --- PATCH /users/{id}/close ---

        @Test
        @DisplayName("PATCH /users/{id}/close - should return 200 with closed customer response")
        void closeUser_shouldReturn200_whenCustomerClosedSuccessfully() throws Exception {
                UserResponse closedResponse = UserResponse.builder()
                                .id(1L)
                                .email("john@example.com")
                                .role(Role.CUSTOMER)
                                .status(CustomerStatus.CLOSED)
                                .active(true)
                                .hasAccounts(false)
                                .accountCount(0)
                                .accounts(Collections.emptyList())
                                .build();

                when(userService.closeUser(1L)).thenReturn(closedResponse);

                mockMvc.perform(patch("/users/1/close"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(1))
                                .andExpect(jsonPath("$.status").value("CLOSED"));
        }

        @Test
        @DisplayName("PATCH /users/{id}/close - should return 200 with inactive employee response")
        void closeUser_shouldReturn200_whenEmployeeClosedSuccessfully() throws Exception {
                UserResponse closedEmployee = UserResponse.builder()
                                .id(2L)
                                .email("emp@bank.com")
                                .role(Role.EMPLOYEE)
                                .active(false)
                                .build();

                when(userService.closeUser(2L)).thenReturn(closedEmployee);

                mockMvc.perform(patch("/users/2/close"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(2))
                                .andExpect(jsonPath("$.active").value(false));
        }

        @Test
        @DisplayName("PATCH /users/{id}/close - should return 404 when user not found")
        void closeUser_shouldReturn404_whenUserNotFound() throws Exception {
                when(userService.closeUser(99L))
                                .thenThrow(new EntityNotFoundException("User with id 99 not found"));

                mockMvc.perform(patch("/users/99/close"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }

        // --- PATCH /users/{id}/reopen ---

        @Test
        @DisplayName("PATCH /users/{id}/reopen - should return 200 with APPROVED customer and reopened accounts")
        void reopenUser_shouldReturn200_whenCustomerReopenedSuccessfully() throws Exception {
                UserResponse reopenedResponse = UserResponse.builder()
                                .id(1L)
                                .email("john@example.com")
                                .role(Role.CUSTOMER)
                                .status(CustomerStatus.APPROVED)
                                .active(true)
                                .hasAccounts(true)
                                .accountCount(2)
                                .accounts(Collections.emptyList())
                                .build();

                when(userService.reopenUser(1L)).thenReturn(reopenedResponse);

                mockMvc.perform(patch("/users/1/reopen"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(1))
                                .andExpect(jsonPath("$.status").value("APPROVED"))
                                .andExpect(jsonPath("$.accountCount").value(2));
        }

        @Test
        @DisplayName("PATCH /users/{id}/reopen - should return 200 with active employee response")
        void reopenUser_shouldReturn200_whenEmployeeReopenedSuccessfully() throws Exception {
                UserResponse activeEmployee = UserResponse.builder()
                                .id(2L)
                                .email("emp@bank.com")
                                .role(Role.EMPLOYEE)
                                .active(true)
                                .build();

                when(userService.reopenUser(2L)).thenReturn(activeEmployee);

                mockMvc.perform(patch("/users/2/reopen"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(2))
                                .andExpect(jsonPath("$.active").value(true));
        }

        @Test
        @DisplayName("PATCH /users/{id}/reopen - should return 404 when user not found")
        void reopenUser_shouldReturn404_whenUserNotFound() throws Exception {
                when(userService.reopenUser(99L))
                                .thenThrow(new EntityNotFoundException("User with id 99 not found"));

                mockMvc.perform(patch("/users/99/reopen"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }
}
