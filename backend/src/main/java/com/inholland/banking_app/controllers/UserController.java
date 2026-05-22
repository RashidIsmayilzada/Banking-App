package com.inholland.banking_app.controllers;

import com.inholland.banking_app.dtos.ApproveCustomer;
import com.inholland.banking_app.dtos.UserRequest;
import com.inholland.banking_app.dtos.UserResponse;
import com.inholland.banking_app.services.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

        @Operation(summary = "Register a new user")
        @PostMapping
        public ResponseEntity<UserResponse> registerUser(@RequestBody UserRequest request) {
                log.info("Received registration request for: {}", request.getEmail());
                return ResponseEntity.ok(userService.register(request));
        }

        @Operation(summary = "Get all users")
        @GetMapping
        @PreAuthorize("hasRole('EMPLOYEE')")
        public ResponseEntity<@NonNull Page<@NonNull UserResponse>> getAllUsers(
                        @ParameterObject @PageableDefault(size = 10, page = 0) Pageable pageable,
                        @RequestParam(required = false) String role,
                        @RequestParam(required = false) Boolean active,
                        @RequestParam(required = false) Boolean hasAccount,
                        @RequestParam(required = false) String search) {
                return ResponseEntity
                                .ok(userService.getAllUsers(pageable, role, active, hasAccount, search));
        }

        @Operation(summary = "Get user by ID")
        @ApiResponse(responseCode = "404", description = "User not found")
        @GetMapping("/{id}")
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
                        @RequestBody ApproveCustomer approveCustomer,
                        @PathVariable Long id) {

                userService.approveCustomer(approveCustomer, id);
                return ResponseEntity
                                .ok(userService.getUserById(id));
        }
}
