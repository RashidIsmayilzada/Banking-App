package com.inholland.banking_app.services;

import com.inholland.banking_app.dtos.AtmDepositRequest;
import com.inholland.banking_app.dtos.AtmSessionStartRequest;
import com.inholland.banking_app.dtos.AtmSessionResponse;
import com.inholland.banking_app.dtos.AtmWithdrawalRequest;
import com.inholland.banking_app.dtos.MoneyResponse;
import com.inholland.banking_app.dtos.TransactionPartyResponse;
import com.inholland.banking_app.dtos.TransactionResponse;
import com.inholland.banking_app.dtos.TransferResultResponse;
import com.inholland.banking_app.models.Account;
import com.inholland.banking_app.models.AtmSession;
import com.inholland.banking_app.models.DailyTransferUsage;
import com.inholland.banking_app.models.Transaction;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.AccountType;
import com.inholland.banking_app.models.enums.Channel;
import com.inholland.banking_app.models.enums.CustomerStatus;
import com.inholland.banking_app.models.enums.Role;
import com.inholland.banking_app.models.enums.TransactionType;
import com.inholland.banking_app.repositories.AccountRepository;
import com.inholland.banking_app.repositories.AtmSessionRepository;
import com.inholland.banking_app.repositories.DailyTransferUsageRepository;
import com.inholland.banking_app.repositories.TransactionRepository;
import com.inholland.banking_app.repositories.UserRepository;
import com.inholland.banking_app.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
@Slf4j
public class AtmService {
    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid email or password";
    private static final String CURRENCY = "EUR";

    public final UserRepository userRepository;
    public final AccountRepository accountRepository;
    public final AtmSessionRepository atmSessionRepository;
    public final TransactionRepository transactionRepository;
    public final DailyTransferUsageRepository dailyTransferUsageRepository;
    public final PasswordEncoder passwordEncoder;
    public final JwtUtil jwtUtil;

    @Transactional
    public AtmSessionResponse startSession(AtmSessionStartRequest request) {
        String email = request.getEmail() == null ? null : request.getEmail().trim();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException(INVALID_CREDENTIALS_MESSAGE));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException(INVALID_CREDENTIALS_MESSAGE);
        }

        if (!user.isActive()) {
            throw new DisabledException("User account is inactive");
        }

        if (user.getRole() != Role.CUSTOMER) {
            throw new LockedException("Only customers can use the ATM");
        }

        if (user.getCustomerProfile() == null
                || user.getCustomerProfile().getStatus() != CustomerStatus.APPROVED) {
            throw new LockedException("Customer account is not approved");
        }

        AtmSession session = new AtmSession();
        session.setCustomer(user);
        session.setStartedAt(LocalDateTime.now());
        session.setSuccessfulLogin(true);
        atmSessionRepository.save(session);

        String token = jwtUtil.generateToken(user.getUsername());

        AtmSessionResponse response = new AtmSessionResponse();
        response.setSessionId(session.getId());
        response.setSessionToken(token);
        response.setCustomerUserId(user.getId());
        response.setStartedAt(session.getStartedAt());
        response.setSuccessfulLogin(true);

        log.info("ATM session started for customer userId={}", user.getId());
        return response;
    }

    @Transactional
    public void endSession(Long sessionId, String username) {
        User user = findUserByUsername(username);
        AtmSession session = atmSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("ATM session not found"));

        if (!session.getCustomer().getId().equals(user.getId())) {
            throw new IllegalArgumentException("ATM session does not belong to the authenticated user");
        }

        session.setEndedAt(LocalDateTime.now());
        atmSessionRepository.save(session);
        log.info("ATM session {} ended for customer userId={}", sessionId, user.getId());
    }

    @Transactional
    public TransferResultResponse deposit(AtmDepositRequest request, String username) {
        User user = findUserByUsername(username);
        Account account = findAndValidateAccount(request.getAccountId(), user);

        account.setBalance(account.getBalance().add(request.getAmount()));
        accountRepository.save(account);

        Transaction transaction = recordTransaction(
                TransactionType.DEPOSIT,
                null,
                account,
                request.getAmount(),
                request.getDescription(),
                user
        );

        log.info("ATM deposit of {} EUR to account {} by userId={}",
                request.getAmount(), account.getId(), user.getId());

        return buildTransferResult(transaction, account);
    }

    @Transactional
    public TransferResultResponse withdraw(AtmWithdrawalRequest request, String username) {
        User user = findUserByUsername(username);
        Account account = findAndValidateAccount(request.getAccountId(), user);

        validateAbsoluteLimit(account, request.getAmount());
        validateDailyLimit(account, request.getAmount());

        account.setBalance(account.getBalance().subtract(request.getAmount()));
        accountRepository.save(account);

        updateDailyUsage(account, request.getAmount());

        Transaction transaction = recordTransaction(
                TransactionType.WITHDRAWAL,
                account,
                null,
                request.getAmount(),
                request.getDescription(),
                user
        );

        log.info("ATM withdrawal of {} EUR from account {} by userId={}",
                request.getAmount(), account.getId(), user.getId());

        return buildTransferResult(transaction, account);
    }


    // HELPER METHODS

    private User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Authenticated user not found"));
    }

    private Account findAndValidateAccount(Long accountId, User user) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        if (!account.getCustomer().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Account does not belong to the authenticated user");
        }

        if (!account.isActive()) {
            throw new IllegalArgumentException("Account is closed");
        }

        if (account.getAccountType() != AccountType.CHECKING) {
            throw new IllegalArgumentException("ATM transactions are only allowed on checking accounts");
        }

        return account;
    }

    private void validateAbsoluteLimit(Account account, BigDecimal amount) {
        // Balance after withdrawal must stay at or above the absolute limit
        BigDecimal balanceAfter = account.getBalance().subtract(amount);
        if (balanceAfter.compareTo(account.getAbsoluteTransferLimit()) < 0) {
            throw new IllegalArgumentException(
                    "Withdrawal would exceed the absolute transfer limit. "
                            + "Current balance: " + account.getBalance()
                            + ", limit: " + account.getAbsoluteTransferLimit());
        }
    }

    private void validateDailyLimit(Account account, BigDecimal amount) {
        // Look up how much has already been withdrawn/transferred today
        DailyTransferUsage todayUsage = dailyTransferUsageRepository
                .findByAccountIdAndUsageDate(account.getId(), LocalDate.now())
                .orElse(null);

        BigDecimal usedToday = BigDecimal.ZERO;
        if (todayUsage != null) {
            usedToday = todayUsage.getTotalOutgoingAmount();
        }

        // Check if adding this withdrawal would exceed the daily limit
        BigDecimal totalAfter = usedToday.add(amount);
        if (totalAfter.compareTo(account.getDailyTransferLimit()) > 0) {
            throw new IllegalArgumentException(
                    "Withdrawal would exceed the daily transfer limit. "
                            + "Used today: " + usedToday
                            + ", limit: " + account.getDailyTransferLimit());
        }
    }

    private void updateDailyUsage(Account account, BigDecimal amount) {
        LocalDate today = LocalDate.now();

        // Try to find an existing usage record for today
        DailyTransferUsage usage = dailyTransferUsageRepository
                .findByAccountIdAndUsageDate(account.getId(), today)
                .orElse(null);

        // If no record exists for today, create a new one
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

    private Transaction recordTransaction(TransactionType type, Account fromAccount,
                                          Account toAccount, BigDecimal amount,
                                          String description, User initiatedBy) {
        Transaction transaction = new Transaction();
        transaction.setTransactionType(type);
        transaction.setFromAccount(fromAccount);
        transaction.setToAccount(toAccount);
        transaction.setAmount(amount);
        transaction.setCurrency(CURRENCY);
        transaction.setChannel(Channel.ATM);
        transaction.setInitiatedBy(initiatedBy);
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setDescription(description);
        return transactionRepository.save(transaction);
    }

    private TransferResultResponse buildTransferResult(Transaction transaction, Account account) {
        TransferResultResponse result = new TransferResultResponse();
        result.setTransaction(mapTransaction(transaction));
        result.setSourceBalance(buildMoney(account.getBalance()));
        return result;
    }

    private TransactionResponse mapTransaction(Transaction transaction) {
        TransactionResponse dto = new TransactionResponse();
        dto.setTransactionId(transaction.getId());
        dto.setTransactionType(transaction.getTransactionType());
        dto.setAmount(buildMoney(transaction.getAmount()));
        dto.setFromAccount(mapParty(transaction.getFromAccount()));
        dto.setToAccount(mapParty(transaction.getToAccount()));
        dto.setChannel(transaction.getChannel());
        dto.setInitiatedByUserId(transaction.getInitiatedBy().getId());
        dto.setCreatedAt(transaction.getCreatedAt());
        dto.setDescription(transaction.getDescription());
        return dto;
    }

    private TransactionPartyResponse mapParty(Account account) {
        if (account == null) {
            return null;
        }
        TransactionPartyResponse party = new TransactionPartyResponse();
        party.setAccountId(account.getId());
        party.setIban(account.getIban());
        party.setName(account.getCustomer().getCustomerProfile().getFirstName()
                + " " + account.getCustomer().getCustomerProfile().getLastName());
        party.setUserId(account.getCustomer().getId());
        return party;
    }

    private MoneyResponse buildMoney(BigDecimal amount) {
        MoneyResponse money = new MoneyResponse();
        money.setAmount(amount);
        money.setCurrency(CURRENCY);
        return money;
    }
}
