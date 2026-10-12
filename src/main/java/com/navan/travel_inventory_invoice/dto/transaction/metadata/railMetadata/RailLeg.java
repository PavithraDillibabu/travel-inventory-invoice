package com.navan.travel_inventory_invoice.dto.transaction.metadata.railMetadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RailLeg(
        @NotBlank String origin,
        @NotBlank String destination,
        @NotNull OffsetDateTime departure,
        @NotNull OffsetDateTime arrival,
        @JsonProperty("train_number") String trainNumber,
        @JsonProperty("class") String travelClass
) {
}
