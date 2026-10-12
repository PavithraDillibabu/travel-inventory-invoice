package com.navan.travel_inventory_invoice.dto.invoice;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Request to create an invoice for one transaction or a trip of transactions. */
public record InvoiceRequestDto(
        @JsonProperty("transaction_ids") List<String> transactionIds
) {
}
