package com.inholland.banking_app.models.factory;

import com.inholland.banking_app.dtos.UserRequest;
import com.inholland.banking_app.models.CustomerProfile;
import com.inholland.banking_app.models.EmployeeProfile;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.CustomerStatus;
import com.inholland.banking_app.models.enums.Role;

import java.time.LocalDateTime;

public final class UserFactory {

    public static User createUser(UserRequest request) {
        return switch (request.getRole()) {
            case CUSTOMER -> createPendingCustomer(request);
            case EMPLOYEE -> createEmployee(request);
            default -> throw new IllegalArgumentException("Unsupported role: " + request.getRole());
        };
    }

    public static User createPendingCustomer(UserRequest request) {
        return createPendingCustomer(request, LocalDateTime.now());
    }

    private static CustomerProfile createCustomerProfile(UserRequest request, LocalDateTime now) {
        CustomerProfile profile = new CustomerProfile();
        profile.setFirstName(request.getFirstName());
        profile.setLastName(request.getLastName());
        profile.setBsn(request.getBsn());
        profile.setPhoneNumber(request.getPhoneNumber());
        profile.setStatus(CustomerStatus.PENDING_APPROVAL);
        profile.setRegisteredAt(now);
        return profile;
    }

    private static User createPendingCustomer(UserRequest request, LocalDateTime now) {
        User user = new User();
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setPasswordHash(request.getPassword());
        user.setRole(Role.CUSTOMER);
        user.setActive(true);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        CustomerProfile profile = createCustomerProfile(request, now);
        profile.setUser(user);
        user.setCustomerProfile(profile);
        return user;
    }

    public static User createEmployee(UserRequest request) {
        return createEmployee(request, LocalDateTime.now());
    }

    private static EmployeeProfile createEmployeeProfile(UserRequest request, LocalDateTime now) {
        EmployeeProfile profile = new EmployeeProfile();
        profile.setFirstName(request.getFirstName());
        profile.setLastName(request.getLastName());
        profile.setEmployeeNumber("EMP-" + request.getEmployeeNumber());
        profile.setEnabled(true);
        profile.setCreatedAt(now);
        return profile;
    }

    private static User createEmployee(UserRequest request, LocalDateTime now) {
        User user = new User();
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setPasswordHash(request.getPassword());
        user.setRole(Role.EMPLOYEE);
        user.setActive(true);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        EmployeeProfile profile = createEmployeeProfile(request, now);
        profile.setUser(user);
        user.setEmployeeProfile(profile);
        return user;
    }
}
