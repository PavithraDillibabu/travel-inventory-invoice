
package com.navan.travel_inventory_invoice.dto.transaction;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record TransactionPatchRequestDto(

        @DecimalMin("0.00")
        BigDecimal total,

        @JsonProperty("tax_lines")
        List<@Valid TaxLineDto> taxLines,

        @JsonProperty("fee_lines")
        List<@Valid FeeLineDto> feeLines,

        @JsonProperty("occurred_at")
        OffsetDateTime occurredAt

) {}
