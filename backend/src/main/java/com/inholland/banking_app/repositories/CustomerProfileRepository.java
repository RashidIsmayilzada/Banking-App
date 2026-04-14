package com.inholland.banking_app.repositories;

import com.inholland.banking_app.models.CustomerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, Long> {

    Optional<CustomerProfile> findByBsn(String bsn);

    boolean existsByBsn(String bsn);
}
