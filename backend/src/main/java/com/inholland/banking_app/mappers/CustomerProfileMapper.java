package com.inholland.banking_app.mappers;


import com.inholland.banking_app.dtos.customer.CustomerResponse;
import com.inholland.banking_app.models.CustomerProfile;
import org.springframework.stereotype.Component;

@Component
public class CustomerProfileMapper {

    public CustomerResponse toResponse(CustomerProfile profile) {
        CustomerResponse response = new CustomerResponse();
        response.setUserId(profile.getUser().getId());
        response.setFirstName(profile.getFirstName());
        response.setLastName(profile.getLastName());
        response.setEmail(profile.getUser().getEmail());
        response.setUsername(profile.getUser().getUsername());
        response.setPhoneNumber(profile.getPhoneNumber());
        response.setStatus(profile.getStatus());
        response.setRegisteredAt(profile.getRegisteredAt());
        return response;
    }
}
