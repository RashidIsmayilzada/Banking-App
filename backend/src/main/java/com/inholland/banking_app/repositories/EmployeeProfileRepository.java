package com.inholland.banking_app.repositories;

import com.inholland.banking_app.models.EmployeeProfile;
import com.inholland.banking_app.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployeeProfileRepository extends JpaRepository<EmployeeProfile, Long> {

    boolean existsByEmployeeNumber(String employeeNumber);

    Optional<EmployeeProfile> findByUserId(Long userId);
    boolean existsByUserId(Long userId);


}
