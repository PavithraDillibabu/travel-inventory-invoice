package com.navan.travel_inventory_invoice.dto.transaction.metadata.hotelMetadata;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record HotelExtra(
        @NotBlank String name,
        @NotNull @PositiveOrZero BigDecimal amount
) {
}
