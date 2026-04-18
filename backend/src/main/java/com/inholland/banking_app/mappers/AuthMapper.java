package com.inholland.banking_app.mappers;

import com.inholland.banking_app.dtos.auth.AuthContextResponse;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.CustomerStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AuthMapper {

    public AuthContextResponse toAuthContextResponse(User user) {
        AuthContextResponse response = new AuthContextResponse();
        response.setUserId(user.getId());
        response.setEmail(user.getEmail());
        response.setUsername(user.getUsername());
        response.setRole(user.getRole());
        response.setActive(user.isActive());
        response.setLastLoginAt(user.getLastLoginAt());

        if (user.getCustomerProfile() != null) {
            CustomerStatus status = user.getCustomerProfile().getStatus();
            response.setCustomerStatus(status);
            response.setAuthorizedFeatures(status == CustomerStatus.APPROVED
                    ? List.of("CUSTOMER_BANKING")
                    : List.of("BASIC_WELCOME"));
        } else if (user.getEmployeeProfile() != null) {
            response.setEmployeeEnabled(user.getEmployeeProfile().isEnabled());
            response.setAuthorizedFeatures(user.getEmployeeProfile().isEnabled()
                    ? List.of("EMPLOYEE_BANKING")
                    : List.of());
        } else {
            response.setAuthorizedFeatures(List.of());
        }

        return response;
    }
}
