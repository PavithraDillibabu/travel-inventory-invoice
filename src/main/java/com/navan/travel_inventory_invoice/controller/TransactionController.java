package com.navan.travel_inventory_invoice.controller;

import com.navan.travel_inventory_invoice.dto.transaction.TransactionRequestDto;
import com.navan.travel_inventory_invoice.dto.transaction.TransactionPatchRequestDto;
import com.navan.travel_inventory_invoice.dto.transaction.TransactionResponseDto;
import com.navan.travel_inventory_invoice.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/transactions")
public class TransactionController {
    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<TransactionResponseDto> createTransaction(
            @Valid @RequestBody TransactionRequestDto request
    ) {
        TransactionResponseDto response = transactionService.createTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponseDto> getTransaction(@PathVariable String id) {
        TransactionResponseDto response = transactionService.getTransaction(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TransactionResponseDto> patchTransaction(
            @PathVariable String id,
            @Valid @RequestBody TransactionPatchRequestDto patch
    ) {
        return ResponseEntity.ok(transactionService.patchTransaction(id, patch));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable String id) {
        transactionService.deleteTransaction(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public List<TransactionResponseDto> getTransactions(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {

        return transactionService.getTransactions(limit, offset);
    }
}
