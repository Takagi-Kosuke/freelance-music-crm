package com.freelancemusiccrm.repository;

import com.freelancemusiccrm.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByTaskId(Long taskId);
}
