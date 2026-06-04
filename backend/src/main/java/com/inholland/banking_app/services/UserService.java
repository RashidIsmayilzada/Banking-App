package com.inholland.banking_app.services;

import com.inholland.banking_app.dtos.ApproveCustomerRequest;
import com.inholland.banking_app.dtos.UserFilterRequest;
import com.inholland.banking_app.dtos.UserRequest;
import com.inholland.banking_app.dtos.UserResponse;
import com.inholland.banking_app.mappers.UserResponseMapper;
import com.inholland.banking_app.models.*;
import com.inholland.banking_app.models.enums.Role;
import com.inholland.banking_app.models.enums.CustomerStatus;
import com.inholland.banking_app.models.enums.AccountType;
import com.inholland.banking_app.models.factory.UserFactory;
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
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;


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
//            initializeDailyTransferUsage(user); query transaction table
        }
    }

    private BigDecimal getDailyTransferLimit(User user) {

        Transaction transaction = transactionRepository.ge
    }

    private void createDefaultAccounts(User user) {
        createAccountIfMissing(user, AccountType.CHECKING);
        createAccountIfMissing(user, AccountType.SAVINGS);
    }

//    private void createAccountIfMissing(User user, AccountType accountType) {
//        boolean accountExists = user.getAccounts().stream()
//                .anyMatch(account -> account.getAccountType() == accountType);
//
//        if (accountExists) {
//            return;
//        }
//
//        String iban = generateIban(user.getId(), accountType);
//        Account account = accountType == AccountType.CHECKING
//                ? AccountFactory.createCheckingAccount(user, iban)
//                : AccountFactory.createSavingsAccount(user, iban);
//
//        accountRepository.save(account);
//        user.getAccounts().add(account);
//    }

    private String generateIban(Long userId, AccountType accountType) {
        long accountNumber = userId * 10 + (accountType == AccountType.CHECKING ? 1 : 2);
        String iban = String.format("NL%02dINHO%010d", accountType == AccountType.CHECKING ? 10 : 20, accountNumber);

        while (accountRepository.existsByIban(iban)) {
            accountNumber++;
            iban = String.format("NL%02dINHO%010d", accountType == AccountType.CHECKING ? 10 : 20, accountNumber);
        }

        return iban;
    }


//    private User currentUser() {
//        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//
//        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() instanceof String) {
//            throw new AccessDeniedException("You must be logged in to perform this action.");
//        }
//
//        // Get username from Spring Security's UserDetails
//        org.springframework.security.core.userdetails.UserDetails userDetails = (org.springframework.security.core.userdetails.UserDetails) auth
//                .getPrincipal();
//        String username = userDetails.getUsername();
//
//        return userRepository.findByUsername(username)
//                .orElseThrow(() -> new AccessDeniedException("Authenticated user not found in database"));
//    }

    //should be in base
}
