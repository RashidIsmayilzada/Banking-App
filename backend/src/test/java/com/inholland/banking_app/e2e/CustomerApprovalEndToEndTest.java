package com.inholland.banking_app.e2e;

import com.inholland.banking_app.dtos.ApproveCustomerRequest;
import com.inholland.banking_app.models.Account;
import com.inholland.banking_app.models.CustomerProfile;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.AccountStatus;
import com.inholland.banking_app.models.enums.AccountType;
import com.inholland.banking_app.models.enums.CustomerStatus;
import com.inholland.banking_app.models.enums.Role;
import com.inholland.banking_app.repositories.AccountRepository;
import com.inholland.banking_app.repositories.CustomerProfileRepository;
import com.inholland.banking_app.repositories.TransactionRepository;
import com.inholland.banking_app.repositories.UserRepository;
import com.inholland.banking_app.services.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that approving a customer automatically gives them their two default
 * accounts (CHECKING + SAVINGS) and that re-approving is idempotent.
 */
@SpringBootTest
@ActiveProfiles("test")
class CustomerApprovalEndToEndTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerProfileRepository customerProfileRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    @AfterEach
    void cleanup() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        customerProfileRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Approving a pending customer creates one CHECKING and one SAVINGS account")
    void approve_createsDefaultAccounts() {
        User customer = persistPendingCustomer();

        userService.approveCustomer(approvedRequest(), customer.getId());

        List<Account> accounts = accountsOf(customer);
        assertThat(accounts).hasSize(2);
        assertThat(accounts).extracting(Account::getAccountType)
                .containsExactlyInAnyOrder(AccountType.CHECKING, AccountType.SAVINGS);
        assertThat(accounts).allSatisfy(a ->
                assertThat(a.getStatus()).isEqualTo(AccountStatus.ACTIVE));
    }

    @Test
    @DisplayName("Re-approving an already-approved customer does not create duplicate accounts")
    void approve_isIdempotent(ApproveCustomerRequest request ) {
        User customer = persistPendingCustomer();
        userService.approveCustomer(approvedRequest(), customer.getId());
        userService.approveCustomer(approvedRequest(), customer.getId());
        assertThat(accountsOf(customer)).hasSize(2);
    }

    // --- helpers ---

    private List<Account> accountsOf(User customer) {
        return accountRepository.findByCustomerId(customer.getId(), Pageable.unpaged()).getContent();
    }

    private ApproveCustomerRequest approvedRequest() {
        ApproveCustomerRequest request = new ApproveCustomerRequest();
        request.setCheckingAbsoluteLimit(BigDecimal.valueOf(-500));
        request.setCheckingDailyLimit(BigDecimal.valueOf(1000));
        request.setSavingsDailyLimit(BigDecimal.valueOf(1500));
        return request;
    }

    private User persistPendingCustomer() {
        LocalDateTime now = LocalDateTime.now();

        User user = new User();
        user.setEmail("approve-test@test.com");
        user.setUsername("approvetest");
        user.setPasswordHash(passwordEncoder.encode("Test1234!@#$"));
        user.setRole(Role.CUSTOMER);
        user.setActive(true);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        CustomerProfile profile = new CustomerProfile();
        profile.setUser(user);
        profile.setFirstName("Approve");
        profile.setLastName("Test");
        profile.setBsn("123456789");
        profile.setPhoneNumber("+31600000000");
        profile.setStatus(CustomerStatus.PENDING_APPROVAL);
        profile.setRegisteredAt(now);
        user.setCustomerProfile(profile);

        return userRepository.save(user);
    }
}
