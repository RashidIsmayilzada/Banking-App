package com.inholland.banking_app.mappers;

import com.inholland.banking_app.dtos.UserResponse;
import com.inholland.banking_app.models.CustomerProfile;
import com.inholland.banking_app.models.EmployeeProfile;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
public class UserResponseMapper {

    private final AccountMapper accountMapper;

    public UserResponse toUserResponse(User user) {
        if (user == null) {
            return null;
        }

        if (user.getRole() == Role.EMPLOYEE) {
            EmployeeProfile employeeProfile = user.getEmployeeProfile();
            if (employeeProfile != null) {
                return toEmployeeResponse(user, employeeProfile);
            }
        } else {
            CustomerProfile customerProfile = user.getCustomerProfile();
            if (customerProfile != null) {
                return toCustomerResponse(user, customerProfile);
            }
        }

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole())
                .registeredAt(user.getCreatedAt())
                .build();
    }

    // binding User + CustomerProfile and account together

    private UserResponse toCustomerResponse(User user, CustomerProfile profile) {
        int accountCount = user.getAccounts() == null ? 0 : user.getAccounts().size();
        return UserResponse.builder()
                .id(user.getId())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .email(user.getEmail())
                .username(user.getUsername())
                .phoneNumber(profile.getPhoneNumber())
                .bsn(profile.getBsn())
                .role(user.getRole())
                .active(user.isActive())
                .status(profile.getStatus())
                .hasAccounts(accountCount > 0)
                .accountCount(accountCount)
                .accounts(user.getAccounts() == null
                    ? List.of()
                    : user.getAccounts().stream().map(accountMapper::toResponse).toList())
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
                .active(user.isActive())
                .registeredAt(profile.getCreatedAt())
                .build();
    }
}
