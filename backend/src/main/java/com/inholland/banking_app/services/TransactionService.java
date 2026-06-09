package com.inholland.banking_app.services;

import com.inholland.banking_app.dtos.TransactionFilterParams;
import com.inholland.banking_app.dtos.TransactionPageDto;
import com.inholland.banking_app.dtos.TransactionRequest;
import com.inholland.banking_app.dtos.TransactionResultDto;
import com.inholland.banking_app.mappers.TransactionMapper;
import com.inholland.banking_app.models.Account;
import com.inholland.banking_app.models.DailyTransferUsage;
import com.inholland.banking_app.models.Transaction;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.Channel;
import com.inholland.banking_app.models.enums.Role;
import com.inholland.banking_app.models.factory.DailyTransferUsageFactory;
import com.inholland.banking_app.models.factory.TransactionFactory;
import com.inholland.banking_app.policies.TransactionPolicy;
import com.inholland.banking_app.repositories.AccountRepository;
import com.inholland.banking_app.repositories.DailyTransferUsageRepository;
import com.inholland.banking_app.repositories.TransactionRepository;
import com.inholland.banking_app.specifications.TransactionSpecification;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
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
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final DailyTransferUsageRepository dailyTransferUsageRepository;
    private final UserService userService;
    private final TransactionMapper transactionMapper;
    private final TransactionPolicy transactionPolicy;

    public TransactionPageDto listTransactions(TransactionFilterParams params, String username) {
        // Restricts results to the caller's own transactions if they are a customer, then returns a filtered page
        User currentUser = resolveUser(username);
        restrictToOwnerIfCustomer(params, currentUser);

        Pageable pageable = buildPageable(params);
        Specification<Transaction> spec = TransactionSpecification.fromParams(params);

        Page<Transaction> page = transactionRepository.findAll(spec, pageable);
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
        };
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

        Transaction tx = TransactionFactory.createTransfer(from, to, amount, currentUser, determineChannel(currentUser), request.getDescription());
        transactionRepository.save(tx);

        return transactionMapper.toTransferResult(tx, from, to);
    }

    private TransactionResultDto executeDeposit(TransactionRequest request, User currentUser) {
        // Validates the account, credits the balance, and persists the deposit transaction
        transactionPolicy.requireIban(request, "DEPOSIT");

        Account account = resolveActiveAccount(request.getIban());
        BigDecimal amount = BigDecimal.valueOf(request.getAmount());

        applyCredit(account, amount);

        Transaction tx = TransactionFactory.createDeposit(account, amount, currentUser, request.getDescription());
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

        Transaction tx = TransactionFactory.createWithdrawal(account, amount, currentUser, request.getDescription());
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
        // Runs all pre-credit checks then credits the account balance and records daily usage
        transactionPolicy.checkBalance(account, amount);
        transactionPolicy.checkDailyLimit(account, amount);
        credit(account, amount);
        updateDailyUsage(account, amount);
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
        return PageRequest.of(params.getPage(), params.getSize(), sort);
    }

    private Channel determineChannel(User currentUser) {
        // Returns EMPLOYEE channel for staff, WEB channel for customers
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
