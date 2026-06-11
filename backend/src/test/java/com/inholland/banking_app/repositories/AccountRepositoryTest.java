package com.inholland.banking_app.repositories;

import com.inholland.banking_app.models.Account;
import com.inholland.banking_app.models.CustomerProfile;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.AccountStatus;
import com.inholland.banking_app.models.enums.AccountType;
import com.inholland.banking_app.models.enums.CustomerStatus;
import com.inholland.banking_app.models.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the custom searchCheckingByCustomerName query against H2.
 * The derived finders (findByCustomerId/Username) are Spring Data generated and
 * are exercised through the functional tests, so they are not unit-tested here.
 */
@DataJpaTest
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1")
class AccountRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void searchCheckingByCustomerName_matchesActiveCheckingByFirstOrLastName_caseInsensitive() {
        User alice = buildCustomerWithProfile("alice", "Alice", "Anderson", "100200300");
        accountRepository.save(buildAccount(alice, "NL01INHO0000000001", AccountType.CHECKING, AccountStatus.ACTIVE));

        List<Account> byFirstName = accountRepository.searchCheckingByCustomerName("ALI");
        List<Account> byLastName = accountRepository.searchCheckingByCustomerName("nderso");

        assertEquals(1, byFirstName.size());
        assertEquals("NL01INHO0000000001", byFirstName.get(0).getIban());
        assertEquals(1, byLastName.size());
    }

    @Test
    void searchCheckingByCustomerName_excludesSavingsAndClosedAccounts() {
        User alice = buildCustomerWithProfile("alice", "Alice", "Anderson", "100200300");
        accountRepository.save(buildAccount(alice, "NL01INHO0000000001", AccountType.SAVINGS, AccountStatus.ACTIVE));
        accountRepository.save(buildAccount(alice, "NL01INHO0000000002", AccountType.CHECKING, AccountStatus.CLOSED));

        assertTrue(accountRepository.searchCheckingByCustomerName("alice").isEmpty());
    }

    private User buildCustomerWithProfile(String username, String firstName, String lastName, String bsn) {
        User customer = new User();
        customer.setEmail(username + "@bank.com");
        customer.setUsername(username);
        customer.setPasswordHash("hashed");
        customer.setRole(Role.CUSTOMER);
        customer.setActive(true);
        customer.setCreatedAt(LocalDateTime.now());
        customer.setUpdatedAt(LocalDateTime.now());

        CustomerProfile profile = new CustomerProfile();
        profile.setUser(customer);
        profile.setFirstName(firstName);
        profile.setLastName(lastName);
        profile.setBsn(bsn);
        profile.setPhoneNumber("+31600000000");
        profile.setStatus(CustomerStatus.APPROVED);
        profile.setRegisteredAt(LocalDateTime.now());
        customer.setCustomerProfile(profile);
        return userRepository.save(customer);
    }

    private Account buildAccount(User customer, String iban, AccountType type, AccountStatus status) {
        Account account = new Account();
        account.setCustomer(customer);
        account.setIban(iban);
        account.setAccountType(type);
        account.setBalance(new BigDecimal("1000.00"));
        account.setAbsoluteTransferLimit(new BigDecimal("5000.00"));
        account.setDailyTransferLimit(new BigDecimal("2000.00"));
        account.setStatus(status);
        account.setCreatedAt(LocalDateTime.now());
        return account;
    }
}
