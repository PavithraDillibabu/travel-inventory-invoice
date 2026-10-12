package com.navan.travel_inventory_invoice.service;

import com.navan.travel_inventory_invoice.dto.transaction.TransactionRequestDto;
import com.navan.travel_inventory_invoice.dto.transaction.TransactionPatchRequestDto;
import com.navan.travel_inventory_invoice.dto.transaction.TransactionResponseDto;
import com.navan.travel_inventory_invoice.entity.transaction.Transaction;
import com.navan.travel_inventory_invoice.mapper.TransactionMapper;
import com.navan.travel_inventory_invoice.repository.InvoiceLineRepository;
import com.navan.travel_inventory_invoice.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.navan.travel_inventory_invoice.pagination.OffsetLimitPageable;

import java.util.List;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final InvoiceLineRepository invoiceLineRepository;
    private final TransactionMapper transactionMapper;

    public TransactionService(
            TransactionRepository transactionRepository,
            InvoiceLineRepository invoiceLineRepository,
            TransactionMapper transactionMapper
    ) {
        this.transactionRepository = transactionRepository;
        this.invoiceLineRepository = invoiceLineRepository;
        this.transactionMapper = transactionMapper;
    }

    @Transactional
    public TransactionResponseDto createTransaction(TransactionRequestDto request) {
        if (transactionRepository.existsByExternalId(request.externalId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Transaction already exists with external_id: " + request.externalId()
            );
        }

        Transaction transaction = transactionMapper.toEntity(request);
        Transaction savedTransaction = transactionRepository.saveAndFlush(transaction);
        return transactionMapper.toResponse(savedTransaction);
    }

    @Transactional(readOnly = true)
    public TransactionResponseDto getTransaction(String id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Transaction not found: " + id
                ));
        return transactionMapper.toResponse(transaction);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponseDto> getTransactions(int limit, int offset) {
        if (limit < 1 || limit > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"limit must be between 1 and 100");
        }
        if (offset < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"offset must be zero or greater");
        }

        Pageable pageable = new OffsetLimitPageable(
                offset,
                limit,
                Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id"))
        );
        return transactionRepository.findAll(pageable).getContent().stream()
                .map(transactionMapper::toResponse)
                .toList();
    }

    @Transactional
    public TransactionResponseDto patchTransaction(String id, TransactionPatchRequestDto patch) {
        Transaction transaction = transactionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Transaction not found: " + id
                ));

        if (invoiceLineRepository.existsByTransactionId(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Transaction is included on an invoice: " + id
            );
        }

        transactionMapper.applyPatch(transaction, patch);
        return transactionMapper.toResponse(transaction);
    }

    @Transactional
    public void deleteTransaction(String id) {
        Transaction transaction = transactionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Transaction not found: " + id
                ));

        if (invoiceLineRepository.existsByTransactionId(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Transaction is included on an invoice: " + id
            );
        }

        transactionRepository.delete(transaction);
    }
}
