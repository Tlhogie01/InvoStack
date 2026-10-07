package com.tman.invostack.entities;


import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String number;

    @Temporal(TemporalType.DATE)
    private Date date;

    @Temporal(TemporalType.DATE)
    private Date dueDate;

    private String status;

    // --- Financial Fields (NEW) ---
    @Column(precision = 19, scale = 2)
    private BigDecimal subtotal;      // Sum of items before tax

    @Column(precision = 19, scale = 2)
    private BigDecimal taxRate;       // e.g., 0.15 for 15%

    @Column(precision = 19, scale = 2)
    private BigDecimal taxAmount;     // subtotal * taxRate

    @Column(precision = 19, scale = 2)
    private BigDecimal total;         // subtotal + taxAmount

    // --- Relationships ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<InvoiceItem> items = new ArrayList<>();



    // --- Constructors ---

    public Invoice() {
        this.subtotal = BigDecimal.ZERO;
        this.taxAmount = BigDecimal.ZERO;
        this.total = BigDecimal.ZERO;
    }

    public Invoice(String number, Date date, Date dueDate, String status, Client client) {
        this.number = number;
        this.date = date;
        this.dueDate = dueDate;
        this.status = status;
        this.client = client;
        this.subtotal = BigDecimal.ZERO;
        this.taxAmount = BigDecimal.ZERO;
        this.total = BigDecimal.ZERO;
    }

    // --- Helper Methods (to manage items) ---

    public void addItem(InvoiceItem item) {
        items.add(item);
        item.setInvoice(this);
        // Note: Recalculation of totals is now done in the Service layer
        // to keep this entity simple and focused.
    }

    public void removeItem(InvoiceItem item) {
        items.remove(item);
        item.setInvoice(null);
    }

    // --- Getters and Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }

    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }

    public Date getDueDate() { return dueDate; }
    public void setDueDate(Date dueDate) { this.dueDate = dueDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getTaxRate() { return taxRate; }    // <-- GETTER
    public void setTaxRate(BigDecimal taxRate) { this.taxRate = taxRate; }  // <-- SETTER (Now exists!)

    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }

    public List<InvoiceItem> getItems() { return items; }
    public void setItems(List<InvoiceItem> items) {
        this.items = items;
        if (items != null) {
            for (InvoiceItem item : items) {
                item.setInvoice(this);
            }
        }
    }
}