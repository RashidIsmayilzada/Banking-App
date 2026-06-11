package com.inholland.banking_app.services;

import com.inholland.banking_app.dtos.ApproveCustomerRequest;
import com.inholland.banking_app.dtos.UserFilterRequest;
import com.inholland.banking_app.dtos.UserResponse;
import com.inholland.banking_app.mappers.UserResponseMapper;
import com.inholland.banking_app.models.*;
import com.inholland.banking_app.models.enums.AccountStatus;
import com.inholland.banking_app.models.enums.CustomerStatus;
import com.inholland.banking_app.models.enums.AccountType;
import com.inholland.banking_app.models.enums.Role;
import com.inholland.banking_app.models.factory.AccountFactory;
import com.inholland.banking_app.repositories.AccountRepository;
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
    private final AccountRepository accountRepository;
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

        CustomerStatus previousStatus = customerProfile.getStatus();
        customerProfile.setStatus(approveCustomerRequest.getStatus());

        if (customerProfile.getStatus() == APPROVED && previousStatus != APPROVED) {
            user.setActive(true);
            boolean hasNoAccounts = accountRepository.findByCustomerId(user.getId(), Pageable.unpaged()).isEmpty();
            if (hasNoAccounts) {
                createDefaultAccounts(user, approveCustomerRequest.getCheckingDailyLimit(),
                        approveCustomerRequest.getSavingsDailyLimit());
            }
        }
    }

    public BigDecimal getDailyOutgoingAmount(String iban) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(23, 59, 59, 999999999);

        return transactionRepository
                .sumOutgoingAmountByAccountIbanAndDate(iban, startOfDay, endOfDay);
    }

    private void createDefaultAccounts(User user, BigDecimal checkingDailyLimit, BigDecimal savingsDailyLimit) {
        createAccount(user, AccountType.CHECKING, checkingDailyLimit);
        createAccount(user, AccountType.SAVINGS, savingsDailyLimit);
    }

    private void createAccount(User user, AccountType accountType, BigDecimal customDailyLimit) {
        String iban = generateIban(user.getId(), accountType);
        Account account = accountType == AccountType.CHECKING
                ? AccountFactory.createCheckingAccount(user, iban)
                : AccountFactory.createSavingsAccount(user, iban);

        if (customDailyLimit != null) {
            account.setDailyTransferLimit(customDailyLimit);
        }

        user.getAccounts().add(account);
    }

    private String generateIban(Long userId, AccountType accountType) {
        long accountNumber = userId * 10 + (accountType == AccountType.CHECKING ? 1 : 2);
        String iban = String.format("NL%02dINHO%010d", accountType == AccountType.CHECKING ? 10 : 20, accountNumber);

        while (accountRepository.existsByIban(iban)) {
            accountNumber++;
            iban = String.format("NL%02dINHO%010d", accountType == AccountType.CHECKING ? 10 : 20, accountNumber);
        }

        return iban;
    }

    @Transactional
    public UserResponse closeUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User with id " + userId + " not found"));
        if (user.getRole() == Role.EMPLOYEE) {
            user.setActive(false);
            userRepository.save(user);
        } else {
            CustomerProfile customerProfile = user.getCustomerProfile();
            if (customerProfile == null) {
                throw new EntityNotFoundException("Customer profile not found for user: " + user.getUsername());
            }
            customerProfile.setStatus(CustomerStatus.CLOSED);
            for (Account account : user.getAccounts()) {
                account.setStatus(AccountStatus.CLOSED);
                account.setClosedAt(java.time.LocalDateTime.now());
            }
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
            for (Account account : user.getAccounts()) {
                account.setStatus(AccountStatus.ACTIVE);
                account.setClosedAt(null);
            }
        }
        return userResponseMapper.toUserResponse(user);
    }

    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));
    }
}
