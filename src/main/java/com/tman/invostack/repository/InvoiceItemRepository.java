package com.tman.invostack.repository;

import com.tman.invostack.entities.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long> {

    // Find all line items for a specific invoice
    List<InvoiceItem> findByInvoiceId(Long invoiceId);

    // Find all items with a description containing a keyword (useful for searching products)
    List<InvoiceItem> findByDescriptionContaining(String keyword);
}