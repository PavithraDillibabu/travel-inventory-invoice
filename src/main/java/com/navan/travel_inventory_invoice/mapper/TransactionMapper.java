package com.navan.travel_inventory_invoice.mapper;

import com.navan.travel_inventory_invoice.dto.transaction.FeeLineDto;
import com.navan.travel_inventory_invoice.dto.transaction.TaxLineDto;
import com.navan.travel_inventory_invoice.dto.transaction.TransactionRequestDto;
import com.navan.travel_inventory_invoice.dto.transaction.TransactionPatchRequestDto;
import com.navan.travel_inventory_invoice.dto.transaction.TransactionResponseDto;
import com.navan.travel_inventory_invoice.dto.transaction.TransactionType;
import com.navan.travel_inventory_invoice.dto.transaction.metadata.TransactionMetadataDto;
import com.navan.travel_inventory_invoice.dto.transaction.metadata.flightMetadata.FlightMetadataDto;
import com.navan.travel_inventory_invoice.dto.transaction.metadata.hotelMetadata.HotelMetadataDto;
import com.navan.travel_inventory_invoice.dto.transaction.metadata.railMetadata.RailMetadataDto;
import com.navan.travel_inventory_invoice.dto.transaction.metadata.navanFeeMetadata.NavanFeeMetadataDto;
import com.navan.travel_inventory_invoice.entity.transaction.Transaction;
import com.navan.travel_inventory_invoice.entity.transaction.TransactionFee;
import com.navan.travel_inventory_invoice.entity.transaction.TransactionTax;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Converts a validated create-transaction payload into its persistence entity graph. */
@Component
public class TransactionMapper {
    private static final int MONEY_SCALE = 2;

    private final ObjectMapper objectMapper;

    public TransactionMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Transaction toEntity(TransactionRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("Transaction request must not be null");
        }

        Transaction transaction = new Transaction();
        transaction.setId(UUID.randomUUID().toString());
        transaction.setType(request.type().value());
        transaction.setExternalId(request.externalId());
        transaction.setCurrency(request.currency());
        transaction.setOccurredAt(request.occurredAt());
        transaction.setTotalMinor(toMinorUnits(request.total()));
        transaction.setMetadata(serializeMetadata(request));
        transaction.setCreatedAt(OffsetDateTime.now());

        if (request.taxLines() != null) {
            request.taxLines().stream().map(this::toEntity).forEach(transaction::addTax);
        }
        if (request.feeLines() != null) {
            request.feeLines().stream().map(this::toEntity).forEach(transaction::addFee);
        }
        return transaction;
    }

    public TransactionResponseDto toResponse(Transaction transaction) {
        return new TransactionResponseDto(
                transaction.getId(),
                TransactionType.from(transaction.getType()),
                transaction.getExternalId(),
                transaction.getCurrency(),
                transaction.getOccurredAt(),
                BigDecimal.valueOf(transaction.getTotalMinor(), MONEY_SCALE),
                transaction.getTaxLines().stream()
                        .map(tax -> new TaxLineDto(tax.getName(), tax.getRate(),
                                BigDecimal.valueOf(tax.getAmountMinor(), MONEY_SCALE)))
                        .toList(),
                transaction.getFeeLines().stream()
                        .map(fee -> new FeeLineDto(fee.getName(),
                                BigDecimal.valueOf(fee.getAmountMinor(), MONEY_SCALE)))
                        .toList(),
                deserializeMetadata(transaction.getType(), transaction.getMetadata())
        );
    }

    public void applyPatch(Transaction transaction, TransactionPatchRequestDto patch) {
        if (patch.total() != null) {
            transaction.setTotalMinor(toMinorUnits(patch.total()));
        }
        if (patch.occurredAt() != null) {
            transaction.setOccurredAt(patch.occurredAt());
        }
        if (patch.taxLines() != null) {
            transaction.getTaxLines().clear();
            patch.taxLines().stream().map(this::toEntity).forEach(transaction::addTax);
        }
        if (patch.feeLines() != null) {
            transaction.getFeeLines().clear();
            patch.feeLines().stream().map(this::toEntity).forEach(transaction::addFee);
        }
    }

    private TransactionTax toEntity(TaxLineDto taxLine) {
        TransactionTax tax = new TransactionTax();
        tax.setName(taxLine.name());
        tax.setRate(taxLine.rate());
        tax.setAmountMinor(toMinorUnits(taxLine.amount()));
        return tax;
    }

    private TransactionFee toEntity(FeeLineDto feeLine) {
        TransactionFee fee = new TransactionFee();
        fee.setName(feeLine.name());
        fee.setAmountMinor(toMinorUnits(feeLine.amount()));
        return fee;
    }

    private String serializeMetadata(TransactionRequestDto request) {
        try {
            return objectMapper.writeValueAsString(request.metadata());
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Unable to serialize transaction metadata", e);
        }
    }

    private TransactionMetadataDto deserializeMetadata(String type, String metadata) {
        try {
            return switch (type) {
                case "flight" -> objectMapper.readValue(metadata, FlightMetadataDto.class);
                case "hotel" -> objectMapper.readValue(metadata, HotelMetadataDto.class);
                case "rail" -> objectMapper.readValue(metadata, RailMetadataDto.class);
                case "navan_fee" -> objectMapper.readValue(metadata, NavanFeeMetadataDto.class);
                default -> throw new IllegalArgumentException("Unknown transaction type: " + type);
            };
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Unable to deserialize transaction metadata", e);
        }
    }

    private long toMinorUnits(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Money amount must not be null");
        }
        try {
            return amount.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY)
                    .movePointRight(MONEY_SCALE)
                    .longValueExact();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Money amount must have at most two decimal places", e);
        }
    }
}
