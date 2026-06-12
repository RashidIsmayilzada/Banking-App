package com.inholland.banking_app.services;

import com.inholland.banking_app.dtos.ApproveCustomerRequest;
import com.inholland.banking_app.dtos.UserFilterRequest;
import com.inholland.banking_app.dtos.UserResponse;
import com.inholland.banking_app.mappers.UserResponseMapper;
import com.inholland.banking_app.models.*;
import com.inholland.banking_app.models.enums.CustomerStatus;
import com.inholland.banking_app.models.enums.Role;
import com.inholland.banking_app.repositories.TransactionRepository;
import com.inholland.banking_app.repositories.UserRepository;
import com.inholland.banking_app.specifications.UserSpecification;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.inholland.banking_app.models.enums.CustomerStatus.APPROVED;

@RequiredArgsConstructor
@Service
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final AccountService accountService;
    private final UserResponseMapper userResponseMapper;
    private final TransactionRepository transactionRepository;

    public Page<UserResponse> getAllUsers(Pageable pageable, UserFilterRequest userFilterRequest) {
        return userRepository.findAll(UserSpecification.fromFilter(userFilterRequest), pageable)
                .map(userResponseMapper::toUserResponse);
    }

    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User with id " + userId + " not found"));
        return userResponseMapper.toUserResponse(user);
    }

    @Transactional
    public void approveCustomer(ApproveCustomerRequest approveCustomerRequest, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("user with id: " + userId + " not found"));

        CustomerProfile customerProfile = user.getCustomerProfile();
        if (customerProfile == null) {
            throw new EntityNotFoundException("Customer profile not found for user: " + user.getUsername());
        }

        applyStatusTransition(user, customerProfile, approveCustomerRequest.getStatus(),
                approveCustomerRequest.getCheckingAbsoluteLimit(),
                approveCustomerRequest.getCheckingDailyLimit(), approveCustomerRequest.getSavingsDailyLimit());
    }

    // Applies an approval status change activating the customer
    // and creating their default accounts the first time they become APPROVED.
    private void applyStatusTransition(User user, CustomerProfile customerProfile, CustomerStatus newStatus,
                                       BigDecimal checkingAbsoluteLimit, BigDecimal checkingDailyLimit,
                                       BigDecimal savingsDailyLimit) {
        CustomerStatus previousStatus = customerProfile.getStatus();
        customerProfile.setStatus(newStatus);

        if (newStatus == APPROVED && previousStatus != APPROVED) {
            user.setActive(true);
            if (accountService.hasNoAccounts(user)) {
                accountService.createDefaultAccounts(user, checkingAbsoluteLimit, checkingDailyLimit, savingsDailyLimit);
            }
        }
    }

    public BigDecimal getDailyOutgoingAmount(String iban) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(23, 59, 59, 999999999);

        return transactionRepository
                .sumOutgoingAmountByAccountIbanAndDate(iban, startOfDay, endOfDay);
    }

    @Transactional
    public UserResponse closeUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User with id " + userId + " not found"));
        if (user.getRole() == Role.EMPLOYEE ) {
            user.setActive(false);
            userRepository.save(user);
        } else {
            CustomerProfile customerProfile = user.getCustomerProfile();
            if (customerProfile == null) {
                throw new EntityNotFoundException("Customer profile not found for user: " + user.getUsername());
            }
            customerProfile.setStatus(CustomerStatus.CLOSED);
            accountService.closeAllAccounts(user);
        }
        return userResponseMapper.toUserResponse(user);
    }


    @Transactional
    public UserResponse reopenUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User with id " + userId + " not found"));
        if (user.getRole() == Role.EMPLOYEE) {
            user.setActive(true);
            userRepository.save(user);
        } else {
            CustomerProfile customerProfile = user.getCustomerProfile();
            if (customerProfile == null) {
                throw new EntityNotFoundException("Customer profile not found for user: " + user.getUsername());
            }
            customerProfile.setStatus(CustomerStatus.APPROVED);
            accountService.reopenAllAccounts(user);
        }
        return userResponseMapper.toUserResponse(user);
    }

    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));
    }
}
