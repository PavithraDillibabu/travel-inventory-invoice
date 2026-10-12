package com.navan.travel_inventory_invoice.dto.transaction.metadata.flightMetadata;

import com.navan.travel_inventory_invoice.dto.transaction.metadata.TransactionMetadataDto;

import java.math.BigDecimal;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record FlightMetadataDto(
        @NotBlank String origin,
        @NotBlank String destination,
        @NotNull @PositiveOrZero BigDecimal fare,
        @NotEmpty List<@NotNull @Valid FlightCoupon> coupons
) implements TransactionMetadataDto {
}
