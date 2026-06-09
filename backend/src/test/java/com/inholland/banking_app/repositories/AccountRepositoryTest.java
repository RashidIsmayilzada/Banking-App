package com.inholland.banking_app.repositories;

import com.inholland.banking_app.models.Account;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.AccountStatus;
import com.inholland.banking_app.models.enums.AccountType;
import com.inholland.banking_app.models.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1")
class AccountRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    private User customer1;
    private User customer2;

    @BeforeEach
    void setUp() {
        customer1 = new User();
        customer1.setEmail("customer1@bank.com");
        customer1.setUsername("customer1");
        customer1.setPasswordHash("hashed");
        customer1.setRole(Role.CUSTOMER);
        customer1.setActive(true);
        customer1.setCreatedAt(LocalDateTime.now());
        customer1.setUpdatedAt(LocalDateTime.now());
        customer1 = userRepository.save(customer1);

        customer2 = new User();
        customer2.setEmail("customer2@bank.com");
        customer2.setUsername("customer2");
        customer2.setPasswordHash("hashed");
        customer2.setRole(Role.CUSTOMER);
        customer2.setActive(true);
        customer2.setCreatedAt(LocalDateTime.now());
        customer2.setUpdatedAt(LocalDateTime.now());
        customer2 = userRepository.save(customer2);

        accountRepository.save(buildAccount(customer1, "NL91ABNA0417164300"));
        accountRepository.save(buildAccount(customer1, "NL91ABNA0417164301"));
        accountRepository.save(buildAccount(customer2, "NL91ABNA0417164302"));
    }

    @Test
    @DisplayName("findByCustomerId() - should return only accounts belonging to the given customer")
    void findByCustomerId_shouldReturnOnlyThatCustomersAccounts() {
        Page<Account> result = accountRepository.findByCustomerId(customer1.getId(), PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .allMatch(a -> a.getCustomer().getId().equals(customer1.getId()));
    }

    @Test
    @DisplayName("findByCustomerId() - should return empty page when customer has no accounts")
    void findByCustomerId_shouldReturnEmptyPage_whenNoAccounts() {
        User customer3 = new User();
        customer3.setEmail("customer3@bank.com");
        customer3.setUsername("customer3");
        customer3.setPasswordHash("hashed");
        customer3.setRole(Role.CUSTOMER);
        customer3.setActive(true);
        customer3.setCreatedAt(LocalDateTime.now());
        customer3.setUpdatedAt(LocalDateTime.now());
        customer3 = userRepository.save(customer3);

        Page<Account> result = accountRepository.findByCustomerId(customer3.getId(), PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("findByCustomerUsername() - should return only accounts belonging to the given username")
    void findByCustomerUsername_shouldReturnOnlyThatCustomersAccounts() {
        Page<Account> result = accountRepository.findByCustomerUsername("customer1", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .allMatch(a -> a.getCustomer().getUsername().equals("customer1"));
    }

    private Account buildAccount(User customer, String iban) {
        Account account = new Account();
        account.setCustomer(customer);
        account.setIban(iban);
        account.setAccountType(AccountType.CHECKING);
        account.setBalance(new BigDecimal("1000.00"));
        account.setAbsoluteTransferLimit(new BigDecimal("5000.00"));
        account.setDailyTransferLimit(new BigDecimal("2000.00"));
        account.setStatus(AccountStatus.ACTIVE);
        account.setCreatedAt(LocalDateTime.now());
        return account;
    }
}
