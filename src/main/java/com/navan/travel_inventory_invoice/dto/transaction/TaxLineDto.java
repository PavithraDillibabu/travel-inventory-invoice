package com.navan.travel_inventory_invoice.dto.transaction;

import java.math.BigDecimal;

public record TaxLineDto(
        String name,
        BigDecimal rate,
        BigDecimal amount
) {
}
