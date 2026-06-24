package com.inholland.banking_app.services;

import com.inholland.banking_app.dtos.ApproveCustomerRequest;
import com.inholland.banking_app.dtos.UserFilterRequest;
import com.inholland.banking_app.dtos.UserResponse;
import com.inholland.banking_app.mappers.UserResponseMapper;
import com.inholland.banking_app.models.*;
import com.inholland.banking_app.models.enums.CustomerStatus;
import com.inholland.banking_app.models.enums.Role;
import com.inholland.banking_app.repositories.UserRepository;
import com.inholland.banking_app.specifications.UserSpecification;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.function.Consumer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final AccountService accountService;
    private final UserResponseMapper userResponseMapper;

    // Get all users and map with the mapper class
    // Return a paged user
    public Page<UserResponse> getAllUsers(Pageable pageable, UserFilterRequest userFilterRequest) {
        return userRepository.findAll(UserSpecification.fromFilter(userFilterRequest), pageable)
                .map(userResponseMapper::toUserResponse);
    }

    public UserResponse getUserById(Long userId) {
        return userResponseMapper.toUserResponse(findUserOrThrow(userId));
    }

    // Set approval to transactional to reverse any changes if there is any error
    @Transactional
    public void approveCustomer(ApproveCustomerRequest approveCustomerRequest, Long userId) {
        User user = findUserOrThrow(userId);
        setApprove(user, approveCustomerRequest);
    }

    @Transactional
    public UserResponse closeUser(Long userId) {
        return setUserState(userId, false, CustomerStatus.CLOSED, accountService::closeAllAccounts);
    }

    @Transactional
    public UserResponse reopenUser(Long userId) {
        return setUserState(userId, true, CustomerStatus.APPROVED, accountService::reopenAllAccounts);
    }

    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));
    }

    /// Private Helper /////

    // set customer profile to approved and set user active true to allow login
    private void setApprove(User user, ApproveCustomerRequest approveCustomer) {
        getCustomerProfileOrThrow(user).setStatus(CustomerStatus.APPROVED);
        user.setActive(true);

        accountService.createDefaultAccounts(user, approveCustomer);

    }

    // Set user state with the input status
    private UserResponse setUserState(Long userId, boolean active, CustomerStatus status,
            Consumer<User> accountAction) {

        // find the user or throw exception
        User user = findUserOrThrow(userId);
        // Check if user role before creating account
        if (user.getRole() == Role.EMPLOYEE) {
            user.setActive(active);
            userRepository.save(user);
        } else {
            // if its not an employee get the status with a getCustomerProfileOrThrow func
            getCustomerProfileOrThrow(user).setStatus(status);
            accountAction.accept(user);
        }
        return userResponseMapper.toUserResponse(user);
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User with id " + userId + " not found"));
    }

    private CustomerProfile getCustomerProfileOrThrow(User user) {
        CustomerProfile customerProfile = user.getCustomerProfile();
        if (customerProfile == null) {
            throw new EntityNotFoundException("Customer profile not found for user: " + user.getUsername());
        }
        return customerProfile;
    }
}