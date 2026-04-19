package com.inholland.banking_app.services;

import com.inholland.banking_app.dtos.customer.CustomerAccountListResponse;
import com.inholland.banking_app.dtos.customer.CustomerAccountResponse;
import com.inholland.banking_app.dtos.customer.CustomerResponse;
import com.inholland.banking_app.mappers.CustomerAccountMapper;
import com.inholland.banking_app.mappers.CustomerProfileMapper;
import com.inholland.banking_app.models.CustomerProfile;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.CustomerStatus;
import com.inholland.banking_app.models.enums.Role;
import com.inholland.banking_app.repositories.AccountRepository;
import com.inholland.banking_app.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

@RequiredArgsConstructor
@Service
public class CustomerProfileService {
    private final CustomerProfileMapper customerProfileMapper;
    private final CustomerAccountMapper customerAccountMapper;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    public CustomerResponse getCurrentCustomerProfile(String username) {
        return customerProfileMapper.toResponse(getApprovedCustomerProfile(username));
    }

    public CustomerAccountListResponse getCurrentCustomerAccounts(String username) {
        CustomerProfile profile = getApprovedCustomerProfile(username);

        CustomerAccountListResponse response = new CustomerAccountListResponse();
        response.setCustomer(customerProfileMapper.toResponse(profile));

        List<CustomerAccountResponse> accounts = accountRepository.findByCustomerId(profile.getUserId())
                .stream()
                .map(customerAccountMapper::toResponse)
                .toList();

        response.setAccounts(accounts);
        response.setTotals(customerAccountMapper.toTotalsResponse(calculateCombinedBalance(accounts)));

        return response;
    }

    private CustomerProfile getApprovedCustomerProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Authenticated user not found"));

        validateActiveCustomer(user);

        CustomerProfile profile = user.getCustomerProfile();
        if (profile == null) {
            throw new NoSuchElementException("Customer profile not found");
        }

        if (profile.getStatus() != CustomerStatus.APPROVED) {
            throw new LockedException("Customer account is not approved");
        }

        return profile;
    }

    private void validateActiveCustomer(User user) {
        if (!user.isActive()) {
            throw new DisabledException("User account is inactive");
        }

        if (user.getRole() != Role.CUSTOMER) {
            throw new LockedException("User is not a customer");
        }
    }

    private BigDecimal calculateCombinedBalance(List<CustomerAccountResponse> accounts) {
        BigDecimal combinedBalance = BigDecimal.ZERO;

        for (CustomerAccountResponse account : accounts) {
            if (account.getBalance() != null) {
                combinedBalance = combinedBalance.add(account.getBalance());
            }
        }

        return combinedBalance;
    }
}
