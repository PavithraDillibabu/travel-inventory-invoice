package com.navan.travel_inventory_invoice.mapper;

import com.navan.travel_inventory_invoice.dto.invoice.InvoiceLineResponseDto;
import com.navan.travel_inventory_invoice.dto.invoice.InvoiceResponseDto;
import com.navan.travel_inventory_invoice.dto.transaction.metadata.flightMetadata.FlightMetadataDto;
import com.navan.travel_inventory_invoice.dto.transaction.metadata.hotelMetadata.HotelMetadataDto;
import com.navan.travel_inventory_invoice.dto.transaction.metadata.navanFeeMetadata.NavanFeeMetadataDto;
import com.navan.travel_inventory_invoice.dto.transaction.metadata.railMetadata.RailMetadataDto;
import com.navan.travel_inventory_invoice.dto.transaction.metadata.TransactionMetadataDto;
import com.navan.travel_inventory_invoice.dto.transaction.FeeLineDto;
import com.navan.travel_inventory_invoice.dto.transaction.TaxLineDto;
import com.navan.travel_inventory_invoice.dto.transaction.TransactionType;
import com.navan.travel_inventory_invoice.entity.invoice.Invoice;
import com.navan.travel_inventory_invoice.entity.invoice.InvoiceFee;
import com.navan.travel_inventory_invoice.entity.invoice.InvoiceLine;
import com.navan.travel_inventory_invoice.entity.invoice.InvoiceTax;
import com.navan.travel_inventory_invoice.entity.transaction.Transaction;
import com.navan.travel_inventory_invoice.entity.transaction.TransactionFee;
import com.navan.travel_inventory_invoice.entity.transaction.TransactionTax;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class InvoiceMapper {
    private static final int MONEY_SCALE = 2;
    private final ObjectMapper objectMapper;

    public InvoiceMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Invoice toEntity(List<Transaction> transactions) {
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID().toString());
        invoice.setCurrency(transactions.get(0).getCurrency());
        invoice.setCreatedAt(OffsetDateTime.now());

        long totalMinor = 0;
        Map<TaxKey, Long> taxRollup = new LinkedHashMap<>();
        Map<String, Long> feeRollup = new LinkedHashMap<>();
        int lineNumber = 1;
        for (Transaction transaction : transactions) {
            totalMinor = Math.addExact(totalMinor, transaction.getTotalMinor());
            InvoiceLine line = new InvoiceLine();
            line.setLineNumber(lineNumber++);
            line.setTransactionId(transaction.getId());
            line.setType(transaction.getType());
            line.setExternalId(transaction.getExternalId());
            line.setCurrency(transaction.getCurrency());
            line.setOccurredAt(transaction.getOccurredAt());
            line.setTotalMinor(transaction.getTotalMinor());
            line.setMetadata(transaction.getMetadata());

            List<TaxLineDto> taxes = transaction.getTaxLines().stream()
                    .map(tax -> toTaxDto(tax, taxRollup)).toList();
            List<FeeLineDto> fees = transaction.getFeeLines().stream()
                    .map(fee -> toFeeDto(fee, feeRollup)).toList();
            line.setTaxLinesSnapshot(writeJson(taxes));
            line.setFeeLinesSnapshot(writeJson(fees));
            invoice.addTransactionLine(line);
        }
        invoice.setTotalMinor(totalMinor);

        taxRollup.forEach((key, amount) -> {
            InvoiceTax tax = new InvoiceTax();
            tax.setName(key.name());
            tax.setRate(key.rate());
            tax.setAmountMinor(amount);
            invoice.addTax(tax);
        });
        feeRollup.forEach((name, amount) -> {
            InvoiceFee fee = new InvoiceFee();
            fee.setName(name);
            fee.setAmountMinor(amount);
            invoice.addFee(fee);
        });
        return invoice;
    }

    public void replaceSnapshot(Invoice invoice, List<Transaction> transactions) {
        Invoice updatedInvoice = toEntity(transactions);
        invoice.setCurrency(updatedInvoice.getCurrency());
        invoice.setTotalMinor(updatedInvoice.getTotalMinor());

        for (InvoiceLine line : updatedInvoice.getTransactionLines()) {
            invoice.addTransactionLine(line);
        }
        for (InvoiceTax tax : updatedInvoice.getTaxLines()) {
            invoice.addTax(tax);
        }
        for (InvoiceFee fee : updatedInvoice.getFeeLines()) {
            invoice.addFee(fee);
        }
    }

    public InvoiceResponseDto toResponse(Invoice invoice) {
        List<InvoiceLineResponseDto> lines = invoice.getTransactionLines().stream()
                .map(line -> new InvoiceLineResponseDto(
                        line.getTransactionId(),
                        TransactionType.from(line.getType()),
                        line.getExternalId(),
                        line.getCurrency(),
                        line.getOccurredAt(),
                        toMoney(line.getTotalMinor()),
                        readMetadata(line.getType(), line.getMetadata()),
                        readJson(line.getTaxLinesSnapshot(), new TypeReference<>() {}),
                        readJson(line.getFeeLinesSnapshot(), new TypeReference<>() {})
                )).toList();
        List<TaxLineDto> taxes = invoice.getTaxLines().stream()
                .map(tax -> new TaxLineDto(tax.getName(), tax.getRate(), toMoney(tax.getAmountMinor())))
                .toList();
        List<FeeLineDto> fees = invoice.getFeeLines().stream()
                .map(fee -> new FeeLineDto(fee.getName(), toMoney(fee.getAmountMinor())))
                .toList();

        return new InvoiceResponseDto(invoice.getId(), invoice.getCurrency(),
                toMoney(invoice.getTotalMinor()), invoice.getCreatedAt(), lines, taxes, fees);
    }

    private TaxLineDto toTaxDto(TransactionTax tax, Map<TaxKey, Long> rollup) {
        BigDecimal amount = toMoney(tax.getAmountMinor());
        rollup.merge(new TaxKey(tax.getName(), tax.getRate()), tax.getAmountMinor(), Math::addExact);
        return new TaxLineDto(tax.getName(), tax.getRate(), amount);
    }

    private FeeLineDto toFeeDto(TransactionFee fee, Map<String, Long> rollup) {
        rollup.merge(fee.getName(), fee.getAmountMinor(), Math::addExact);
        return new FeeLineDto(fee.getName(), toMoney(fee.getAmountMinor()));
    }

    private TransactionMetadataDto readMetadata(String type, String value) {
        try {
            return switch (type) {
                case "flight" -> objectMapper.readValue(value, FlightMetadataDto.class);
                case "hotel" -> objectMapper.readValue(value, HotelMetadataDto.class);
                case "rail" -> objectMapper.readValue(value, RailMetadataDto.class);
                case "navan_fee" -> objectMapper.readValue(value, NavanFeeMetadataDto.class);
                default -> throw new IllegalArgumentException("Unknown transaction type: " + type);
            };
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Unable to deserialize invoice metadata", e);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Unable to serialize invoice snapshot", e);
        }
    }

    private <T> T readJson(String value, TypeReference<T> type) {
        try {
            return objectMapper.readValue(value, type);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Unable to read invoice snapshot", e);
        }
    }

    private BigDecimal toMoney(long minor) {
        return BigDecimal.valueOf(minor, MONEY_SCALE);
    }

    private record TaxKey(String name, BigDecimal rate) {}
}
