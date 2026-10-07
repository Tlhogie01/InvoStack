package com.tman.invostack.service;

import com.tman.invostack.entities.InvoiceItem;
import com.tman.invostack.repository.InvoiceItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class InvoiceItemService {

    private final InvoiceItemRepository invoiceItemRepository;

    public InvoiceItemService(InvoiceItemRepository invoiceItemRepository) {
        this.invoiceItemRepository = invoiceItemRepository;
    }

    public InvoiceItem findById(Long id) {
        return invoiceItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("InvoiceItem not found with id: " + id));
    }

    public List<InvoiceItem> findByInvoiceId(Long invoiceId) {
        return invoiceItemRepository.findByInvoiceId(invoiceId);
    }

    public void deleteItem(Long id) {
        InvoiceItem item = findById(id);
        // WARNING: Deleting directly DOES NOT recalculate invoice total automatically.
        // Prefer using InvoiceService.removeItemFromInvoice() instead!
        invoiceItemRepository.delete(item);
    }

    // Note: For adding/updating, use InvoiceService.addItemToInvoice()
    // to ensure totals are recalculated.
}