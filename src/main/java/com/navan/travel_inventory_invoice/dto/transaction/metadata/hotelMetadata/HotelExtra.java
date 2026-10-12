package com.navan.travel_inventory_invoice.dto.transaction.metadata.hotelMetadata;

import java.math.BigDecimal;

public record HotelExtra(
        String name,
        BigDecimal amount
) {
}
