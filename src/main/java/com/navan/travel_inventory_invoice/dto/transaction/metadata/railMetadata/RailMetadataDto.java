package com.navan.travel_inventory_invoice.dto.transaction.metadata.railMetadata;

import com.navan.travel_inventory_invoice.dto.transaction.metadata.TransactionMetadataDto;

import java.util.List;

public record RailMetadataDto(
        List<RailLeg> legs
)implements TransactionMetadataDto {
}
