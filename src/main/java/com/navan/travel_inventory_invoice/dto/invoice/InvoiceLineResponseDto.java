package com.navan.travel_inventory_invoice.dto.invoice;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.navan.travel_inventory_invoice.dto.transaction.metadata.TransactionMetadataDto;
import com.navan.travel_inventory_invoice.dto.transaction.FeeLineDto;
import com.navan.travel_inventory_invoice.dto.transaction.TaxLineDto;
import com.navan.travel_inventory_invoice.dto.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Frozen transaction details included on an invoice.
 */
public record InvoiceLineResponseDto(
        @JsonProperty("transaction_id") String transactionId,
        TransactionType type,
        @JsonProperty("external_id") String externalId,
        String currency,
        @JsonProperty("occurred_at") OffsetDateTime occurredAt,
        BigDecimal total,
        TransactionMetadataDto metadata,
        @JsonProperty("tax_lines") List<TaxLineDto> taxLines,
        @JsonProperty("fee_lines") List<FeeLineDto> feeLines
) {
}
