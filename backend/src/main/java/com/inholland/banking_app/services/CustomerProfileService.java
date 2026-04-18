package com.inholland.banking_app.services;

import com.inholland.banking_app.dtos.customer.CustomerResponse;
import com.inholland.banking_app.mappers.CustomerProfileMapper;
import com.inholland.banking_app.models.CustomerProfile;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.CustomerStatus;
import com.inholland.banking_app.models.enums.Role;
import com.inholland.banking_app.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@RequiredArgsConstructor
@Service
public class CustomerProfileService {
    private final CustomerProfileMapper customerProfileMapper;
    private final UserRepository userRepository;

    public CustomerResponse getCurrentCustomerProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Authenticated user not found"));

        if (!user.isActive()) {
            throw new DisabledException("User account is inactive");
        }

        if (user.getRole() != Role.CUSTOMER) {
            throw new LockedException("User is not a customer");
        }

        CustomerProfile profile = user.getCustomerProfile();
        if (profile == null) {
            throw new NoSuchElementException("Customer profile not found");
        }

        if (profile.getStatus() != CustomerStatus.APPROVED) {
            throw new LockedException("Customer account is not approved");
        }

        return customerProfileMapper.toResponse(profile);
    }
}
