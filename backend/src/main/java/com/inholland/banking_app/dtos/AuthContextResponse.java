package com.inholland.banking_app.dtos;

import com.inholland.banking_app.models.enums.CustomerStatus;
import com.inholland.banking_app.models.enums.Role;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class AuthContextResponse {
    private Long userId;
    private String email;
    private String username;
    private Role role;
    private boolean active;
    private LocalDateTime lastLoginAt;
    private CustomerStatus customerStatus;
    private Boolean employeeEnabled;
    private List<String> authorizedFeatures;
}
