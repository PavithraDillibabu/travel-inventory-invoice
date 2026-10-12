
package com.navan.travel_inventory_invoice.entity.transaction;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Setter
@Getter
@Table(name = "transaction_taxes",
        indexes = @Index(
                name = "idx_transaction_taxes_transaction",
                columnList = "transaction_id"
        )
)
public class TransactionTax {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "transaction_id",
            nullable = false
    )
    private Transaction transaction;

    @Column(nullable = false)
    private String name;

    // Optional decimal rate, e.g. 0.07
    @Column(precision = 12, scale = 6)
    private java.math.BigDecimal rate;

    @Column(name = "amount_minor", nullable = false)
    private Long amountMinor;

}
