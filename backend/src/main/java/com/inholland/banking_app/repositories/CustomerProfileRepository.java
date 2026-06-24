package com.inholland.banking_app.repositories;

import com.inholland.banking_app.models.CustomerProfile;

import org.springframework.data.jpa.repository.JpaRepository;

;


public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, Long> {

    boolean existsByBsn(String bsn);

}
