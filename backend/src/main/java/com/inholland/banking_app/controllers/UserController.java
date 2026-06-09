package com.inholland.banking_app.controllers;

import com.inholland.banking_app.dtos.ApproveCustomerRequest;
import com.inholland.banking_app.dtos.UserFilterRequest;
import com.inholland.banking_app.dtos.UserRequest;
import com.inholland.banking_app.dtos.UserResponse;
import com.inholland.banking_app.services.AuthService;
import com.inholland.banking_app.services.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springdoc.core.annotations.ParameterObject;

@Tag(name = "Users", description = "User registration and retrieval")
@RequiredArgsConstructor
@RestController
@Slf4j
@RequestMapping("/users")
public class UserController {

        private final UserService userService;
        private final AuthService authService;

        @Operation(summary = "Register a new customer")
        @PostMapping
        public ResponseEntity<UserResponse> register(@RequestBody UserRequest request) {
                UserResponse response = authService.register(request);
                log.info("User registered: {} (role={})", response.getEmail(), response.getRole());
                return ResponseEntity.status(201).body(response);
        }

        @Operation(summary = "Get all users")
        @GetMapping
        @PreAuthorize("hasRole('EMPLOYEE')")
        public ResponseEntity<@NonNull Page<@NonNull UserResponse>> getAllUsers(
                        @ParameterObject @PageableDefault(size = 10, page = 0) Pageable pageable,
                        @ModelAttribute UserFilterRequest userFilterRequest){
                return ResponseEntity
                                .ok(userService.getAllUsers(pageable, userFilterRequest));
        }

        @Operation(summary = "Get user by ID")
        @ApiResponse(responseCode = "404", description = "User not found")
        @GetMapping("/{id}")
        @PreAuthorize("hasRole('EMPLOYEE')")
        public ResponseEntity<UserResponse> getUser(
                        @Parameter(description = "ID of the user to retrieve", example = "1") @PathVariable Long id) {

                return ResponseEntity
                                .ok(userService.getUserById(id));
        }

        @Operation(summary = "Set customer approval or denial")
        @ApiResponse(responseCode = "404", description = "User not found")
        @PatchMapping("/{id}/approval")
        @PreAuthorize("hasRole('EMPLOYEE')")
        public ResponseEntity<UserResponse> approveUser(
                        @Parameter(description = "ID of the user to set approval status", example = "approved, denied")
                        @RequestBody ApproveCustomerRequest approveCustomer,
                        @PathVariable Long id) {

                userService.approveCustomer(approveCustomer, id);
                return ResponseEntity
                                .ok(userService.getUserById(id));
        }
}
