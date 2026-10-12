package com.navan.travel_inventory_invoice.dto.transaction.metadata.navanFeeMetadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.navan.travel_inventory_invoice.dto.transaction.metadata.TransactionMetadataDto;

public record NavanFeeMetadataDto(
        @JsonProperty("fee_kind") String feeKind,
        String description,
        @JsonProperty("related_external_id") String relatedExternalId
) implements TransactionMetadataDto {
}
