package com.inholland.banking_app.mappers;

import com.inholland.banking_app.models.enums.Role;
import org.springframework.stereotype.Component;

import com.inholland.banking_app.dtos.UserRequest;
import com.inholland.banking_app.models.CustomerProfile;
import com.inholland.banking_app.models.EmployeeProfile;
import com.inholland.banking_app.models.User;

@Component
public class UserRequestMapper {

    public User toUserRequest(UserRequest request) {
        if(request == null){
            return null;
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(request.getPassword());
        user.setEmail(request.getEmail());
        if(request.getRole() == null && request.getEmployeeNumber() == null){
            user.setRole(Role.CUSTOMER);
            user.setCustomerProfile(toCustomerProfile(request));
        }
        else {
            user.setRole(request.getRole());
            user.setEmployeeProfile(toEmployeeProfile(request));
        }
        return user;
    }

    private CustomerProfile toCustomerProfile(UserRequest request){

        CustomerProfile customerProfile = new CustomerProfile();
        customerProfile.setFirstName(request.getFirstName());
        customerProfile.setLastName(request.getLastName());
        customerProfile.setBsn(request.getBsn());
        customerProfile.setPhoneNumber(request.getPhoneNumber());

        return customerProfile;
    }

    private EmployeeProfile toEmployeeProfile(UserRequest request){

        EmployeeProfile employeeProfile = new EmployeeProfile();
        employeeProfile.setFirstName(request.getFirstName());
        employeeProfile.setLastName(request.getLastName());
        employeeProfile.setEmployeeNumber(request.getBsn());

        return employeeProfile;
    }
    
}
