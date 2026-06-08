package com.inholland.banking_app.policies;

import com.inholland.banking_app.exceptions.DuplicateResourceException;
import com.inholland.banking_app.exceptions.ForbiddenException;
import com.inholland.banking_app.models.CustomerProfile;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.CustomerStatus;
import com.inholland.banking_app.models.enums.Role;
import com.inholland.banking_app.repositories.CustomerProfileRepository;
import com.inholland.banking_app.repositories.EmployeeProfileRepository;
import com.inholland.banking_app.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class UserPolicy {
    private final UserRepository userRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final EmployeeProfileRepository employeeProfileRepository;

    public void assertUniqueEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email already exists");
        }
    }

    public void assertUniqueUsername(String username) {
        if (userRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("Username already exists");
        }
    }

    public void assertUniqueBsn(String bsn) {
        if (customerProfileRepository.existsByBsn(bsn)) {
            throw new DuplicateResourceException("Bsn already exists");
        }
    }

    public void assertUniqueEmployeeNumber(String employeeNumber) {
        if (employeeProfileRepository.existsByEmployeeNumber(employeeNumber)) {
            throw new DuplicateResourceException("Employee number already exists");
        }
    }

    public void assertPasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password does not meet strength requirements");
        }
        boolean hasUppercase = false, hasLowercase = false, hasDigit = false, hasSpecial = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUppercase = true;
            else if (Character.isLowerCase(c)) hasLowercase = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else hasSpecial = true;
        }
        if (!hasUppercase || !hasLowercase || !hasDigit || !hasSpecial) {
            throw new IllegalArgumentException("Password does not meet strength requirements");
        }
    }

    public void assertActiveUser(User user) {
        if (!user.isActive()) {
            throw new DisabledException("User account is inactive");
        }
    }

    public void assertLoginAllowed(User user) {
        if (user.getRole() != Role.CUSTOMER) {
            return;
        }
        Optional<CustomerProfile> profile = customerProfileRepository.findById(user.getId());
        if (profile.isEmpty()) {
            return;
        }
        CustomerStatus status = profile.get().getStatus();
        if (status == CustomerStatus.REJECTED || status == CustomerStatus.CLOSED) {
            throw new ForbiddenException("This account is no longer allowed to access the application");
        }
    }
}
