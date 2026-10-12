package com.navan.travel_inventory_invoice.dto.transaction.metadata.hotelMetadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.navan.travel_inventory_invoice.dto.transaction.metadata.TransactionMetadataDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record HotelMetadataDto(
        @NotBlank String name,
        @NotNull @Valid HotelAddress address,
        @JsonProperty("check_in") @NotNull LocalDate checkIn,
        @JsonProperty("check_out") @NotNull LocalDate checkOut,
        @NotNull @Positive Integer nights,
        @JsonProperty("room_rate_per_night") @NotNull @PositiveOrZero BigDecimal roomRatePerNight,
        List<@NotNull @Valid HotelExtra> extras
) implements TransactionMetadataDto {
}
