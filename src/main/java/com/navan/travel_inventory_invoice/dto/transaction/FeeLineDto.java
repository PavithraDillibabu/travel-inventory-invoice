package com.navan.travel_inventory_invoice.dto.transaction;

import java.math.BigDecimal;

public record FeeLineDto(
        String name,
        BigDecimal amount
) {
}
