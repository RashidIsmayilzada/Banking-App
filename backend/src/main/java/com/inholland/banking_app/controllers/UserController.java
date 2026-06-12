package com.inholland.banking_app.controllers;

import com.inholland.banking_app.dtos.ApproveCustomerRequest;
import com.inholland.banking_app.dtos.UserFilterRequest;
import com.inholland.banking_app.dtos.UserResponse;
import com.inholland.banking_app.services.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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

@Tag(name = "Users", description = "User registration and management")
@RequiredArgsConstructor
@RestController
@Slf4j
@RequestMapping("/users")
public class UserController {

        private final UserService userService;

        @Operation(summary = "Get all users (paginated)",
                   description = "Returns a paginated, filterable list of all users. Requires EMPLOYEE role.",
                   security = @SecurityRequirement(name = "bearerAuth"))
        @ApiResponses({
                @ApiResponse(responseCode = "200", description = "Paginated user list returned successfully",
                        content = @Content(schema = @Schema())),
                @ApiResponse(responseCode = "403", description = "Access denied — EMPLOYEE role required",
                        content = @Content(schema = @Schema()))
        })
        @GetMapping
        @PreAuthorize("hasRole('EMPLOYEE')")
        public ResponseEntity<@NonNull Page<@NonNull UserResponse>> getAllUsers(
                        @ParameterObject @PageableDefault(size = 10, page = 0) Pageable pageable,
                        @ModelAttribute UserFilterRequest userFilterRequest) {
                return ResponseEntity
                                .ok(userService.getAllUsers(pageable, userFilterRequest));
        }

        @Operation(summary = "Get user by ID",
                   description = "Returns a single user by their ID. Requires EMPLOYEE role.",
                   security = @SecurityRequirement(name = "bearerAuth"))
        @ApiResponses({
                @ApiResponse(responseCode = "200", description = "User found",
                        content = @Content(schema = @Schema(implementation = UserResponse.class))),
                @ApiResponse(responseCode = "403", description = "Access denied — EMPLOYEE role required",
                        content = @Content(schema = @Schema())),
                @ApiResponse(responseCode = "404", description = "User not found",
                        content = @Content(schema = @Schema()))
        })
        @GetMapping("/{id}")
        @PreAuthorize("hasRole('EMPLOYEE')")
        public ResponseEntity<UserResponse> getUser(
                        @Parameter(description = "ID of the user to retrieve", example = "1")
                        @PathVariable Long id) {
                return ResponseEntity
                                .ok(userService.getUserById(id));
        }

        @Operation(summary = "Approve or reject a customer",
                   description = "Sets the customer approval status (APPROVED / REJECTED) and optionally configures daily transfer limits. Requires EMPLOYEE role.",
                   security = @SecurityRequirement(name = "bearerAuth"))
        @ApiResponses({
                @ApiResponse(responseCode = "200", description = "Status updated; updated user returned",
                        content = @Content(schema = @Schema(implementation = UserResponse.class))),
                @ApiResponse(responseCode = "400", description = "Invalid status value in request body",
                        content = @Content(schema = @Schema())),
                @ApiResponse(responseCode = "403", description = "Access denied — EMPLOYEE role required",
                        content = @Content(schema = @Schema())),
                @ApiResponse(responseCode = "404", description = "User not found",
                        content = @Content(schema = @Schema()))
        })
        @PatchMapping("/{id}/approval")
        @PreAuthorize("hasRole('EMPLOYEE')")
        public ResponseEntity<UserResponse> approveUser(
                        @Parameter(description = "ID of the customer to approve or reject", example = "1")
                        @PathVariable Long id,
                        @RequestBody ApproveCustomerRequest approveCustomer) {
                userService.approveCustomer(approveCustomer, id);
                return ResponseEntity
                                .ok(userService.getUserById(id));
        }

        @Operation(summary = "Close a user",
                   description = "For customers: sets profile CLOSED and closes all accounts. For employees: sets active=false. Requires EMPLOYEE role.",
                   security = @SecurityRequirement(name = "bearerAuth"))
        @ApiResponses({
                @ApiResponse(responseCode = "200", description = "User closed; updated user returned",
                        content = @Content(schema = @Schema(implementation = UserResponse.class))),
                @ApiResponse(responseCode = "403", description = "Access denied — EMPLOYEE role required",
                        content = @Content(schema = @Schema())),
                @ApiResponse(responseCode = "404", description = "User not found",
                        content = @Content(schema = @Schema()))
        })
        @PatchMapping("/{id}/close")
        @PreAuthorize("hasRole('EMPLOYEE')")
        public ResponseEntity<UserResponse> closeUser(
                        @Parameter(description = "ID of the user to close", example = "1")
                        @PathVariable Long id) {
                return ResponseEntity.ok(userService.closeUser(id));
        }

        @Operation(summary = "Reopen a user",
                   description = "For customers: sets profile APPROVED and reopens all accounts. For employees: sets active=true. Requires EMPLOYEE role.",
                   security = @SecurityRequirement(name = "bearerAuth"))
        @ApiResponses({
                @ApiResponse(responseCode = "200", description = "User reopened; updated user returned",
                        content = @Content(schema = @Schema(implementation = UserResponse.class))),
                @ApiResponse(responseCode = "403", description = "Access denied — EMPLOYEE role required",
                        content = @Content(schema = @Schema())),
                @ApiResponse(responseCode = "404", description = "User not found",
                        content = @Content(schema = @Schema()))
        })
        @PatchMapping("/{id}/reopen")
        @PreAuthorize("hasRole('EMPLOYEE')")
        public ResponseEntity<UserResponse> reopenUser(
                        @Parameter(description = "ID of the user to reopen", example = "1")
                        @PathVariable Long id) {
                return ResponseEntity.ok(userService.reopenUser(id));
        }
}
