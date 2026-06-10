package com.inholland.banking_app.config;

import com.inholland.banking_app.models.*;
import com.inholland.banking_app.models.enums.*;
import com.inholland.banking_app.models.factory.AccountFactory;
import com.inholland.banking_app.models.factory.TransactionFactory;
import com.inholland.banking_app.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final EmployeeProfileRepository employeeProfileRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;
    private final org.springframework.core.env.Environment environment;

    @Override
    @Transactional
    public void run(String... args) {
        if (List.of(environment.getActiveProfiles()).contains("test")) {
            log.info("[DEBUG_LOG] Skipping DataSeeder in test profile");
            return;
        }
        // Seed Employees
        seedEmployee("employee01@bank.com", "employee01", "EMP001", "Admin", "User");
        seedEmployee("employee02@bank.com", "employee02", "EMP002", "Jane", "Doe");
        seedEmployee("employee03@bank.com", "employee03", "EMP003", "Employee", "Three", "Test1234!");

        // Seed Customers
        User customer1 = seedCustomer("john.doe@gmail.com", "johndoe", "John", "Doe", "123456789", "0612345678");
        User customer2 = seedCustomer("jane.smith@gmail.com", "janesmith", "Jane", "Smith", "987654321", "0687654321");
        User customer3 = seedCustomer("bob.jones@hotmail.com", "bobjones", "Bob", "Jones", "555555555", "0655555555");

        // Seed Accounts
        Account c1Checking = seedAccount(customer1, AccountType.CHECKING, new BigDecimal("1500.00"), "NL10INHO0000000001");
        Account c1Savings = seedAccount(customer1, AccountType.SAVINGS, new BigDecimal("5000.00"), "NL20INHO0000000001");
        
        Account c2Checking = seedAccount(customer2, AccountType.CHECKING, new BigDecimal("250.00"), "NL10INHO0000000002");
        
        Account c3Checking = seedAccount(customer3, AccountType.CHECKING, new BigDecimal("10000.00"), "NL10INHO0000000003");
        Account c3Savings = seedAccount(customer3, AccountType.SAVINGS, new BigDecimal("50000.00"), "NL20INHO0000000003");

        // Seed Transactions
        seedTransaction(TransactionType.DEPOSIT, null, c1Checking, new BigDecimal("100.00"), customer1, "Initial ATM deposit");
        seedTransaction(TransactionType.TRANSFER, c1Checking, c2Checking, new BigDecimal("50.00"), customer1, "Dinner reimbursement");
        seedTransaction(TransactionType.TRANSFER, c3Checking, c1Checking, new BigDecimal("500.00"), customer3, "Monthly rent");
        seedTransaction(TransactionType.WITHDRAWAL, c1Checking, null, new BigDecimal("20.00"), customer1, "Cash withdrawal");
        seedTransaction(TransactionType.TRANSFER, c1Checking, c1Savings, new BigDecimal("200.00"), customer1, "Internal transfer to savings");
    }

    private void seedEmployee(String email, String username, String empNumber, String firstName, String lastName) {
        seedEmployee(email, username, empNumber, firstName, lastName, "Password@123");
    }

    private void seedEmployee(String email, String username, String empNumber, String firstName, String lastName, String password) {
        if (userRepository.findByEmail(email).isEmpty()) {
            log.info("[DEBUG_LOG] Seeding employee user: {}", email);
            User user = new User();
            user.setEmail(email);
            user.setUsername(username);
            user.setPasswordHash(passwordEncoder.encode(password));
            user.setRole(Role.EMPLOYEE);
            user.setActive(true);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            
            user = userRepository.save(user);

            EmployeeProfile profile = new EmployeeProfile();
            profile.setUser(user);
            profile.setFirstName(firstName);
            profile.setLastName(lastName);
            profile.setEmployeeNumber(empNumber);
            profile.setEnabled(true);
            profile.setCreatedAt(LocalDateTime.now());
            
            user.setEmployeeProfile(profile);
            userRepository.save(user);
            log.info("[DEBUG_LOG] Successfully seeded employee: {}", email);
        }
    }

    private User seedCustomer(String email, String username, String firstName, String lastName, String bsn, String phone) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            log.info("[DEBUG_LOG] Seeding customer user: {}", email);
            User user = new User();
            user.setEmail(email);
            user.setUsername(username);
            user.setPasswordHash(passwordEncoder.encode("Password@123"));
            user.setRole(Role.CUSTOMER);
            user.setActive(true);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            
            user = userRepository.save(user);

            CustomerProfile profile = new CustomerProfile();
            profile.setUser(user);
            profile.setUserId(user.getId());
            profile.setFirstName(firstName);
            profile.setLastName(lastName);
            profile.setBsn(bsn);
            profile.setPhoneNumber(phone);
            profile.setStatus(CustomerStatus.APPROVED);
            profile.setRegisteredAt(LocalDateTime.now());
            
            user.setCustomerProfile(profile);
            userRepository.save(user);
            log.info("[DEBUG_LOG] Successfully seeded customer: {}", email);
            return user;
        });
    }

    private Account seedAccount(User customer, AccountType type, BigDecimal balance, String iban) {
        return accountRepository.findById(iban).orElseGet(() -> {
            log.info("[DEBUG_LOG] Seeding account: {} for {}", iban, customer.getEmail());
            Account account = type == AccountType.CHECKING 
                ? AccountFactory.createCheckingAccount(customer, iban)
                : AccountFactory.createSavingsAccount(customer, iban);
            
            account.setBalance(balance);
            account = accountRepository.save(account);
            log.info("[DEBUG_LOG] Successfully seeded account: {}", iban);
            return account;
        });
    }

    private void seedTransaction(TransactionType type, Account from, Account to, BigDecimal amount, User initiator, String desc) {
        log.info("[DEBUG_LOG] Seeding transaction: {} - {} from {} to {}", type, amount, 
            from != null ? from.getIban() : "N/A", 
            to != null ? to.getIban() : "N/A");
            
        Transaction tx;
        if (type == TransactionType.TRANSFER) {
            tx = TransactionFactory.createTransfer(from, to, amount, initiator, Channel.WEB, desc);
        } else if (type == TransactionType.DEPOSIT) {
            tx = TransactionFactory.createDeposit(to, amount, initiator, Channel.ATM, desc);
        } else {
            tx = TransactionFactory.createWithdrawal(from, amount, initiator, Channel.ATM, desc);
        }
        
        transactionRepository.save(tx);
        log.info("[DEBUG_LOG] Successfully seeded transaction");
    }
}
