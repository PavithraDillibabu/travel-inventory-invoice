package com.navan.travel_inventory_invoice.service;

import com.navan.travel_inventory_invoice.dto.invoice.InvoiceRequestDto;
import com.navan.travel_inventory_invoice.dto.invoice.InvoiceResponseDto;
import com.navan.travel_inventory_invoice.entity.invoice.Invoice;
import com.navan.travel_inventory_invoice.entity.transaction.Transaction;
import com.navan.travel_inventory_invoice.mapper.InvoiceMapper;
import com.navan.travel_inventory_invoice.pagination.OffsetLimitPageable;
import com.navan.travel_inventory_invoice.repository.InvoiceLineRepository;
import com.navan.travel_inventory_invoice.repository.InvoiceRepository;
import com.navan.travel_inventory_invoice.repository.TransactionRepository;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Service
public class InvoiceService {
    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineRepository invoiceLineRepository;
    private final TransactionRepository transactionRepository;
    private final InvoiceMapper invoiceMapper;

    public InvoiceService(
            InvoiceRepository invoiceRepository,
            InvoiceLineRepository invoiceLineRepository,
            TransactionRepository transactionRepository,
            InvoiceMapper invoiceMapper) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceLineRepository = invoiceLineRepository;
        this.transactionRepository = transactionRepository;
        this.invoiceMapper = invoiceMapper;
    }

    @Transactional
    public InvoiceResponseDto createInvoice(InvoiceRequestDto request) {
        List<String> ids = request == null ? List.of() : request.transactionIds();
        if (ids.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Provide transaction_ids to create an invoice");
        }
        if (new HashSet<>(ids).size() != ids.size()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A transaction may appear only once on an invoice");
        }

        // Lock ID order to serialize against transaction PATCH/DELETE
        // and avoid different lock orders for overlapping invoice requests.
        List<String> lockIds = ids.stream().sorted().toList();
        List<Transaction> found = transactionRepository.findAllByIdForUpdate(lockIds);
        if (found.size() != ids.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "One or more transactions were not found");
        }
        Map<String, Transaction> transactionsById  = new HashMap<>();
        found.forEach(transaction -> transactionsById .put(transaction.getId(), transaction));
        List<Transaction> transactions = ids.stream().map(transactionsById ::get).toList();

        String currency = transactions.get(0).getCurrency();
        if (transactions.stream().anyMatch(transaction -> !currency.equals(transaction.getCurrency()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Transactions on an invoice must use the same currency");
        }
        if (ids.stream().anyMatch(invoiceLineRepository::existsByTransactionId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A transaction is already included on an invoice");
        }

        Invoice invoice = invoiceRepository.save(invoiceMapper.toEntity(transactions));
        return invoiceMapper.toResponse(invoice);
    }

    @Transactional(readOnly = true)
    public InvoiceResponseDto getInvoice(String id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Invoice not found: " + id));
        return invoiceMapper.toResponse(invoice);
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponseDto> getInvoices(int limit, int offset) {
        if (limit < 1 || limit > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "limit must be between 1 and 100"
            );
        }
        if (offset < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "offset must be zero or greater"
            );
        }

        OffsetLimitPageable pageable = new OffsetLimitPageable(
                offset,
                limit,
                Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id"))
        );
        return invoiceRepository.findAll(pageable).getContent().stream()
                .map(invoiceMapper::toResponse)
                .toList();
    }

    @Transactional
    public InvoiceResponseDto patchInvoice(String id, InvoiceRequestDto request) {
        Invoice invoice = invoiceRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Invoice not found: " + id));

        List<String> ids = request == null ? List.of() : request.transactionIds();
        if (ids == null || ids.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Provide transaction_ids to update an invoice");
        }
        if (new HashSet<>(ids).size() != ids.size()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A transaction may appear only once on an invoice");
        }

        // Lock ID order to serialize against transaction PATCH/DELETE
        // and avoid different lock orders for overlapping invoice requests.
        List<String> lockIds = ids.stream().sorted().toList();
        List<Transaction> found = transactionRepository.findAllByIdForUpdate(lockIds);
        if (found.size() != ids.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "One or more transactions were not found");
        }
        Map<String, Transaction> transactionsById = new HashMap<>();
        found.forEach(transaction -> transactionsById.put(transaction.getId(), transaction));
        List<Transaction> transactions = ids.stream().map(transactionsById::get).toList();

        String currency = transactions.get(0).getCurrency();
        if (transactions.stream().anyMatch(transaction -> !currency.equals(transaction.getCurrency()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Transactions on an invoice must use the same currency");
        }

        HashSet<String> currentInvoiceTransactionIds = new HashSet<>();
        invoice.getTransactionLines().forEach(line ->
                currentInvoiceTransactionIds.add(line.getTransactionId()));
        if (ids.stream().anyMatch(transactionId ->
                !currentInvoiceTransactionIds.contains(transactionId)
                        && invoiceLineRepository.existsByTransactionId(transactionId))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A transaction is already included on another invoice");
        }

        invoice.getTransactionLines().clear();
        invoice.getTaxLines().clear();
        invoice.getFeeLines().clear();
        invoiceRepository.flush();

        invoiceMapper.replaceSnapshot(invoice, transactions);
        return invoiceMapper.toResponse(invoice);
    }

    @Transactional
    public void deleteInvoice(String id) {
        Invoice invoice = invoiceRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Invoice not found: " + id));
        invoiceRepository.delete(invoice);
    }
}
