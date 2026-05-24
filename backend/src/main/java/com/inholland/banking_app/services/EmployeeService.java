package com.inholland.banking_app.services;

import com.inholland.banking_app.dtos.*;
import com.inholland.banking_app.mappers.AccountMapper;
import com.inholland.banking_app.mappers.UserResponseMapper;
import com.inholland.banking_app.repositories.AccountRepository;
import com.inholland.banking_app.repositories.CustomerProfileRepository;
import com.inholland.banking_app.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor

public class EmployeeService {
    private final CustomerProfileRepository customerProfileRepository;
    private final UserRepository userRepository;
    private final UserResponseMapper userMapper;
    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;

    public Page<CustomerResponse> getAllCustomers(Pageable pageable) {
        return customerProfileRepository.findAll(pageable)
                .map(userMapper::toCustomerResponse);
    }

    public Page<UserResponse> customersWithoutAccounts(Pageable pageable) {
        return userRepository.findAllByAccountsIsEmpty(pageable)
                .map(userMapper::toUserResponse);
    }

    public Page<AccountResponse> getAccountByUserId(Pageable pageable, Long customerId) {
        if (!userRepository.existsById(customerId)) {
            throw new EntityNotFoundException("User with id " + customerId + " not found");
        }

        return accountRepository.findByCustomerId(pageable, customerId)
                .map(accountMapper::toAccountResponse);
    }

}
