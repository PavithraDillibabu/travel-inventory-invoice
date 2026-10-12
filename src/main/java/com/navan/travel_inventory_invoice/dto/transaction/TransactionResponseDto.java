
package com.navan.travel_inventory_invoice.dto.transaction;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.navan.travel_inventory_invoice.dto.transaction.metadata.TransactionMetadataDto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record TransactionResponseDto(
        String id,
        TransactionType type,

        @JsonProperty("external_id")
        String externalId,

        String currency,

        @JsonProperty("occurred_at")
        OffsetDateTime occurredAt,

        BigDecimal total,

        @JsonProperty("tax_lines")
        List<TaxLineDto> taxLines,

        @JsonProperty("fee_lines")
        List<FeeLineDto> feeLines,

        TransactionMetadataDto metadata
) {}
