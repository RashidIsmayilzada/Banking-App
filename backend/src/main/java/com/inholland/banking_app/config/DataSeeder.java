package com.inholland.banking_app.config;

import com.inholland.banking_app.models.EmployeeProfile;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.Role;
import com.inholland.banking_app.repositories.EmployeeProfileRepository;
import com.inholland.banking_app.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final EmployeeProfileRepository employeeProfileRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        seedEmployee("employee01@bank.com", "employee01", "EMP001", "Admin", "User");
        seedEmployee("employee02@bank.com", "employee02", "EMP002", "Jane", "Doe");
    }

    private void seedEmployee(String email, String username, String empNumber, String firstName, String lastName) {
        if (userRepository.findByEmail(email).isEmpty()) {
            log.info("[DEBUG_LOG] Seeding employee user: {}", email);
            User user = new User();
            user.setEmail(email);
            user.setUsername(username);
            user.setPasswordHash(passwordEncoder.encode("Password@123"));
            user.setRole(Role.EMPLOYEE);
            user.setActive(true);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            
            user = userRepository.save(user);

            EmployeeProfile profile = new EmployeeProfile();
            profile.setUser(user);
            profile.setFirstName(firstName);
            profile.setLastName(lastName);
            profile.setEmployeeNumber(empNumber);
            profile.setEnabled(true);
            profile.setCreatedAt(LocalDateTime.now());
            
            user.setEmployeeProfile(profile);
            userRepository.save(user);
            log.info("[DEBUG_LOG] Successfully seeded employee: {}", email);
        }
    }
}
