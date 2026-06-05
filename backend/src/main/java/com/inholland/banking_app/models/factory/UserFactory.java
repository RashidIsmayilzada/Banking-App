package com.inholland.banking_app.models.factory;

import com.inholland.banking_app.dtos.UserRequest;
import com.inholland.banking_app.models.CustomerProfile;
import com.inholland.banking_app.models.EmployeeProfile;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.CustomerStatus;
import com.inholland.banking_app.models.enums.Role;

import java.time.LocalDateTime;

public final class UserFactory {

    public static User createUser(UserRequest request){
        if(request.getRole() == Role.CUSTOMER){
            return createPendingCustomer(request);
        }
        return createEmployee(request);
    }

    public static User createPendingCustomer(UserRequest request) {
        return createPendingCustomer(request, LocalDateTime.now());
    }

    private static CustomerProfile createCustomer(UserRequest request, LocalDateTime now) {
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

        CustomerProfile profile = createCustomer(request, now);
        user.setCustomerProfile(profile);
        return user;
    }

    // Employee factory

    public static User createEmployee(UserRequest request) {
        return createEmployee(request, LocalDateTime.now());
    }

    private static EmployeeProfile createEmployeeProfile(User user, UserRequest request) {
        EmployeeProfile profile = new EmployeeProfile();
        profile.setUser(user);
        profile.setFirstName(request.getFirstName());
        profile.setLastName(request.getLastName());
        profile.setEmployeeNumber("EMP-" + user.getId());
        profile.setEnabled(true);
        profile.setCreatedAt(LocalDateTime.now());
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

        EmployeeProfile profile =  createEmployeeProfile(user, request);
        user.setEmployeeProfile(profile);
        return user;
    }
}
