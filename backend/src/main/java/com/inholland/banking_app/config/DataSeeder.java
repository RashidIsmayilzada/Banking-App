package com.inholland.banking_app.config;

import com.inholland.banking_app.models.Account;
import com.inholland.banking_app.models.CustomerProfile;
import com.inholland.banking_app.models.DailyTransferUsage;
import com.inholland.banking_app.models.EmployeeProfile;
import com.inholland.banking_app.models.Transaction;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.AccountType;
import com.inholland.banking_app.models.enums.Channel;
import com.inholland.banking_app.models.enums.CustomerStatus;
import com.inholland.banking_app.models.enums.Role;
import com.inholland.banking_app.models.enums.TransactionType;
import com.inholland.banking_app.repositories.AccountRepository;
import com.inholland.banking_app.repositories.CustomerProfileRepository;
import com.inholland.banking_app.repositories.DailyTransferUsageRepository;
import com.inholland.banking_app.repositories.EmployeeProfileRepository;
import com.inholland.banking_app.repositories.TransactionRepository;
import com.inholland.banking_app.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!test")
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final EmployeeProfileRepository employeeProfileRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final DailyTransferUsageRepository dailyTransferUsageRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already seeded — skipping");
            return;
        }

        log.info("Seeding database with test data...");

        // --- Users ---
        User alice = createUser("alice@bank.nl", "alice", Role.CUSTOMER);
        User bob   = createUser("bob@bank.nl",   "bob",   Role.CUSTOMER);
        User carol = createUser("carol@bank.nl",  "carol", Role.EMPLOYEE);

        // --- Profiles ---
        createCustomerProfile(alice, "Alice", "Jansen",  "123456781", "+31612345678");
        createCustomerProfile(bob,   "Bob",   "de Vries", "987654321", "+31698765432");
        createEmployeeProfile(carol, "Carol", "Bakker",  "EMP-001");

        // --- Accounts ---
        // Alice: checking (main) + savings
        Account aliceChecking = createAccount(alice, "NL91INHO0100000001", AccountType.CHECKING,
                new BigDecimal("5000.00"), new BigDecimal("0.00"),  new BigDecimal("2000.00"));
        Account aliceSavings  = createAccount(alice, "NL91INHO0100000002", AccountType.SAVINGS,
                new BigDecimal("12000.00"), new BigDecimal("0.00"), new BigDecimal("5000.00"));

        // Bob: checking
        Account bobChecking = createAccount(bob, "NL91INHO0100000003", AccountType.CHECKING,
                new BigDecimal("3200.00"), new BigDecimal("0.00"), new BigDecimal("1500.00"));

        // --- Transactions (historical, spread over the last few days) ---

        // 1. Alice transfers €250 to Bob (web)
        saveTx(TransactionType.TRANSFER, aliceChecking, bobChecking,
                new BigDecimal("250.00"), Channel.WEB, alice,
                LocalDateTime.now().minusDays(5).withHour(9).withMinute(14),
                "Dinner split");

        // 2. Bob transfers €100 back to Alice (web)
        saveTx(TransactionType.TRANSFER, bobChecking, aliceChecking,
                new BigDecimal("100.00"), Channel.WEB, bob,
                LocalDateTime.now().minusDays(4).withHour(11).withMinute(30),
                "My share of the groceries");

        // 3. Alice moves €1000 from checking to savings (internal transfer, web)
        saveTx(TransactionType.TRANSFER, aliceChecking, aliceSavings,
                new BigDecimal("1000.00"), Channel.WEB, alice,
                LocalDateTime.now().minusDays(3).withHour(8).withMinute(0),
                "Monthly savings deposit");

        // 4. ATM deposit into Bob's account (employee-operated)
        saveTx(TransactionType.DEPOSIT, null, bobChecking,
                new BigDecimal("500.00"), Channel.ATM, carol,
                LocalDateTime.now().minusDays(2).withHour(14).withMinute(22),
                "Cash deposit at ATM");

        // 5. ATM withdrawal from Alice's checking
        saveTx(TransactionType.WITHDRAWAL, aliceChecking, null,
                new BigDecimal("80.00"), Channel.ATM, alice,
                LocalDateTime.now().minusDays(1).withHour(17).withMinute(45),
                "Cash withdrawal");

        // 6. Employee-initiated transfer (Carol moves money for a customer)
        saveTx(TransactionType.TRANSFER, bobChecking, aliceChecking,
                new BigDecimal("200.00"), Channel.EMPLOYEE, carol,
                LocalDateTime.now().minusHours(3),
                "Standing order correction");

        // --- Daily usage entry (reflects today's outgoing from Alice's checking) ---
        DailyTransferUsage usage = new DailyTransferUsage();
        usage.setAccount(aliceChecking);
        usage.setUsageDate(LocalDate.now());
        usage.setTotalOutgoingAmount(new BigDecimal("80.00")); // today's ATM withdrawal
        usage.setUpdatedAt(LocalDateTime.now());
        dailyTransferUsageRepository.save(usage);

        log.info("Seeding complete — 3 users, 3 accounts, 6 transactions");
    }

    private User createUser(String email, String username, Role role) {
        User u = new User();
        u.setEmail(email);
        u.setUsername(username);
        u.setPasswordHash(passwordEncoder.encode("Test1234!"));
        u.setRole(role);
        u.setActive(true);
        u.setCreatedAt(LocalDateTime.now().minusDays(30));
        u.setUpdatedAt(LocalDateTime.now().minusDays(30));
        return userRepository.save(u);
    }

    private void createCustomerProfile(User user, String firstName, String lastName,
                                        String bsn, String phone) {
        CustomerProfile p = new CustomerProfile();
        p.setUser(user);
        p.setFirstName(firstName);
        p.setLastName(lastName);
        p.setBsn(bsn);
        p.setPhoneNumber(phone);
        p.setStatus(CustomerStatus.APPROVED);
        p.setRegisteredAt(LocalDateTime.now().minusDays(30));
        customerProfileRepository.save(p);
    }

    private void createEmployeeProfile(User user, String firstName, String lastName,
                                        String employeeNumber) {
        EmployeeProfile p = new EmployeeProfile();
        p.setUser(user);
        p.setFirstName(firstName);
        p.setLastName(lastName);
        p.setEmployeeNumber(employeeNumber);
        p.setEnabled(true);
        p.setCreatedAt(LocalDateTime.now().minusDays(30));
        employeeProfileRepository.save(p);
    }

    private Account createAccount(User customer, String iban, AccountType type,
                                   BigDecimal balance, BigDecimal absLimit, BigDecimal dailyLimit) {
        Account a = new Account();
        a.setCustomer(customer);
        a.setIban(iban);
        a.setAccountType(type);
        a.setBalance(balance);
        a.setAbsoluteTransferLimit(absLimit);
        a.setDailyTransferLimit(dailyLimit);
        a.setActive(true);
        a.setCreatedAt(LocalDateTime.now().minusDays(30));
        return accountRepository.save(a);
    }

    private void saveTx(TransactionType type, Account from, Account to,
                         BigDecimal amount, Channel channel, User initiatedBy,
                         LocalDateTime createdAt, String description) {
        Transaction tx = new Transaction();
        tx.setTransactionType(type);
        tx.setFromAccount(from);
        tx.setToAccount(to);
        tx.setAmount(amount);
        tx.setCurrency("EUR");
        tx.setChannel(channel);
        tx.setInitiatedBy(initiatedBy);
        tx.setCreatedAt(createdAt);
        tx.setDescription(description);
        transactionRepository.save(tx);
    }
}
