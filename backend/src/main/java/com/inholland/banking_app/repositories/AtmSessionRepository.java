package com.inholland.banking_app.repositories;

import com.inholland.banking_app.models.AtmSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AtmSessionRepository extends JpaRepository<AtmSession, Long> {

    List<AtmSession> findByCustomerId(Long customerId);
}
