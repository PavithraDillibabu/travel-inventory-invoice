package com.navan.travel_inventory_invoice.entity.invoice;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
@Entity
@Getter
@Setter
@Table(name = "invoices", indexes = @Index(
        name = "idx_invoices_created",
        columnList = "created_at,id"
))
public class Invoice {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "total_minor", nullable = false)
    private Long totalMinor;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @OneToMany(
            mappedBy = "invoice",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("id ASC")
    private List<InvoiceLine> transactionLines = new ArrayList<>();

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<InvoiceTax> taxLines = new ArrayList<>();

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<InvoiceFee> feeLines = new ArrayList<>();

    public void addTransactionLine(InvoiceLine line) {
        line.setInvoice(this);
        transactionLines.add(line);
    }

    public void addTax(InvoiceTax tax) {
        tax.setInvoice(this);
        taxLines.add(tax);
    }

    public void addFee(InvoiceFee fee) {
        fee.setInvoice(this);
        feeLines.add(fee);
    }
}
