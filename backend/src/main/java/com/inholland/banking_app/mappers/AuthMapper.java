package com.inholland.banking_app.mappers;

import com.inholland.banking_app.dtos.AuthContextResponse;
import com.inholland.banking_app.models.CustomerProfile;
import com.inholland.banking_app.models.EmployeeProfile;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.CustomerStatus;
import com.inholland.banking_app.repositories.CustomerProfileRepository;
import com.inholland.banking_app.repositories.EmployeeProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

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

        Optional<CustomerProfile> customerProfile = customerProfileRepository.findById(user.getId());
        if (customerProfile.isPresent()) {
            CustomerStatus status = customerProfile.get().getStatus();
            response.setUserStatus(status);
            response.setAuthorizedFeatures(status == CustomerStatus.APPROVED
                    ? List.of("CUSTOMER_BANKING")
                    : List.of("BASIC_WELCOME"));
            return response;
        }

        Optional<EmployeeProfile> employeeProfile = employeeProfileRepository.findById(user.getId());
        if (employeeProfile.isPresent()) {
            response.setAuthorizedFeatures(employeeProfile.get().isEnabled()
                    ? List.of("EMPLOYEE_BANKING")
                    : List.of());
            return response;
        }

        response.setAuthorizedFeatures(List.of());
        return response;
    }
}
