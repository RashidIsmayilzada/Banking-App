package com.inholland.banking_app.services;

import com.inholland.banking_app.dtos.PageMetadataResponse;
import com.inholland.banking_app.dtos.TransactionPageResponse;
import com.inholland.banking_app.dtos.TransactionRequest;
import com.inholland.banking_app.dtos.TransferResultResponse;
import com.inholland.banking_app.mappers.AtmMapper;
import com.inholland.banking_app.models.Account;
import com.inholland.banking_app.models.DailyTransferUsage;
import com.inholland.banking_app.models.Transaction;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.Channel;
import com.inholland.banking_app.models.enums.Role;
import com.inholland.banking_app.models.enums.TransactionType;
import com.inholland.banking_app.repositories.AccountRepository;
import com.inholland.banking_app.repositories.DailyTransferUsageRepository;
import com.inholland.banking_app.repositories.TransactionRepository;
import com.inholland.banking_app.repositories.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionService {

    private static final String CURRENCY = "EUR";

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final DailyTransferUsageRepository dailyTransferUsageRepository;
    private final AtmMapper transactionMapper;

    // ---------------------------------------------------------------------
    // POST /transactions
    // ---------------------------------------------------------------------

    @Transactional
    public TransferResultResponse create(TransactionRequest request, String username) {
        User initiator = findUserByUsername(username);

        if (request.getType() == null) {
            throw new IllegalArgumentException("type is required");
        }

        return switch (request.getType()) {
            case TRANSFER -> createTransfer(request, initiator);
            case DEPOSIT -> createDeposit(request, initiator);
            case WITHDRAWAL -> createWithdrawal(request, initiator);
        };
    }

    private TransferResultResponse createTransfer(TransactionRequest request, User initiator) {
        if (request.getFromAccountId() == null) {
            throw new IllegalArgumentException("fromAccountId is required for TRANSFER");
        }
        if ((request.getToAccountId() == null) == (request.getToIban() == null)) {
            throw new IllegalArgumentException(
                    "Exactly one of toAccountId or toIban is required for TRANSFER");
        }

        Account from = accountRepository.findById(request.getFromAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Source account not found"));
        ensureCanOperateOnAccount(from, initiator);
        ensureAccountActive(from);

        Account to;
        if (request.getToAccountId() != null) {
            to = accountRepository.findById(request.getToAccountId())
                    .orElseThrow(() -> new IllegalArgumentException("Destination account not found"));
        } else {
            to = accountRepository.findByIban(request.getToIban())
                    .orElseThrow(() -> new IllegalArgumentException("Destination IBAN not found"));
        }
        ensureAccountActive(to);

        if (from.getId().equals(to.getId())) {
            throw new IllegalArgumentException("Source and destination accounts must differ");
        }

        BigDecimal amount = request.getAmount();
        validateAbsoluteLimit(from, amount);
        validateDailyLimit(from, amount);

        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));
        accountRepository.save(from);
        accountRepository.save(to);

        updateDailyUsage(from, amount);

        Channel channel = initiator.getRole() == Role.EMPLOYEE ? Channel.EMPLOYEE : Channel.WEB;
        Transaction tx = recordTransaction(TransactionType.TRANSFER, from, to,
                amount, request.getDescription(), initiator, channel);

        log.info("TRANSFER {} EUR from accountId={} to accountId={} by userId={}",
                amount, from.getId(), to.getId(), initiator.getId());

        TransferResultResponse result = transactionMapper.toTransferResultResponse(tx, from);
        result.setDestinationBalance(transactionMapper.toMoneyResponse(to.getBalance()));
        return result;
    }

    private TransferResultResponse createDeposit(TransactionRequest request, User initiator) {
        if (request.getAccountId() == null) {
            throw new IllegalArgumentException("accountId is required for DEPOSIT");
        }
        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        ensureCanOperateOnAccount(account, initiator);
        ensureAccountActive(account);

        account.setBalance(account.getBalance().add(request.getAmount()));
        accountRepository.save(account);

        Transaction tx = recordTransaction(TransactionType.DEPOSIT, null, account,
                request.getAmount(), request.getDescription(), initiator, Channel.ATM);

        log.info("DEPOSIT {} EUR to accountId={} by userId={}",
                request.getAmount(), account.getId(), initiator.getId());

        return transactionMapper.toTransferResultResponse(tx, account);
    }

    private TransferResultResponse createWithdrawal(TransactionRequest request, User initiator) {
        if (request.getAccountId() == null) {
            throw new IllegalArgumentException("accountId is required for WITHDRAWAL");
        }
        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        ensureCanOperateOnAccount(account, initiator);
        ensureAccountActive(account);

        validateAbsoluteLimit(account, request.getAmount());
        validateDailyLimit(account, request.getAmount());

        account.setBalance(account.getBalance().subtract(request.getAmount()));
        accountRepository.save(account);
        updateDailyUsage(account, request.getAmount());

        Transaction tx = recordTransaction(TransactionType.WITHDRAWAL, account, null,
                request.getAmount(), request.getDescription(), initiator, Channel.ATM);

        log.info("WITHDRAWAL {} EUR from accountId={} by userId={}",
                request.getAmount(), account.getId(), initiator.getId());

        return transactionMapper.toTransferResultResponse(tx, account);
    }

    // ---------------------------------------------------------------------
    // GET /transactions
    // ---------------------------------------------------------------------

    @Transactional(readOnly = true)
    public TransactionPageResponse list(TransactionListFilters filters, Pageable pageable, String username) {
        User caller = findUserByUsername(username);
        boolean isEmployee = caller.getRole() == Role.EMPLOYEE;

        Specification<Transaction> spec = buildSpec(filters, caller, isEmployee);
        Page<Transaction> page = transactionRepository.findAll(spec, pageable);

        List<com.inholland.banking_app.dtos.TransactionResponse> items =
                page.getContent().stream().map(transactionMapper::toTransactionResponse).toList();

        PageMetadataResponse meta = new PageMetadataResponse(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
        return new TransactionPageResponse(items, meta);
    }

    private Specification<Transaction> buildSpec(TransactionListFilters f, User caller, boolean isEmployee) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (f.startDateTime() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), f.startDateTime()));
            }
            if (f.endDateTime() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), f.endDateTime()));
            }
            if (f.amountMin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), f.amountMin()));
            }
            if (f.amountMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("amount"), f.amountMax()));
            }
            if (f.amountEquals() != null) {
                predicates.add(cb.equal(root.get("amount"), f.amountEquals()));
            }
            if (f.channel() != null) {
                predicates.add(cb.equal(root.get("channel"), f.channel()));
            }
            if (f.iban() != null) {
                predicates.add(cb.or(
                        cb.equal(root.get("fromAccount").get("iban"), f.iban()),
                        cb.equal(root.get("toAccount").get("iban"), f.iban())
                ));
            }
            if (f.accountId() != null) {
                predicates.add(cb.or(
                        cb.equal(root.get("fromAccount").get("id"), f.accountId()),
                        cb.equal(root.get("toAccount").get("id"), f.accountId())
                ));
            }

            if (isEmployee) {
                if (f.userId() != null) {
                    predicates.add(cb.equal(root.get("initiatedBy").get("id"), f.userId()));
                }
            } else {
                // Customers are restricted to transactions involving them or their accounts.
                Long me = caller.getId();
                if (f.userId() != null && !f.userId().equals(me)) {
                    throw new AccessDeniedException("Customers may only view their own transactions");
                }
                Predicate isInitiator = cb.equal(root.get("initiatedBy").get("id"), me);
                Predicate fromIsMine = cb.equal(root.get("fromAccount").get("customer").get("id"), me);
                Predicate toIsMine = cb.equal(root.get("toAccount").get("customer").get("id"), me);
                predicates.add(cb.or(isInitiator, fromIsMine, toIsMine));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public record TransactionListFilters(
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            BigDecimal amountMin,
            BigDecimal amountMax,
            BigDecimal amountEquals,
            String iban,
            Long userId,
            Long accountId,
            Channel channel
    ) {}

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Authenticated user not found"));
    }

    private void ensureCanOperateOnAccount(Account account, User initiator) {
        if (initiator.getRole() == Role.EMPLOYEE) {
            return;
        }
        if (!account.getCustomer().getId().equals(initiator.getId())) {
            throw new AccessDeniedException("Account does not belong to the authenticated user");
        }
    }

    private void ensureAccountActive(Account account) {
        if (!account.isActive()) {
            throw new IllegalArgumentException("Account " + account.getIban() + " is closed");
        }
    }

    private void validateAbsoluteLimit(Account account, BigDecimal amount) {
        BigDecimal balanceAfter = account.getBalance().subtract(amount);
        if (balanceAfter.compareTo(account.getAbsoluteTransferLimit()) < 0) {
            throw new IllegalArgumentException(
                    "Transaction would exceed the absolute transfer limit. "
                            + "Current balance: " + account.getBalance()
                            + ", limit: " + account.getAbsoluteTransferLimit());
        }
    }

    private void validateDailyLimit(Account account, BigDecimal amount) {
        DailyTransferUsage todayUsage = dailyTransferUsageRepository
                .findByAccountIdAndUsageDate(account.getId(), LocalDate.now())
                .orElse(null);

        BigDecimal usedToday = todayUsage != null ? todayUsage.getTotalOutgoingAmount() : BigDecimal.ZERO;
        BigDecimal totalAfter = usedToday.add(amount);
        if (totalAfter.compareTo(account.getDailyTransferLimit()) > 0) {
            throw new IllegalArgumentException(
                    "Transaction would exceed the daily transfer limit. "
                            + "Used today: " + usedToday
                            + ", limit: " + account.getDailyTransferLimit());
        }
    }

    private void updateDailyUsage(Account account, BigDecimal amount) {
        LocalDate today = LocalDate.now();
        DailyTransferUsage usage = dailyTransferUsageRepository
                .findByAccountIdAndUsageDate(account.getId(), today)
                .orElse(null);

        if (usage == null) {
            usage = new DailyTransferUsage();
            usage.setAccount(account);
            usage.setUsageDate(today);
            usage.setTotalOutgoingAmount(BigDecimal.ZERO);
        }

        usage.setTotalOutgoingAmount(usage.getTotalOutgoingAmount().add(amount));
        usage.setUpdatedAt(LocalDateTime.now());
        dailyTransferUsageRepository.save(usage);
    }

    private Transaction recordTransaction(TransactionType type, Account fromAccount, Account toAccount,
                                          BigDecimal amount, String description, User initiatedBy,
                                          Channel channel) {
        Transaction transaction = new Transaction();
        transaction.setTransactionType(type);
        transaction.setFromAccount(fromAccount);
        transaction.setToAccount(toAccount);
        transaction.setAmount(amount);
        transaction.setCurrency(CURRENCY);
        transaction.setChannel(channel);
        transaction.setInitiatedBy(initiatedBy);
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setDescription(description == null ? "" : description);
        return transactionRepository.save(transaction);
    }
}
