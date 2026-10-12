package com.navan.travel_inventory_invoice.repository;

import com.navan.travel_inventory_invoice.entity.invoice.InvoiceLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceLineRepository extends JpaRepository<InvoiceLine, Long> {
    boolean existsByTransactionId(String transactionId);
}
