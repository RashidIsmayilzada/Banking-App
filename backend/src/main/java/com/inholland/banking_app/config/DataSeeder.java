package com.inholland.banking_app.config;

import com.inholland.banking_app.models.CustomerProfile;
import com.inholland.banking_app.models.EmployeeProfile;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.CustomerStatus;
import com.inholland.banking_app.models.enums.Role;
import com.inholland.banking_app.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            seedEmployeeAccount();
            seedCustomerAccount();
            log.info("Database seeded successfully with default accounts");
        } else {
            log.info("Database already contains data, skipping seeding");
        }
    }

    private void seedEmployeeAccount() {
        User employee = new User();
        employee.setEmail("employee@bank.com");
        employee.setUsername("employee");
        employee.setPasswordHash(passwordEncoder.encode("Password123!"));
        employee.setRole(Role.EMPLOYEE);
        employee.setActive(true);
        employee.setCreatedAt(LocalDateTime.now());
        employee.setUpdatedAt(LocalDateTime.now());

        EmployeeProfile employeeProfile = new EmployeeProfile();
        employeeProfile.setUser(employee);
        employeeProfile.setFirstName("John");
        employeeProfile.setLastName("Doe");
        employeeProfile.setEmployeeNumber("EMP001");
        employeeProfile.setEnabled(true);
        employeeProfile.setCreatedAt(LocalDateTime.now());

        employee.setEmployeeProfile(employeeProfile);
        userRepository.save(employee);
        log.info("Seeded employee account - email: employee@bank.com, password: Password123!");
    }

    private void seedCustomerAccount() {
        User customer = new User();
        customer.setEmail("customer@test.com");
        customer.setUsername("customer");
        customer.setPasswordHash(passwordEncoder.encode("Password123!"));
        customer.setRole(Role.CUSTOMER);
        customer.setActive(true);
        customer.setCreatedAt(LocalDateTime.now());
        customer.setUpdatedAt(LocalDateTime.now());

        CustomerProfile customerProfile = new CustomerProfile();
        customerProfile.setUser(customer);
        customerProfile.setFirstName("Jane");
        customerProfile.setLastName("Smith");
        customerProfile.setBsn("123456789");
        customerProfile.setPhoneNumber("+31612345678");
        customerProfile.setStatus(CustomerStatus.APPROVED);
        customerProfile.setRegisteredAt(LocalDateTime.now());

        customer.setCustomerProfile(customerProfile);
        userRepository.save(customer);
        log.info("Seeded customer account - email: customer@test.com, password: Password123!");
    }
}
