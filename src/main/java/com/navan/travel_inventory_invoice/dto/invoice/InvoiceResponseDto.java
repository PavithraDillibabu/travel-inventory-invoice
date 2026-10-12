package com.navan.travel_inventory_invoice.dto.invoice;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.navan.travel_inventory_invoice.dto.transaction.FeeLineDto;
import com.navan.travel_inventory_invoice.dto.transaction.TaxLineDto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/** Invoice header, frozen transaction lines, and separate tax and fee rollups. */
public record InvoiceResponseDto(
        String id,
        String currency,
        BigDecimal total,
        @JsonProperty("created_at") OffsetDateTime createdAt,
        @JsonProperty("transaction_lines") List<InvoiceLineResponseDto> transactionLines,
        @JsonProperty("tax_lines") List<TaxLineDto> taxLines,
        @JsonProperty("fee_lines") List<FeeLineDto> feeLines
) {
}
