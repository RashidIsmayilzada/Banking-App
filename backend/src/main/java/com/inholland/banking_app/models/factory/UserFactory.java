package com.inholland.banking_app.models.factory;

import com.inholland.banking_app.dtos.RegisterCustomerRequest;
import com.inholland.banking_app.dtos.RegisterEmployeeRequest;
import com.inholland.banking_app.models.CustomerProfile;
import com.inholland.banking_app.models.EmployeeProfile;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.CustomerStatus;
import com.inholland.banking_app.models.enums.Role;

import java.time.LocalDateTime;

public final class UserFactory {

    private UserFactory() {
    }

    public static User createPendingCustomer(RegisterCustomerRequest request, String passwordHash) {
        return createPendingCustomer(request, passwordHash, LocalDateTime.now());
    }

    public static User createEmployee(RegisterEmployeeRequest request, String passwordHash) {
        return createEmployee(request, passwordHash, LocalDateTime.now());
    }

    public static User createPendingCustomer(RegisterCustomerRequest request, String passwordHash, LocalDateTime now) {
        User user = new User();
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordHash);
        user.setRole(Role.CUSTOMER);
        user.setActive(true);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        CustomerProfile profile = new CustomerProfile();
        profile.setUser(user);
        profile.setFirstName(request.getFirstName());
        profile.setLastName(request.getLastName());
        profile.setBsn(request.getBsn());
        profile.setPhoneNumber(request.getPhoneNumber());
        profile.setStatus(CustomerStatus.PENDING_APPROVAL);
        profile.setRegisteredAt(now);

        user.setCustomerProfile(profile);
        return user;
    }

    public static User createEmployee(RegisterEmployeeRequest request, String passwordHash, LocalDateTime now) {
        User user = new User();
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordHash);
        user.setRole(Role.EMPLOYEE);
        user.setActive(true);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        EmployeeProfile profile = new EmployeeProfile();
        profile.setUser(user);
        profile.setFirstName(request.getFirstName());
        profile.setLastName(request.getLastName());
        profile.setEmployeeNumber(request.getEmployeeNumber());
        profile.setEnabled(true);
        profile.setCreatedAt(now);

        user.setEmployeeProfile(profile);
        return user;
    }
}
