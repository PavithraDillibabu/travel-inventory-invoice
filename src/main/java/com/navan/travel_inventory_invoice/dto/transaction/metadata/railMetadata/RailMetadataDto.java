package com.navan.travel_inventory_invoice.dto.transaction.metadata.railMetadata;

import com.navan.travel_inventory_invoice.dto.transaction.metadata.TransactionMetadataDto;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record RailMetadataDto(
        @NotEmpty List<@NotNull @Valid RailLeg> legs
)implements TransactionMetadataDto {
}
