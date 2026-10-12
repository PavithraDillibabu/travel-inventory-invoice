
package com.navan.travel_inventory_invoice.entity.transaction;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Setter
@Getter
@Table(name = "transaction_fees",
        indexes = @Index(
                name = "idx_transaction_fees_transaction",
                columnList = "transaction_id"
        )
)
public class TransactionFee {

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

    @Column(name = "amount_minor", nullable = false)
    private Long amountMinor;

}
