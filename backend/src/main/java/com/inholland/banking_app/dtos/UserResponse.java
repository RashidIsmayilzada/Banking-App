package com.inholland.banking_app.dtos;

import com.inholland.banking_app.models.enums.CustomerStatus;
import com.inholland.banking_app.models.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String username;
    private String phoneNumber;
    private String bsn;
    private Role role;
    private CustomerStatus status;
    private Boolean hasAccounts;
    private Integer accountCount;
    private List<AccountResponse> accounts;
    private LocalDateTime registeredAt;
}
