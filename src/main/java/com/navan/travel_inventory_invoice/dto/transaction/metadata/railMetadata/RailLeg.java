package com.navan.travel_inventory_invoice.dto.transaction.metadata.railMetadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

public record RailLeg(
        String origin,
        String destination,
        OffsetDateTime departure,
        OffsetDateTime arrival,
        @JsonProperty("train_number") String trainNumber,
        @JsonProperty("class") String travelClass
) {
}
