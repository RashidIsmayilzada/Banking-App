package com.inholland.banking_app.repositories;

import com.inholland.banking_app.models.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, String> {

    Page<Account> findByCustomerId(Long customerId, Pageable pageable);
}
