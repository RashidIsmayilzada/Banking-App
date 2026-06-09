package com.inholland.banking_app.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserFilterRequest {

    private String role;
    private String status;
    private Boolean hasAccount;
    private Boolean active;
    private String search;

}
