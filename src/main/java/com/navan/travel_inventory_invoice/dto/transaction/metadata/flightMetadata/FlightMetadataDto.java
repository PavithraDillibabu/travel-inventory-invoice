package com.navan.travel_inventory_invoice.dto.transaction.metadata.flightMetadata;

import com.navan.travel_inventory_invoice.dto.transaction.metadata.TransactionMetadataDto;

import java.math.BigDecimal;
import java.util.List;

public record FlightMetadataDto(
        String origin,
        String destination,
        BigDecimal fare,
        List<FlightCoupon> coupons
) implements TransactionMetadataDto {
}
