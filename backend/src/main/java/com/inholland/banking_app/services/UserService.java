package com.inholland.banking_app.services;

import com.inholland.banking_app.dtos.ApproveCustomer;
import com.inholland.banking_app.dtos.UserResponse;
import com.inholland.banking_app.mappers.UserResponseMapper;
import com.inholland.banking_app.models.Account;
import com.inholland.banking_app.models.CustomerProfile;
import com.inholland.banking_app.models.DailyTransferUsage;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.Role;
import com.inholland.banking_app.models.enums.AccountType;
import com.inholland.banking_app.models.factory.AccountFactory;
import com.inholland.banking_app.repositories.AccountRepository;
import com.inholland.banking_app.repositories.DailyTransferUsageRepository;
import com.inholland.banking_app.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.inholland.banking_app.models.enums.CustomerStatus.APPROVED;

@RequiredArgsConstructor
@Service
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final DailyTransferUsageRepository dailyTransferUsageRepository;
    private final UserResponseMapper userResponseMapper;


    public Page<UserResponse> getAllUsers(Pageable pageable, String role, Boolean active, Boolean hasAccount,
            String search) {
        return userRepository.findAll(buildUserFilter(role, active, hasAccount, search), pageable)
                .map(userResponseMapper::toUserResponse);
    }

    private Specification<User> buildUserFilter(String role, Boolean active, Boolean hasAccount, String search) {
        return Specification.allOf(
                hasRole(role),
                hasActive(active),
                hasAccount(hasAccount),
                containsSearch(search));
    }

    private Specification<User> hasRole(String role) {
        return (root, query, criteriaBuilder) -> {
            if (role == null || role.isBlank()) {
                return null;
            }
            Role enumRole = Role.valueOf(role.trim().toUpperCase());
            return criteriaBuilder.equal(root.get("role"), enumRole);
        };
    }

    private Specification<User> hasActive(Boolean active) {
        return (root, query, criteriaBuilder) -> active == null
                ? null
                : criteriaBuilder.equal(root.get("active"), active);
    }

    private Specification<User> hasAccount(Boolean hasAccount) {
        return (root, query, criteriaBuilder) -> {
            if (hasAccount == null) {
                return null;
            }

            return hasAccount
                    ? criteriaBuilder.isNotEmpty(root.get("accounts"))
                    : criteriaBuilder.isEmpty(root.get("accounts"));
        };
    }

    private Specification<User> containsSearch(String search) {
        return (root, query, criteriaBuilder) -> {
            query.distinct(true);
            if (search == null || search.isBlank()) {
                return null;
            }

            String pattern = "%" + search.trim().toLowerCase() + "%";
            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("username")), pattern),
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("customerProfile").get("firstName")),
                            pattern
                    ),
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("customerProfile").get("lastName")),
                            pattern
                    )
            );
        };
    }

    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User with id " + userId + " not found"));
        return userResponseMapper.toUserResponse(user);
    }

    @Transactional
    public void approveCustomer(ApproveCustomer approveCustomer, Long userId) {
        User employee = currentUser();

        if (employee.getRole() != Role.EMPLOYEE) {
            throw new AccessDeniedException("Only employees can approve customers.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("user with id: " + userId + " not found"));

        CustomerProfile customerProfile = user.getCustomerProfile();
        if (customerProfile == null) {
            throw new EntityNotFoundException("Customer profile not found for user: " + user.getUsername());
        }
        customerProfile.setStatus(approveCustomer.getStatus());

        if (customerProfile.getStatus() == APPROVED) {
            createDefaultAccounts(user);
            initializeDailyTransferUsage(user);
        }
    }

    private void createDefaultAccounts(User user) {
        createAccountIfMissing(user, AccountType.CHECKING);
        createAccountIfMissing(user, AccountType.SAVINGS);
    }

    private void createAccountIfMissing(User user, AccountType accountType) {
        boolean accountExists = user.getAccounts().stream()
                .anyMatch(account -> account.getAccountType() == accountType);

        if (accountExists) {
            return;
        }

        String iban = generateIban(user.getId(), accountType);
        Account account = accountType == AccountType.CHECKING
                ? AccountFactory.createCheckingAccount(user, iban)
                : AccountFactory.createSavingsAccount(user, iban);

        accountRepository.save(account);
        user.getAccounts().add(account);
    }

    private String generateIban(Long userId, AccountType accountType) {
        long accountNumber = userId * 10 + (accountType == AccountType.CHECKING ? 1 : 2);
        String iban = String.format("NL%02dINHO%010d", accountType == AccountType.CHECKING ? 10 : 20, accountNumber);

        while (accountRepository.existsByIban(iban)) {
            accountNumber++;
            iban = String.format("NL%02dINHO%010d", accountType == AccountType.CHECKING ? 10 : 20, accountNumber);
        }

        return iban;
    }

    private void initializeDailyTransferUsage(User user) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        for (Account account : user.getAccounts()) {
            dailyTransferUsageRepository.findByAccountIdAndUsageDate(account.getId(), today)
                    .orElseGet(() -> {
                        DailyTransferUsage usage = new DailyTransferUsage();
                        usage.setAccount(account);
                        usage.setUsageDate(today);
                        usage.setTotalOutgoingAmount(BigDecimal.ZERO);
                        usage.setUpdatedAt(now);
                        return dailyTransferUsageRepository.save(usage);
                    });
        }
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() instanceof String) {
            throw new AccessDeniedException("You must be logged in to perform this action.");
        }

        // Get username from Spring Security's UserDetails
        org.springframework.security.core.userdetails.UserDetails userDetails = (org.springframework.security.core.userdetails.UserDetails) auth
                .getPrincipal();
        String username = userDetails.getUsername();

        // Load the actual User entity from database
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("Authenticated user not found in database"));
    }
}
