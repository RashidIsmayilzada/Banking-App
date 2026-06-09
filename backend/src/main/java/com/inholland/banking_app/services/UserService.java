package com.inholland.banking_app.services;

import com.inholland.banking_app.dtos.ApproveCustomerRequest;
import com.inholland.banking_app.dtos.UserFilterRequest;
import com.inholland.banking_app.dtos.UserResponse;
import com.inholland.banking_app.mappers.UserResponseMapper;
import com.inholland.banking_app.models.*;
import com.inholland.banking_app.models.enums.Role;
import com.inholland.banking_app.models.enums.CustomerStatus;
import com.inholland.banking_app.models.enums.AccountType;
import com.inholland.banking_app.models.factory.AccountFactory;
import com.inholland.banking_app.repositories.AccountRepository;
import com.inholland.banking_app.repositories.TransactionRepository;
import com.inholland.banking_app.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.inholland.banking_app.models.enums.CustomerStatus.APPROVED;

@RequiredArgsConstructor
@Service
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final UserResponseMapper userResponseMapper;
    private final TransactionRepository transactionRepository;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;


    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable, UserFilterRequest userFilterRequest) {
        return userRepository.findAll(buildUserFilter(userFilterRequest), pageable)
                .map(userResponseMapper::toUserResponse);
    }

    private Specification<User> buildUserFilter(UserFilterRequest userFilterRequest) {
        return Specification.allOf(
                hasRole(userFilterRequest.getRole()),
                hasActive(userFilterRequest.getActive()),
                hasAccount(userFilterRequest.getHasAccount()),
                hasCustomerStatus(userFilterRequest.getStatus()),
                containsSearch(userFilterRequest.getSearch()));
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

    private Specification<User> hasCustomerStatus(String status) {
        return (root, query, criteriaBuilder) -> {
            if (status == null || status.isBlank()) {
                return null;
            }
            CustomerStatus customerStatus = CustomerStatus.valueOf(status.trim().toUpperCase());
            return criteriaBuilder.equal(root.get("customerProfile").get("status"), customerStatus);
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

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User with id " + userId + " not found"));
        return userResponseMapper.toUserResponse(user);
    }

    @Transactional
    public void approveCustomer(ApproveCustomerRequest approveCustomerRequest, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("user with id: " + userId + " not found"));

        CustomerProfile customerProfile = user.getCustomerProfile();
        if (customerProfile == null) {
            throw new EntityNotFoundException("Customer profile not found for user: " + user.getUsername());
        }
        customerProfile.setStatus(approveCustomerRequest.getStatus());

        if (customerProfile.getStatus() == APPROVED) {
            createDefaultAccounts(user);

        }
    }

    public BigDecimal getDailyOutgoingAmount(String iban) {
        LocalDate startOfDay = LocalDate.now();
        LocalDate endOfDay = LocalDate.now();

        return transactionRepository
                .sumOutgoingAmountByAccountIbanAndDate(iban, startOfDay, endOfDay);
    }

    private void createDefaultAccounts(User user) {
        createAccount(user, AccountType.CHECKING);
        createAccount(user, AccountType.SAVINGS);
    }

    private void createAccount(User user, AccountType accountType) {

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

    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));
    }
}
