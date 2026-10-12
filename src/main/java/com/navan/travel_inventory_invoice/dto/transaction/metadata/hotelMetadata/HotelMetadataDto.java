package com.navan.travel_inventory_invoice.dto.transaction.metadata.hotelMetadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.navan.travel_inventory_invoice.dto.transaction.metadata.TransactionMetadataDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record HotelMetadataDto(
        String name,
        HotelAddress address,
        @JsonProperty("check_in") LocalDate checkIn,
        @JsonProperty("check_out") LocalDate checkOut,
        Integer nights,
        @JsonProperty("room_rate_per_night") BigDecimal roomRatePerNight,
        List<HotelExtra> extras
) implements TransactionMetadataDto {
}
