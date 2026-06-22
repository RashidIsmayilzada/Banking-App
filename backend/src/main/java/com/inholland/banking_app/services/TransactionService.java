package com.inholland.banking_app.services;

import com.inholland.banking_app.dtos.TransactionFilterParams;
import com.inholland.banking_app.dtos.TransactionPageDto;
import com.inholland.banking_app.dtos.TransactionRequest;
import com.inholland.banking_app.dtos.TransactionResultDto;
import com.inholland.banking_app.dtos.TransactionReversalResponse;
import com.inholland.banking_app.exceptions.AccountStateException;
import com.inholland.banking_app.mappers.TransactionMapper;
import com.inholland.banking_app.models.Account;
import com.inholland.banking_app.models.DailyTransferUsage;
import com.inholland.banking_app.models.Transaction;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.AuditAction;
import com.inholland.banking_app.models.enums.Channel;
import com.inholland.banking_app.models.enums.Role;
import com.inholland.banking_app.models.enums.TransactionType;
import com.inholland.banking_app.models.factory.DailyTransferUsageFactory;
import com.inholland.banking_app.models.factory.TransactionFactory;
import com.inholland.banking_app.policies.TransactionPolicy;
import com.inholland.banking_app.repositories.AccountRepository;
import com.inholland.banking_app.repositories.DailyTransferUsageRepository;
import com.inholland.banking_app.repositories.TransactionRepository;
import com.inholland.banking_app.repositories.UserRepository;
import com.inholland.banking_app.specifications.TransactionSpecification;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final DailyTransferUsageRepository dailyTransferUsageRepository;
    private final UserService userService;
    private final TransactionMapper transactionMapper;
    private final TransactionPolicy transactionPolicy;
    // --Efe(Admin)
    private final UserRepository userRepository;
    // --Efe(Admin)
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public TransactionPageDto listTransactions(TransactionFilterParams params, String username) {
        // Restricts results to the caller's own transactions if they are a customer, then returns a filtered page
        User currentUser = resolveUser(username);
        log.info("[DEBUG_LOG] listTransactions for user: {}, role: {}", username, currentUser.getRole());
        
        restrictToOwnerIfCustomer(params, currentUser);
        log.info("[DEBUG_LOG] Applied userId filter: {}", params.getUserId());

        Pageable pageable = buildPageable(params);
        Specification<Transaction> spec = TransactionSpecification.fromParams(params);

        Page<Transaction> page = transactionRepository.findAll(spec, pageable);
        log.info("[DEBUG_LOG] Found {} transactions", page.getTotalElements());
        
        return transactionMapper.toPageDto(page);
    }

    @Transactional
    public TransactionResultDto createTransaction(TransactionRequest request, String username) {
        // Dispatches to the correct transaction handler based on the requested type
        User currentUser = resolveUser(username);
        return switch (request.getType()) {
            case TRANSFER -> executeTransfer(request, currentUser);
            case DEPOSIT -> executeDeposit(request, currentUser);
            case WITHDRAWAL -> executeWithdrawal(request, currentUser);
            case REVERSAL -> throw new IllegalArgumentException("cannot use this endpoint for reversals");
        };
    }

    // --Efe(Admin)
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

        auditService.record(admin, AuditAction.TRANSACTION_REVERSED, "TRANSACTION", original.getId(),
                "Reversed original transaction #" + original.getId());

        TransactionReversalResponse response = new TransactionReversalResponse();
        response.setOriginalTransactionId(original.getId());
        response.setReversalTransactionId(reversal.getId());
        response.setTransactionType(TransactionType.REVERSAL.name());
        response.setAmount(original.getAmount());
        response.setDescription(reversal.getDescription());
        response.setCreatedAt(reversal.getCreatedAt());
        return response;
    }

    private TransactionResultDto executeTransfer(TransactionRequest request, User currentUser) {
        // Validates transfer fields, resolves both accounts, moves funds, and persists the transaction
        transactionPolicy.validateTransferFields(request);

        Account from = resolveSourceAccount(request.getFromIban(), currentUser);
        Account to = resolveDestinationByIban(request.getToIban());
        transactionPolicy.validateActiveAccount(to, "Destination account is not active");
        BigDecimal amount = BigDecimal.valueOf(request.getAmount());

        applyDebit(from, amount);
        credit(to, amount);

        Transaction tx = TransactionFactory.createTransfer(from, to, amount, currentUser, determineChannel(request, currentUser), request.getDescription());
        transactionRepository.save(tx);

        return transactionMapper.toTransferResult(tx, from, to);
    }

    private TransactionResultDto executeDeposit(TransactionRequest request, User currentUser) {
        // Validates the account, credits the balance, and persists the deposit transaction
        transactionPolicy.requireIban(request, "DEPOSIT");

        Account account = resolveActiveAccount(request.getIban());
        transactionPolicy.validateAccountOwnership(account, currentUser, "You can only deposit to your own accounts");
        BigDecimal amount = BigDecimal.valueOf(request.getAmount());

        applyCredit(account, amount);

        Transaction tx = TransactionFactory.createDeposit(account, amount, currentUser, determineChannel(request, currentUser), request.getDescription());
        transactionRepository.save(tx);

        return transactionMapper.toSingleAccountResult(tx, account);
    }

    private TransactionResultDto executeWithdrawal(TransactionRequest request, User currentUser) {
        // Validates the account and ownership, debits the balance, and persists the withdrawal transaction
        transactionPolicy.requireIban(request, "WITHDRAWAL");

        Account account = resolveActiveAccount(request.getIban());
        transactionPolicy.validateAccountOwnership(account, currentUser, "You can only withdraw from your own accounts");
        BigDecimal amount = BigDecimal.valueOf(request.getAmount());

        applyDebit(account, amount);

        Transaction tx = TransactionFactory.createWithdrawal(account, amount, currentUser, determineChannel(request, currentUser), request.getDescription());
        transactionRepository.save(tx);

        return transactionMapper.toSingleAccountResult(tx, account);
    }

    // account resolution methods throw EntityNotFoundException if the specified account doesn't exist,

    private Account resolveSourceAccount(String iban, User currentUser) {
        // Finds the source account, verifying ownership and that it is an active checking account
        Account account = accountRepository.findById(iban)
                .orElseThrow(() -> new EntityNotFoundException("Source account not found"));
        transactionPolicy.validateAccountOwnership(account, currentUser, "You can only transfer from your own accounts");
        transactionPolicy.validateActiveAccount(account, "Source account is not active");
        transactionPolicy.validateCheckingAccount(account);
        return account;
    }

    private Account resolveDestinationByIban(String iban) {
        // Finds the destination account by IBAN, allowing transfers to any customer
        return accountRepository.findByIban(iban)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Destination account not found for IBAN: " + iban));
    }

    private Account resolveActiveAccount(String iban) {
        // Finds an account by IBAN and verifies it is active
        Account account = accountRepository.findById(iban)
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));
        transactionPolicy.validateActiveAccount(account, "Account is not active");
        return account;
    }

    // debit and credit methods modify the account balance and save it, but do not perform any checks themselves.

    private void applyDebit(Account account, BigDecimal amount) {
        // Runs all pre-debit checks then debits the account balance and records daily usage
        transactionPolicy.checkBalance(account, amount);
        transactionPolicy.checkDailyLimit(account, amount);
        debit(account, amount);
        updateDailyUsage(account, amount);
    }

    private void applyCredit(Account account, BigDecimal amount) {
        credit(account, amount);
    }

    private void debit(Account account, BigDecimal amount) {
        // Subtracts the amount from the account balance and saves
        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);
    }

    private void credit(Account account, BigDecimal amount) {
        // Adds the amount to the account balance and saves
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);
    }

    private void updateDailyUsage(Account account, BigDecimal amount) {
        // Creates or updates today's daily transfer usage record for the account
        LocalDate today = LocalDate.now();
        DailyTransferUsage usage = dailyTransferUsageRepository
                .findByAccountIbanAndUsageDate(account.getIban(), today)
                .orElseGet(() -> DailyTransferUsageFactory.create(account, today));
        usage.setTotalOutgoingAmount(usage.getTotalOutgoingAmount().add(amount));
        usage.setUpdatedAt(LocalDateTime.now());
        dailyTransferUsageRepository.save(usage);
    }

    // Utility Methods

    private void restrictToOwnerIfCustomer(TransactionFilterParams params, User user) {
        // Forces the filter's userId to the caller's own ID when the caller is a customer
        if (user.getRole() == Role.CUSTOMER) {
            params.setUserId(user.getId());
        }
    }

    private Pageable buildPageable(TransactionFilterParams params) {
        // Converts filter params into a Spring Pageable with the requested page, size, and sort order
        Sort sort = parseSort(params.getSort());
        int size = params.getSize() > 0 ? params.getSize() : 20;
        return PageRequest.of(params.getPage(), size, sort);
    }

    private Channel determineChannel(TransactionRequest request, User currentUser) {
        if (request.getChannel() != null) return request.getChannel();
        return currentUser.getRole() == Role.EMPLOYEE ? Channel.EMPLOYEE : Channel.WEB;
    }

    private Sort parseSort(String sortParam) {
        // Parses a "field,direction" sort string into a Spring Sort; defaults to createdAt DESC
        if (sortParam == null || sortParam.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        String[] parts = sortParam.split(",");
        String field = parts[0].trim();
        Sort.Direction direction = parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim())
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return Sort.by(direction, field);
    }

    private User resolveUser(String username) {
        return userService.getByUsername(username);
    }
}
