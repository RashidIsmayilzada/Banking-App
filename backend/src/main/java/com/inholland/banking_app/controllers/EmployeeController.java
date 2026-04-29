package com.inholland.banking_app.controllers;

import com.inholland.banking_app.dtos.*;
import com.inholland.banking_app.models.CustomerProfile;
import com.inholland.banking_app.models.Transaction;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.services.EmployeeService;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestControllerAdvice
@RestController
@Slf4j
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    // TODO: Employee can view a paginated list of all customer accounts.
    @GetMapping("/employee/customers")
    public ResponseEntity<@NonNull Page<@NonNull CustomerResponse>> getAllCustomers(
            @PageableDefault(size = 4, page = 0) Pageable pageable) {
        log.info("Retrieving customers with pagination: {}", pageable);
        return ResponseEntity.ok(employeeService.getAllCustomers(pageable));
    }

    // TODO: Add pagination
    @GetMapping("/employee/customers/pending")
    public ResponseEntity<@NonNull Page<@NonNull UserResponse>> getCustomersWithNoAccounts(
            @PageableDefault(size = 5, page = 0) Pageable pageable) {
        log.info("Retrieving customers with no accounts: {}", pageable);
        return ResponseEntity.ok(
                employeeService.customersWithoutAccounts(pageable)
        );
    }

    // TODO: Get customer accounts
    @GetMapping("/employee/customers/{customerId}/accounts")
    public ResponseEntity<@NonNull Page<@NonNull AccountResponse>> getCustomerAccounts(
            @PageableDefault(size = 20, page = 0) Pageable pageable, @PathVariable Long customerId) {
        log.info("Retrieving accounts for customer ID: {}", customerId);
        return ResponseEntity.ok(
                employeeService.getAccountByUserId(pageable, customerId)
        );
    }

    // TODO: Approve customer registration
    @PostMapping("/employee/customers/{userId}/approve")
    public  void approveCustomer(@RequestBody ApprovalRequestDTO approvalRequestDTO, @PathVariable Long userId) {
        log.info("Approving customer registration for ID: {}", userId);
        employeeService.approveCustomer(approvalRequestDTO, userId);

    }

    // TODO: employee can make transfer to customer
    @GetMapping("/employee/transfer")
    public void makeTransfer(TransferRequest transferRequest) {
        log.info("Making transfer to customer: {}", transferRequest);
//        employeeService.makeTransfer(transferRequest);
//
    }
}