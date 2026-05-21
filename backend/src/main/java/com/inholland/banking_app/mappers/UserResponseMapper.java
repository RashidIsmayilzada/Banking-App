package com.inholland.banking_app.mappers;

import com.inholland.banking_app.dtos.UserResponse;
import com.inholland.banking_app.models.CustomerProfile;
import com.inholland.banking_app.models.EmployeeProfile;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.CustomerStatus;
import org.springframework.stereotype.Component;

@Component
public class UserResponseMapper {

    public UserResponse toCustomerResponse(User user, CustomerProfile profile) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .email(user.getEmail())
                .username(user.getUsername())
                .phoneNumber(profile.getPhoneNumber())
                .bsn(profile.getBsn())
                .role(user.getRole())
                .status(CustomerStatus.PENDING_APPROVAL)
                .hasAccounts(false)
                .accountCount(0)
                .registeredAt(profile.getRegisteredAt())
                .build();
    }

    public UserResponse toEmployeeResponse(User user, EmployeeProfile profile) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole())
                .registeredAt(profile.getCreatedAt())
                .build();
    }
}
