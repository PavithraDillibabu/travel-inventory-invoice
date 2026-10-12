
package com.navan.travel_inventory_invoice.entity.transaction;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "transactions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_transactions_external_id",
                columnNames = "external_id"
        ),
        indexes = @Index(
                name = "idx_transactions_created",
                columnList = "created_at,id"
        )
)
public class Transaction {

    @Id
    @Column(length = 36)
    private String id;

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

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @OneToMany(
            mappedBy = "transaction",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("id ASC")
    private List<TransactionTax> taxLines =
            new ArrayList<>();

    @OneToMany(
            mappedBy = "transaction",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("id ASC")
    private List<TransactionFee> feeLines =
            new ArrayList<>();

    public void addTax(TransactionTax tax) {
        tax.setTransaction(this);
        taxLines.add(tax);
    }

    public void addFee(TransactionFee fee) {
        fee.setTransaction(this);
        feeLines.add(fee);
    }

}
