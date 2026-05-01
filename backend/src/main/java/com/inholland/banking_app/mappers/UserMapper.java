package com.inholland.banking_app.mappers;

import com.inholland.banking_app.dtos.CustomerResponse;
import com.inholland.banking_app.dtos.UserResponse;
import com.inholland.banking_app.models.CustomerProfile;
import com.inholland.banking_app.models.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toUserResponse(User user) {
        if (user == null) {
            return null;
        }

        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setUsername(user.getUsername());
        response.setRole(user.getRole());
        response.setActive(user.isActive());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        response.setLastLoginAt(user.getLastLoginAt());

        if (user.getCustomerProfile() != null) {
            response.setCustomerProfile(toCustomerResponse(user.getCustomerProfile()));
        }

        return response;
    }

    public CustomerResponse toCustomerResponse(CustomerProfile profile) {
        if (profile == null) {
            return null;
        }

        CustomerResponse response = new CustomerResponse();
        response.setUserId(profile.getUserId());
        response.setFirstName(profile.getFirstName());
        response.setLastName(profile.getLastName());
        response.setPhoneNumber(profile.getPhoneNumber());
        response.setBsn(profile.getBsn());
        response.setStatus(profile.getStatus());
        response.setRegisteredAt(profile.getRegisteredAt());
        if (profile.getUser() != null) {
            response.setEmail(profile.getUser().getEmail());
            response.setUsername(profile.getUser().getUsername());
        }

        return response;
    }
}
