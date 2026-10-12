package com.navan.travel_inventory_invoice.dto.transaction.metadata.flightMetadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

public record FlightCoupon(
        String carrier,
        @JsonProperty("flight_number") String flightNumber,
        String from,
        String to,
        OffsetDateTime departure,
        OffsetDateTime arrival
) {
}
