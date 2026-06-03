package com.inholland.banking_app.services;

import com.inholland.banking_app.dtos.UserCreateRequest;
import com.inholland.banking_app.dtos.UserResponse;
import com.inholland.banking_app.exceptions.DuplicateResourceException;
import com.inholland.banking_app.mappers.UserMapper;
import com.inholland.banking_app.models.CustomerProfile;
import com.inholland.banking_app.models.EmployeeProfile;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.Role;
import com.inholland.banking_app.models.factory.UserFactory;
import com.inholland.banking_app.repositories.CustomerProfileRepository;
import com.inholland.banking_app.repositories.EmployeeProfileRepository;
import com.inholland.banking_app.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final EmployeeProfileRepository employeeProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        // Validates uniqueness constraints, creates the user and role-specific profile, and returns the response
        validateUniqueEmail(request.getEmail());
        validateUniqueUsername(request.getUsername());

        Role role = request.getRole();

        if (role == Role.CUSTOMER) {
            validateUniqueBsn(request.getBsn());
        }

        String passwordHash = passwordEncoder.encode(request.getPassword());
        User user = UserFactory.createUser(request, passwordHash, role);
        userRepository.save(user);

        if (role == Role.EMPLOYEE) {
            EmployeeProfile profile = UserFactory.createEmployeeProfile(user, request);
            employeeProfileRepository.save(profile);
            return userMapper.toEmployeeResponse(user, profile);
        }

        CustomerProfile profile = UserFactory.createCustomerProfile(user, request);
        customerProfileRepository.save(profile);
        return userMapper.toCustomerResponse(user, profile);
    }

    private void validateUniqueEmail(String email) {
        // Throws DuplicateResourceException if the email is already registered
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email already exists");
        }
    }

    private void validateUniqueUsername(String username) {
        // Throws DuplicateResourceException if the username is already taken
        if (userRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("Username already exists");
        }
    }

    private void validateUniqueBsn(String bsn) {
        // Throws DuplicateResourceException if the BSN is already in use
        if (customerProfileRepository.existsByBsn(bsn)) {
            throw new DuplicateResourceException("BSN already exists");
        }
    }
}
