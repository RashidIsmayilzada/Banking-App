package com.inholland.banking_app.specifications;

import com.inholland.banking_app.dtos.UserFilterRequest;
import com.inholland.banking_app.models.Account;
import com.inholland.banking_app.models.CustomerProfile;
import com.inholland.banking_app.models.EmployeeProfile;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.AccountStatus;
import com.inholland.banking_app.models.enums.AccountType;
import com.inholland.banking_app.models.enums.CustomerStatus;
import com.inholland.banking_app.models.enums.Role;
import com.inholland.banking_app.repositories.AccountRepository;
import com.inholland.banking_app.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1")
class UserSpecificationTest {

    @Autowired private UserRepository userRepository;
    @Autowired private AccountRepository accountRepository;

    private static final Pageable PAGE = PageRequest.of(0, 20);

    // only the approved customer is referenced after setUp (for account seeding)
    private User approvedCustomer;

    @BeforeEach
    void setUp() {
        approvedCustomer = saveCustomer("alice@bank.com", "alice_j", "Alice", "Johnson",
                "123456781", CustomerStatus.APPROVED, true);
        saveCustomer("bob@bank.com",   "bob_s",   "Bob",   "Smith",
                "123456782", CustomerStatus.PENDING_APPROVAL, true);
        saveCustomer("carol@bank.com", "carol_d", "Carol", "Davis",
                "123456783", CustomerStatus.CLOSED, true);
        saveCustomer("dave@bank.com",  "dave_w",  "Dave",  "Wilson",
                "123456784", CustomerStatus.PENDING_APPROVAL, false);
        saveEmployee("emp@bank.com",   "emp01",   "Jane",  "Doe");

        saveAccount(approvedCustomer, "NL10INHO0000000011", AccountType.CHECKING);
        saveAccount(approvedCustomer, "NL20INHO0000000012", AccountType.SAVINGS);
    }

    // ── hasRole ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("hasRole(CUSTOMER) - returns only customers")
    void fromFilter_shouldReturnOnlyCustomers_whenRoleIsCustomer() {
        Page<User> result = query(filter(f -> f.setRole("CUSTOMER")));

        assertThat(result.getTotalElements()).isEqualTo(4);
        assertThat(result.getContent()).allMatch(u -> u.getRole() == Role.CUSTOMER);
    }

    @Test
    @DisplayName("hasRole(EMPLOYEE) - returns only employees")
    void fromFilter_shouldReturnOnlyEmployees_whenRoleIsEmployee() {
        Page<User> result = query(filter(f -> f.setRole("EMPLOYEE")));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("emp@bank.com");
    }

    @Test
    @DisplayName("hasRole(null) - returns all users")
    void fromFilter_shouldReturnAllUsers_whenRoleIsNull() {
        assertThat(query(new UserFilterRequest()).getTotalElements()).isEqualTo(5);
    }

    // ── hasActive ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("hasActive(true) - returns only active users")
    void fromFilter_shouldReturnActiveUsers_whenActiveIsTrue() {
        Page<User> result = query(filter(f -> f.setActive(true)));

        assertThat(result.getTotalElements()).isEqualTo(4);
        assertThat(result.getContent()).allMatch(User::isActive);
    }

    @Test
    @DisplayName("hasActive(false) - returns only inactive users")
    void fromFilter_shouldReturnInactiveUsers_whenActiveIsFalse() {
        Page<User> result = query(filter(f -> f.setActive(false)));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("dave@bank.com");
    }

    @Test
    @DisplayName("hasActive(null) - returns all users regardless of active flag")
    void fromFilter_shouldReturnAll_whenActiveIsNull() {
        assertThat(query(new UserFilterRequest()).getTotalElements()).isEqualTo(5);
    }

    // ── hasAccount ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("hasAccount(true) - returns only users who own at least one account")
    void fromFilter_shouldReturnUsersWithAccounts_whenHasAccountIsTrue() {
        Page<User> result = query(filter(f -> f.setHasAccount(true)));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("alice@bank.com");
    }

    @Test
    @DisplayName("hasAccount(false) - returns only users with no accounts")
    void fromFilter_shouldReturnUsersWithoutAccounts_whenHasAccountIsFalse() {
        Page<User> result = query(filter(f -> f.setHasAccount(false)));

        assertThat(result.getTotalElements()).isEqualTo(4);
        assertThat(result.getContent()).noneMatch(u -> u.getEmail().equals("alice@bank.com"));
    }

    // ── hasCustomerStatus ────────────────────────────────────────────────────

    @Test
    @DisplayName("hasCustomerStatus(APPROVED) - returns only approved customers")
    void fromFilter_shouldReturnApprovedCustomers_whenStatusIsApproved() {
        Page<User> result = query(filter(f -> f.setStatus("APPROVED")));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("alice@bank.com");
    }

    @Test
    @DisplayName("hasCustomerStatus(PENDING_APPROVAL) - returns all pending customers")
    void fromFilter_shouldReturnPendingCustomers_whenStatusIsPendingApproval() {
        Page<User> result = query(filter(f -> f.setStatus("PENDING_APPROVAL")));

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).extracting(User::getEmail)
                .containsExactlyInAnyOrder("bob@bank.com", "dave@bank.com");
    }

    @Test
    @DisplayName("hasCustomerStatus(CLOSED) - returns only closed customers")
    void fromFilter_shouldReturnClosedCustomers_whenStatusIsClosed() {
        Page<User> result = query(filter(f -> f.setStatus("CLOSED")));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("carol@bank.com");
    }

    @Test
    @DisplayName("hasCustomerStatus(null) - returns all users")
    void fromFilter_shouldReturnAll_whenStatusIsNull() {
        assertThat(query(new UserFilterRequest()).getTotalElements()).isEqualTo(5);
    }

    // ── containsSearch ───────────────────────────────────────────────────────

    @Test
    @DisplayName("search by partial email - matches correct user")
    void fromFilter_shouldMatchByEmail_whenSearchContainsEmailPart() {
        Page<User> result = query(filter(f -> f.setSearch("alice@bank")));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("alice@bank.com");
    }

    @Test
    @DisplayName("search by username - matches correct user")
    void fromFilter_shouldMatchByUsername_whenSearchContainsUsernamePart() {
        Page<User> result = query(filter(f -> f.setSearch("bob_s")));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("bob@bank.com");
    }

    @Test
    @DisplayName("search by first name (case-insensitive) - matches correct customer")
    void fromFilter_shouldMatchByFirstName_caseInsensitive() {
        Page<User> result = query(filter(f -> f.setSearch("CAROL")));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("carol@bank.com");
    }

    @Test
    @DisplayName("search by last name (case-insensitive) - matches correct customer")
    void fromFilter_shouldMatchByLastName_caseInsensitive() {
        Page<User> result = query(filter(f -> f.setSearch("JOHNSON")));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("alice@bank.com");
    }

    @Test
    @DisplayName("search by IBAN - matches the account owner")
    void fromFilter_shouldMatchByIban_whenSearchContainsIbanPart() {
        Page<User> result = query(filter(f -> f.setSearch("NL10INHO")));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("alice@bank.com");
    }

    @Test
    @DisplayName("search with no match - returns empty result")
    void fromFilter_shouldReturnEmpty_whenSearchMatchesNothing() {
        Page<User> result = query(filter(f -> f.setSearch("zzznomatch")));

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("search with blank string - returns all users (blank treated as no filter)")
    void fromFilter_shouldReturnAll_whenSearchIsBlank() {
        assertThat(query(filter(f -> f.setSearch("   "))).getTotalElements()).isEqualTo(5);
    }

    // ── combined filters ─────────────────────────────────────────────────────

    @Test
    @DisplayName("role=CUSTOMER + status=APPROVED - returns only approved customers")
    void fromFilter_shouldNarrowToApprovedCustomers_whenRoleAndStatusCombined() {
        Page<User> result = query(filter(f -> {
            f.setRole("CUSTOMER");
            f.setStatus("APPROVED");
        }));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("alice@bank.com");
    }

    @Test
    @DisplayName("role=CUSTOMER + active=true + hasAccount=false - returns active customers without accounts")
    void fromFilter_shouldReturnActiveCustomersWithoutAccounts_whenCombinedFilters() {
        Page<User> result = query(filter(f -> {
            f.setRole("CUSTOMER");
            f.setActive(true);
            f.setHasAccount(false);
        }));

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).extracting(User::getEmail)
                .containsExactlyInAnyOrder("bob@bank.com", "carol@bank.com");
    }

    @Test
    @DisplayName("role=CUSTOMER + status=PENDING_APPROVAL + active=false - returns only inactive pending customers")
    void fromFilter_shouldReturnInactivePendingCustomers_whenAllFiltersCombined() {
        Page<User> result = query(filter(f -> {
            f.setRole("CUSTOMER");
            f.setStatus("PENDING_APPROVAL");
            f.setActive(false);
        }));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("dave@bank.com");
    }

    @Test
    @DisplayName("role=CUSTOMER + search=email - search is scoped to customers only")
    void fromFilter_shouldScopeSearchToCustomers_whenRoleAndSearchCombined() {
        Page<User> result = query(filter(f -> {
            f.setRole("CUSTOMER");
            f.setSearch("bank.com");
        }));

        assertThat(result.getTotalElements()).isEqualTo(4);
        assertThat(result.getContent()).allMatch(u -> u.getRole() == Role.CUSTOMER);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Page<User> query(UserFilterRequest filter) {
        return userRepository.findAll(UserSpecification.fromFilter(filter), PAGE);
    }

    @FunctionalInterface
    private interface FilterConfigurer { void configure(UserFilterRequest f); }

    private UserFilterRequest filter(FilterConfigurer configurer) {
        UserFilterRequest f = new UserFilterRequest();
        configurer.configure(f);
        return f;
    }

    private User saveCustomer(String email, String username, String firstName, String lastName,
                               String bsn, CustomerStatus status, boolean active) {
        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPasswordHash("hashed");
        user.setRole(Role.CUSTOMER);
        user.setActive(active);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        CustomerProfile profile = new CustomerProfile();
        profile.setFirstName(firstName);
        profile.setLastName(lastName);
        profile.setBsn(bsn);
        profile.setPhoneNumber("+31600000000");
        profile.setStatus(status);
        profile.setRegisteredAt(LocalDateTime.now());
        profile.setUser(user);
        user.setCustomerProfile(profile);

        return userRepository.save(user);
    }

    private User saveEmployee(String email, String username, String firstName, String lastName) {
        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPasswordHash("hashed");
        user.setRole(Role.EMPLOYEE);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        EmployeeProfile profile = new EmployeeProfile();
        profile.setFirstName(firstName);
        profile.setLastName(lastName);
        profile.setEmployeeNumber("EMP001");
        profile.setEnabled(true);
        profile.setCreatedAt(LocalDateTime.now());
        profile.setUser(user);
        user.setEmployeeProfile(profile);

        return userRepository.save(user);
    }

    private void saveAccount(User owner, String iban, AccountType type) {
        Account account = new Account();
        account.setIban(iban);
        account.setCustomer(owner);
        account.setAccountType(type);
        account.setBalance(BigDecimal.ZERO);
        account.setAbsoluteTransferLimit(BigDecimal.ZERO);
        account.setDailyTransferLimit(new BigDecimal("1000.00"));
        account.setStatus(AccountStatus.ACTIVE);
        account.setCreatedAt(LocalDateTime.now());
        accountRepository.save(account);
    }
}
