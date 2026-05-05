package com.inholland.banking_app.models.factory;

import com.inholland.banking_app.dtos.UserCreateRequest;
import com.inholland.banking_app.models.CustomerProfile;
import com.inholland.banking_app.models.EmployeeProfile;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.CustomerStatus;
import com.inholland.banking_app.models.enums.Role;

import java.time.LocalDateTime;

public final class UserFactory {

    private UserFactory() {}

    public static User createUser(UserCreateRequest request, String passwordHash, Role role) {
        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordHash);
        user.setRole(role);
        user.setActive(true);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return user;
    }

    public static CustomerProfile createCustomerProfile(User user, UserCreateRequest request) {
        CustomerProfile profile = new CustomerProfile();
        profile.setUser(user);
        profile.setFirstName(request.getFirstName());
        profile.setLastName(request.getLastName());
        profile.setBsn(request.getBsn());
        profile.setPhoneNumber(request.getPhoneNumber());
        profile.setStatus(CustomerStatus.PENDING_APPROVAL);
        profile.setRegisteredAt(LocalDateTime.now());
        return profile;
    }

    public static EmployeeProfile createEmployeeProfile(User user, UserCreateRequest request) {
        EmployeeProfile profile = new EmployeeProfile();
        profile.setUser(user);
        profile.setFirstName(request.getFirstName());
        profile.setLastName(request.getLastName());
        profile.setEmployeeNumber("EMP-" + user.getId());
        profile.setEnabled(true);
        profile.setCreatedAt(LocalDateTime.now());
        return profile;
    }
}
