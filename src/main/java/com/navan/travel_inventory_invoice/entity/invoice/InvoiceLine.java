
package com.navan.travel_inventory_invoice.entity.invoice;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Getter
@Setter
@Table(
        name = "invoice_lines",
        indexes = {
                @Index(
                        name = "idx_invoice_lines_invoice",
                        columnList = "invoice_id"
                )
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_invoice_line_transaction",
                        columnNames = "transaction_id"
                )
        }
)
public class InvoiceLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @Column(name = "transaction_id", nullable = false, length = 36)
    private String transactionId;

    @Column(nullable = false)
    private String type;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @Column(name = "total_minor", nullable = false)
    private Long totalMinor;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String metadata;

    @Column(
            name = "tax_lines_snapshot",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String taxLinesSnapshot;

    @Column(
            name = "fee_lines_snapshot",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String feeLinesSnapshot;
}
