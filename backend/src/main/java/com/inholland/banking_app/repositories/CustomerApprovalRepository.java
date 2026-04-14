package com.inholland.banking_app.repositories;

import com.inholland.banking_app.models.CustomerApproval;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerApprovalRepository extends JpaRepository<CustomerApproval, Long> {

    List<CustomerApproval> findByCustomerId(Long customerId);

    List<CustomerApproval> findByApprovedByEmployeeId(Long employeeId);
}
