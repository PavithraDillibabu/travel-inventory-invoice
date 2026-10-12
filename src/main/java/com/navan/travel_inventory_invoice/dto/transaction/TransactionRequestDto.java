package com.navan.travel_inventory_invoice.dto.transaction;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.navan.travel_inventory_invoice.dto.transaction.metadata.flightMetadata.FlightMetadataDto;
import com.navan.travel_inventory_invoice.dto.transaction.metadata.hotelMetadata.HotelMetadataDto;
import com.navan.travel_inventory_invoice.dto.transaction.metadata.railMetadata.RailMetadataDto;
import com.navan.travel_inventory_invoice.dto.transaction.metadata.TransactionMetadataDto;
import com.navan.travel_inventory_invoice.dto.transaction.metadata.navanFeeMetadata.NavanFeeMetadataDto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record TransactionRequestDto(
        @NotNull
        TransactionType type,
        @JsonProperty("external_id") String externalId,
        String currency,
        @JsonProperty("occurred_at") OffsetDateTime occurredAt,
        BigDecimal total,
        @JsonProperty("tax_lines") List<TaxLineDto> taxLines,
        @JsonProperty("fee_lines") List<FeeLineDto> feeLines,

        @NotNull
        @Valid
        @JsonTypeInfo(
                use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
                property = "type"
        )
        @JsonSubTypes({
                @JsonSubTypes.Type(
                        value = FlightMetadataDto.class, name = "flight"),
                @JsonSubTypes.Type(
                        value = HotelMetadataDto.class, name = "hotel"),
                @JsonSubTypes.Type(
                        value = RailMetadataDto.class, name = "rail"),
                @JsonSubTypes.Type(
                        value = NavanFeeMetadataDto.class, name = "navan_fee")
        })
        TransactionMetadataDto metadata
) {
}
