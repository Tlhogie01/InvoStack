package com.tman.invostack.repository;

import com.tman.invostack.entities.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    // Find all invoices belonging to a specific client (using the foreign key client_id)
    List<Invoice> findByClientId(Long clientId);

    // Find all invoices with a specific status (e.g., "DRAFT", "PAID", "OVERDUE")
    List<Invoice> findByStatus(String status);

    // Find all invoices due before a certain date (for overdue reminders)
    List<Invoice> findByDueDateBefore(Date date);

    // Find an invoice by its unique business number (e.g., "INV-2026-001")
    Invoice findByNumber(String number);

    Optional<Invoice> findFirstByOrderByIdDesc();
}