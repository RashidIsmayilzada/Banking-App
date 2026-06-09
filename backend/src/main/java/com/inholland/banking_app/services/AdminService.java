package com.inholland.banking_app.services;

import com.inholland.banking_app.dtos.EmployeeCreateRequest;
import com.inholland.banking_app.dtos.EmployeeResponse;
import com.inholland.banking_app.dtos.EmployeeUpdateRequest;
import com.inholland.banking_app.exceptions.DuplicateResourceException;
import com.inholland.banking_app.models.EmployeeProfile;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.Channel;
import com.inholland.banking_app.models.enums.Role;
import com.inholland.banking_app.models.enums.TransactionType;
import com.inholland.banking_app.models.enums.AuditAction;
import com.inholland.banking_app.repositories.EmployeeProfileRepository;
import com.inholland.banking_app.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.inholland.banking_app.models.Account;
import com.inholland.banking_app.dtos.AccountResponse;
import com.inholland.banking_app.repositories.AccountRepository;
import com.inholland.banking_app.mappers.AccountMapper;
import com.inholland.banking_app.exceptions.AccountStateException;

import com.inholland.banking_app.repositories.TransactionRepository;
import com.inholland.banking_app.dtos.TransactionReversalResponse;
import com.inholland.banking_app.models.Transaction;
import com.inholland.banking_app.repositories.AuditLogRepository;
import com.inholland.banking_app.dtos.AuditLogResponse;
import com.inholland.banking_app.models.AuditLog;


@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final EmployeeProfileRepository employeeProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final TransactionRepository transactionRepository;

    private final AuditService auditService;
    private final AuditLogRepository auditLogRepository;

    @Transactional
    public EmployeeResponse createEmployee(EmployeeCreateRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already taken");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already taken");
        }
        if (employeeProfileRepository.existsByEmployeeNumber(request.getEmployeeNumber())) {
            throw new DuplicateResourceException("Employee number already taken");
        }
        User user = new User();
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.EMPLOYEE);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user = userRepository.save(user);

        EmployeeProfile profile = new EmployeeProfile();
        profile.setUser(user);
        profile.setFirstName(request.getFirstName());
        profile.setLastName(request.getLastName());
        profile.setEmployeeNumber(request.getEmployeeNumber());
        profile.setEnabled(true);
        profile.setCreatedAt(LocalDateTime.now());
        employeeProfileRepository.save(profile);

        // Record Audit Log
        auditService.record(getCurrentAdmin(), AuditAction.EMPLOYEE_CREATED, "EMPLOYEE", user.getId(), "Created employee: " + user.getUsername());

        return toResponse(user, profile);
    }

    private EmployeeResponse toResponse(User user, EmployeeProfile profile) {
        EmployeeResponse response = new EmployeeResponse();
        response.setId(user.getId());
        response.setFirstName(profile.getFirstName());
        response.setLastName(profile.getLastName());
        response.setEmail(user.getEmail());
        response.setUsername(user.getUsername());
        response.setEmployeeNumber(profile.getEmployeeNumber());
        response.setActive(user.isActive());
        response.setEnabled(profile.isEnabled());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }

    public List<EmployeeResponse> getAllEmployees() {
        return employeeProfileRepository.findAll().stream()
                .map(profile -> toResponse(profile.getUser(), profile))
                .collect(Collectors.toList());
    }

    public EmployeeResponse getEmployee(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found with ID: " + id));

        EmployeeProfile profile = employeeProfileRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Employee profile not found"));

        return toResponse(user, profile);
    }

    @Transactional
    public EmployeeResponse updateEmployee(Long id, EmployeeUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found with ID: " + id));

        EmployeeProfile profile = employeeProfileRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Employee profile not found"));

        profile.setFirstName(request.getFirstName());
        profile.setLastName(request.getLastName());

        employeeProfileRepository.save(profile);

        // Record Audit Log
        auditService.record(getCurrentAdmin(), AuditAction.EMPLOYEE_UPDATED, "EMPLOYEE", user.getId(), "Updated employee details");

        return toResponse(user, profile);
    }

    @Transactional
    public EmployeeResponse setEmployeeStatus(Long id, boolean active) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found with ID: " + id));

        user.setActive(active);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        EmployeeProfile profile = employeeProfileRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Employee profile not found"));

        // Record Audit Log
        AuditAction action = active ? AuditAction.EMPLOYEE_ENABLED : AuditAction.EMPLOYEE_DISABLED;
        auditService.record(getCurrentAdmin(), action, "EMPLOYEE", user.getId(), "Set active status to: " + active);

        return toResponse(user, profile);
    }

    @Transactional
    public void deleteEmployee(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found with ID: " + id));

        user.setActive(false);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        employeeProfileRepository.findByUser(user)
                .ifPresent(profile -> {
                    profile.setEnabled(false);
                    employeeProfileRepository.save(profile);
                });

        // Record Audit Log
        auditService.record(getCurrentAdmin(), AuditAction.EMPLOYEE_DELETED, "EMPLOYEE", user.getId(), "Soft deleted employee");
    }


    // ACCOUNT METHODS

    public List<AccountResponse> getAllAccounts() {
        return accountRepository.findAll().stream()
                .map(accountMapper::toResponse)
                .collect(Collectors.toList());
    }

    public AccountResponse getAccount(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found with ID: " + id));

        return accountMapper.toResponse(account);
    }

    @Transactional
    public AccountResponse freezeAccount(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found with ID: " + id));

        if (account.isClosed()) {
            throw new AccountStateException("Cannot freeze a closed account");
        }
        if (account.isFrozen()) {
            throw new AccountStateException("Account is already frozen");
        }

        account.markFrozen();
        accountRepository.save(account);

        // Record Audit Log
        auditService.record(getCurrentAdmin(), AuditAction.ACCOUNT_FROZEN, "ACCOUNT", account.getId(), "Froze account");

        return accountMapper.toResponse(account);
    }

    @Transactional
    public AccountResponse unfreezeAccount(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found with ID: " + id));

        if (!account.isFrozen()) {
            throw new AccountStateException("Account is not frozen");
        }

        account.unfreeze();
        accountRepository.save(account);

        // Record Audit Log
        auditService.record(getCurrentAdmin(), AuditAction.ACCOUNT_UNFROZEN, "ACCOUNT", account.getId(), "Unfroze account");

        return accountMapper.toResponse(account);
    }

    @Transactional
    public AccountResponse closeAccount(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found with ID: " + id));

        if (account.isClosed()) {
            throw new AccountStateException("Account is already closed");
        }

        account.markClosed();
        accountRepository.save(account);

        // Record Audit Log
        auditService.record(getCurrentAdmin(), AuditAction.ACCOUNT_CLOSED, "ACCOUNT", account.getId(), "Closed account");

        return accountMapper.toResponse(account);
    }


    // TRANSACTION METHODS


    @Transactional
    public TransactionReversalResponse reverseTransaction(Long id, String adminUsername) {

        Transaction original = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found with ID: " + id));

        if (original.getTransactionType() == TransactionType.REVERSAL) {
            throw new IllegalStateException("Cannot reverse a reversal transaction");
        }

        if (transactionRepository.existsByReversesTransactionId(id)) {
            throw new IllegalStateException("Transaction has already been reversed");
        }

        User admin = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new IllegalArgumentException("Admin user not found"));

        Account fromAccount = original.getFromAccount();
        Account toAccount = original.getToAccount();

        switch (original.getTransactionType()) {
            case TRANSFER -> {
                if (toAccount.isClosed()) {
                    throw new AccountStateException("Cannot reverse: destination account is closed");
                }
                if (toAccount.getBalance().compareTo(original.getAmount()) < 0) {
                    throw new IllegalStateException("Cannot reverse: destination account has insufficient funds");
                }
                toAccount.setBalance(toAccount.getBalance().subtract(original.getAmount()));
                fromAccount.setBalance(fromAccount.getBalance().add(original.getAmount()));
                accountRepository.save(toAccount);
                accountRepository.save(fromAccount);
            }
            case DEPOSIT -> {
                if (toAccount.isClosed()) {
                    throw new AccountStateException("Cannot reverse: account is closed");
                }
                if (toAccount.getBalance().compareTo(original.getAmount()) < 0) {
                    throw new IllegalStateException("Cannot reverse: account has insufficient funds");
                }
                toAccount.setBalance(toAccount.getBalance().subtract(original.getAmount()));
                accountRepository.save(toAccount);
            }
            case WITHDRAWAL -> {
                if (fromAccount.isClosed()) {
                    throw new AccountStateException("Cannot reverse: account is closed");
                }
                fromAccount.setBalance(fromAccount.getBalance().add(original.getAmount()));
                accountRepository.save(fromAccount);
            }
            default -> throw new IllegalStateException("Unsupported transaction type for reversal");
        }

        Transaction reversal = new Transaction();
        reversal.setTransactionType(TransactionType.REVERSAL);
        reversal.setFromAccount(toAccount);
        reversal.setToAccount(fromAccount);
        reversal.setAmount(original.getAmount());
        reversal.setCurrency(original.getCurrency());
        reversal.setChannel(Channel.EMPLOYEE);
        reversal.setInitiatedBy(admin);
        reversal.setCreatedAt(LocalDateTime.now());
        reversal.setDescription("Reversal of transaction #" + original.getId());
        reversal.setReversesTransaction(original);
        transactionRepository.save(reversal);

        // Record Audit Log
        auditService.record(admin, AuditAction.TRANSACTION_REVERSED, "TRANSACTION", original.getId(), "Reversed original transaction #" + original.getId());

        TransactionReversalResponse response = new TransactionReversalResponse();
        response.setOriginalTransactionId(original.getId());
        response.setReversalTransactionId(reversal.getId());
        response.setTransactionType(TransactionType.REVERSAL.name());
        response.setAmount(original.getAmount());
        response.setDescription(reversal.getDescription());
        response.setCreatedAt(reversal.getCreatedAt());
        return response;
    }


// AUDIT LOG METHODS

    public List<AuditLogResponse> getAuditLogs() {
        return auditLogRepository.findAll().stream()
                .map(this::toAuditLogResponse)
                .collect(Collectors.toList());
    }

    private AuditLogResponse toAuditLogResponse(AuditLog log) {
        AuditLogResponse response = new AuditLogResponse();
        response.setId(log.getId());
        response.setActorId(log.getActorId());
        response.setActorUsername(log.getActorUsername());

        if (log.getAction() != null) {
            response.setAction(log.getAction().name());
        }

        response.setTargetType(log.getTargetType());
        response.setTargetId(log.getTargetId());
        response.setDetails(log.getDetails());
        response.setCreatedAt(log.getCreatedAt());
        return response;
    }
    private User getCurrentAdmin() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Current authenticated admin user not found"));
    }
}