package com.inholland.banking_app.repositories;

import com.inholland.banking_app.models.CustomerProfile;

import com.inholland.banking_app.models.enums.CustomerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

;


public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, Long> {

    boolean existsByBsn(String bsn);

}
