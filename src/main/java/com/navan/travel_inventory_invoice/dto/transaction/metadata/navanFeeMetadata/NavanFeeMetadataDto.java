package com.navan.travel_inventory_invoice.dto.transaction.metadata.navanFeeMetadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.navan.travel_inventory_invoice.dto.transaction.metadata.TransactionMetadataDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record NavanFeeMetadataDto(
        @JsonProperty("fee_kind") @NotBlank @Pattern(regexp = "trip_fee|agent_call_fee") String feeKind,
        String description,
        @JsonProperty("related_external_id") String relatedExternalId
) implements TransactionMetadataDto {
}
