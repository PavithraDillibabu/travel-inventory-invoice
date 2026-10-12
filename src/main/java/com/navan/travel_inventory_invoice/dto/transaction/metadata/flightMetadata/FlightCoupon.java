package com.navan.travel_inventory_invoice.dto.transaction.metadata.flightMetadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FlightCoupon(
        @NotBlank String carrier,
        @JsonProperty("flight_number") @NotBlank String flightNumber,
        @NotBlank String from,
        @NotBlank String to,
        @NotNull OffsetDateTime departure,
        @NotNull OffsetDateTime arrival
) {
}
