package com.tman.invostack.service;

import com.tman.invostack.entities.Client;
import com.tman.invostack.entities.Invoice;
import com.tman.invostack.entities.InvoiceItem;
import com.tman.invostack.repository.InvoiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class InvoiceService {

    // Default tax rate (15%). This could be dynamic per client or country.
    private static final BigDecimal DEFAULT_TAX_RATE = new BigDecimal("0.15");

    // Status transition rules: Current Status -> Allowed Next Statuses
    private static final Map<String, List<String>> ALLOWED_TRANSITIONS = Map.of(
            "DRAFT", List.of("SENT", "CANCELLED"),
            "SENT", List.of("PAID", "OVERDUE"),
            "OVERDUE", List.of("PAID"),
            "PAID", List.of(),  // No further transitions allowed
            "CANCELLED", List.of() // No further transitions allowed
    );

    private final InvoiceRepository invoiceRepository;
    private final ClientService clientService;

    public InvoiceService(InvoiceRepository invoiceRepository, ClientService clientService) {
        this.invoiceRepository = invoiceRepository;
        this.clientService = clientService;
    }

    // =========================================
    // 1. CREATING AN INVOICE (with number generation)
    // =========================================

    public Invoice createInvoice(Long clientId, Invoice invoice, BigDecimal taxRate) {
        // 1. Fetch the client
        Client client = clientService.findById(clientId);

        // 2. Set client
        invoice.setClient(client);

        // 3. Set invoice date and due date (optional defaults)
        if (invoice.getDate() == null) {
            invoice.setDate(new Date());
        }
        if (invoice.getDueDate() == null) {
            // Default due date: 30 days from today
            LocalDate due = LocalDate.now().plusDays(30);
            invoice.setDueDate(Date.from(due.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        }

        // 4. Set initial status
        invoice.setStatus("DRAFT");

        // 5. Set tax rate (use default if null)
        invoice.setTaxRate(taxRate != null ? taxRate : DEFAULT_TAX_RATE);

        // 6. Generate sequential invoice number
        invoice.setNumber(generateInvoiceNumber());

        // 7. Recalculate totals (items should be added AFTER creation, or passed in)
        //    If you pass items in the invoice object, we calculate here.
        recalculateInvoiceTotals(invoice);

        // 8. Save and return
        return invoiceRepository.save(invoice);
    }

    // =========================================
    // 2. INVOICE NUMBER GENERATOR (Sequential)
    // =========================================

    private String generateInvoiceNumber() {
        // Fetch the last created invoice by ID (ID is auto-incrementing)
        Invoice lastInvoice = invoiceRepository.findFirstByOrderByIdDesc().orElse(null);

        int nextId;
        if (lastInvoice == null) {
            nextId = 1;
        } else {
            // Use the ID of the last invoice + 1. Even if rows are deleted, this stays unique.
            nextId = (int) (lastInvoice.getId() + 1);
        }

        // Format as "INV-000001", "INV-000002", etc.
        return String.format("INV-%06d", nextId);
    }

    // =========================================
    // 3. TOTAL CALCULATION (Subtotal, Tax, Total)
    // =========================================

    private void recalculateInvoiceTotals(Invoice invoice) {
        List<InvoiceItem> items = invoice.getItems();

        // Calculate Subtotal (sum of all line item subtotals)
        BigDecimal subtotal = items.stream()
                .map(InvoiceItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        invoice.setSubtotal(subtotal);

        // Calculate Tax Amount
        BigDecimal taxRate = invoice.getTaxRate() != null ? invoice.getTaxRate() : DEFAULT_TAX_RATE;
        BigDecimal taxAmount = subtotal.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
        invoice.setTaxAmount(taxAmount);

        // Calculate Grand Total
        BigDecimal total = subtotal.add(taxAmount).setScale(2, RoundingMode.HALF_UP);
        invoice.setTotal(total);
    }

    // =========================================
    // 4. MANAGING ITEMS (Add/Remove - auto recalcs)
    // =========================================

    public Invoice addItemToInvoice(Long invoiceId, InvoiceItem item) {
        Invoice invoice = findById(invoiceId);

        // Prevent adding items to non-DRAFT invoices (business rule)
        if (!"DRAFT".equals(invoice.getStatus()) && !"SENT".equals(invoice.getStatus())) {
            throw new RuntimeException("Cannot add items to an invoice that is already PAID or CANCELLED.");
        }

        // Add item (bidirectional sync happens via the helper method in Invoice entity)
        invoice.addItem(item);

        // Recalculate totals (this happens inside invoice.addItem, but we call it explicitly for safety)
        recalculateInvoiceTotals(invoice);

        return invoiceRepository.save(invoice);
    }

    public Invoice removeItemFromInvoice(Long invoiceId, Long itemId) {
        Invoice invoice = findById(invoiceId);

        if (!"DRAFT".equals(invoice.getStatus())) {
            throw new RuntimeException("Cannot remove items from an invoice that is not in DRAFT status.");
        }

        // Find and remove the item from the invoice's list
        InvoiceItem itemToRemove = invoice.getItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Item not found in this invoice"));

        invoice.removeItem(itemToRemove); // Helper method handles the removal and recalc

        return invoiceRepository.save(invoice);
    }

    // =========================================
    // 5. STATUS TRANSITION VALIDATION
    // =========================================

    public Invoice updateInvoiceStatus(Long invoiceId, String newStatus) {
        Invoice invoice = findById(invoiceId);
        String currentStatus = invoice.getStatus();

        // Validate the transition
        if (!isValidTransition(currentStatus, newStatus)) {
            throw new RuntimeException(
                    String.format("Invalid status transition from '%s' to '%s'. Allowed transitions: %s",
                            currentStatus, newStatus, ALLOWED_TRANSITIONS.getOrDefault(currentStatus, List.of()))
            );
        }

        invoice.setStatus(newStatus);
        return invoiceRepository.save(invoice);
    }

    private boolean isValidTransition(String currentStatus, String newStatus) {
        List<String> allowedNext = ALLOWED_TRANSITIONS.getOrDefault(currentStatus, List.of());
        return allowedNext.contains(newStatus);
    }

    // =========================================
    // 6. STANDARD CRUD / FINDERS
    // =========================================

    public Invoice findById(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + id));
    }

    public List<Invoice> findAllInvoices() {
        return invoiceRepository.findAll();
    }

    public List<Invoice> findInvoicesByClient(Long clientId) {
        return invoiceRepository.findByClientId(clientId);
    }

    public List<Invoice> findInvoicesByStatus(String status) {
        return invoiceRepository.findByStatus(status);
    }

    public Invoice findByNumber(String number) {
        return invoiceRepository.findByNumber(number);
    }

    public void deleteInvoice(Long id) {
        Invoice invoice = findById(id);
        // Only allow deletion of DRAFT invoices
        if (!"DRAFT".equals(invoice.getStatus())) {
            throw new RuntimeException("Cannot delete an invoice that is not in DRAFT status.");
        }
        invoiceRepository.delete(invoice);
    }

    // =========================================
    // 7. MANUAL REFRESH (if totals get out of sync)
    // =========================================

    public Invoice refreshInvoiceTotals(Long invoiceId) {
        Invoice invoice = findById(invoiceId);
        recalculateInvoiceTotals(invoice);
        return invoiceRepository.save(invoice);
    }
}