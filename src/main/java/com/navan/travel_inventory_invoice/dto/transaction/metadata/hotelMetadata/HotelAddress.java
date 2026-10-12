package com.navan.travel_inventory_invoice.dto.transaction.metadata.hotelMetadata;

import jakarta.validation.constraints.NotBlank;

public record HotelAddress(
        @NotBlank String street,
        @NotBlank String city,
        @NotBlank String country
) {
}
