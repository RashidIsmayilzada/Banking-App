package com.inholland.banking_app.services;

import com.inholland.banking_app.dtos.*;
import com.inholland.banking_app.exceptions.ApprovalFailedException;
import com.inholland.banking_app.mappers.AccountMapper;
import com.inholland.banking_app.mappers.UserMapper;
import com.inholland.banking_app.models.Account;
import com.inholland.banking_app.models.CustomerApproval;
import com.inholland.banking_app.models.CustomerProfile;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.CustomerStatus;
import com.inholland.banking_app.repositories.AccountRepository;
import com.inholland.banking_app.repositories.CustomerApprovalRepository;
import com.inholland.banking_app.repositories.CustomerProfileRepository;
import com.inholland.banking_app.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor

public class EmployeeService {
    private final CustomerProfileRepository customerProfileRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final CustomerApprovalRepository customerApprovalRepository;

    public Page<@NonNull UserResponse> customersWithoutAccounts(Pageable pageable) {
        log.info("Retrieving customers without accounts with pagination: {}", pageable);
        Page<@NonNull User> customers = userRepository.findAllByAccountsIsEmpty(pageable);
        if (!customers.hasContent()) {
            log.error("No customers without an accounts was found");
            throw new EntityNotFoundException("No customers without an accounts was found");
        }
        log.info("Retrieved {} customers without accounts", customers.getTotalElements());
        return customers.map(userMapper::toUserResponse);
    }

    public Page<@NonNull CustomerResponse> getAllCustomers(Pageable pageable) {
        log.info("Retrieving all customers with pagination: {}", pageable);
        Page<@NonNull CustomerProfile> customers = customerProfileRepository.findAll(pageable);
        if (!customers.hasContent()) {
            log.error("No customers found");
            throw new EntityNotFoundException("No customers found");
        }
        log.info("Retrieved {} customers", customers.getTotalElements());
        return customers.map(userMapper::toCustomerResponse);
    }

    public Page<@NonNull AccountResponse> getAccountByUserId(Pageable pageable, Long customerId) {
        Optional<CustomerProfile> customerProfile = customerProfileRepository.findById(customerId);
        if (customerProfile.isEmpty()) {
            throw new EntityNotFoundException("Customer with id: " + customerId + " not found");
        }
        Page<@NonNull Account> accounts = accountRepository.findByCustomerId(pageable, customerId);

        if (accounts.isEmpty()) {
            throw new EntityNotFoundException("No accounts found for customer with name: " + customerProfile.get().getFirstName());
        }
        return accounts.map(accountMapper::toAccountResponse);

    }

    public void approveCustomer(ApprovalRequestDTO requestDTO, Long userId) {
        log.info("Processing approval for user ID: {} with decision: {}", userId, requestDTO.getDecision());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("user with id: " + userId + " not found"));

        log.info("User with id: {} found", user.getUsername());

        log.info("Beginning user registration: {} ", requestDTO.getDecision());
         CustomerProfile customerProfile = user.getCustomerProfile();
         if (customerProfile == null) {
             throw new EntityNotFoundException("Customer profile not found for user: " + user.getUsername());
         }
         customerProfile.setStatus(CustomerStatus.APPROVED);
         log.info("Customer profile status updated for user: {}", user.getUsername());

        CustomerApproval approval = CustomerApproval.builder()
                .customer(user)
                .approvedByEmployee(currentUser())
                .decision(requestDTO.getDecision())
                .note(requestDTO.getNote())
                .decidedAt(LocalDateTime.now())
                .build();
        customerApprovalRepository.save(approval);
        log.info("Approval saved for user: {}", user.getUsername());
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() instanceof String) {
            throw new AccessDeniedException("You must be logged in to perform this action.");
        }
        return (User) auth.getPrincipal();
    }

}
