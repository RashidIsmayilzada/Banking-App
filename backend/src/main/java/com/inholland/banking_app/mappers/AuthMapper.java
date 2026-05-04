package com.inholland.banking_app.mappers;

import com.inholland.banking_app.dtos.AuthContextResponse;
import com.inholland.banking_app.models.CustomerProfile;
import com.inholland.banking_app.models.EmployeeProfile;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.CustomerStatus;
import com.inholland.banking_app.models.enums.Role;
import com.inholland.banking_app.repositories.CustomerProfileRepository;
import com.inholland.banking_app.repositories.EmployeeProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AuthMapper {

    private final CustomerProfileRepository customerProfileRepository;
    private final EmployeeProfileRepository employeeProfileRepository;

    public AuthContextResponse toAuthContextResponse(User user) {
        AuthContextResponse response = new AuthContextResponse();
        response.setUserId(user.getId());
        response.setEmail(user.getEmail());
        response.setUsername(user.getUsername());
        response.setRole(user.getRole());
        response.setActive(user.isActive());
        response.setLastLoginAt(user.getLastLoginAt());

        if (user.getRole() == Role.CUSTOMER) {
            CustomerStatus status = customerProfileRepository.findById(user.getId())
                    .map(CustomerProfile::getStatus)
                    .orElse(null);
            response.setCustomerStatus(status);
            response.setAuthorizedFeatures(status == CustomerStatus.APPROVED
                    ? List.of("CUSTOMER_BANKING")
                    : List.of("BASIC_WELCOME"));
        } else if (user.getRole() == Role.EMPLOYEE) {
            boolean enabled = employeeProfileRepository.findById(user.getId())
                    .map(EmployeeProfile::isEnabled)
                    .orElse(false);
            response.setEmployeeEnabled(enabled);
            response.setAuthorizedFeatures(enabled
                    ? List.of("EMPLOYEE_BANKING")
                    : List.of());
        } else {
            response.setAuthorizedFeatures(List.of());
        }

        return response;
    }
}
