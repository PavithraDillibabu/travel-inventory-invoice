package com.navan.travel_inventory_invoice.controller;

import com.navan.travel_inventory_invoice.dto.invoice.InvoiceRequestDto;
import com.navan.travel_inventory_invoice.dto.invoice.InvoiceResponseDto;
import com.navan.travel_inventory_invoice.service.InvoiceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/invoices")
public class InvoiceController {
    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PostMapping
    public ResponseEntity<InvoiceResponseDto> createInvoice(@Valid @RequestBody InvoiceRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(invoiceService.createInvoice(request));
    }

    @GetMapping("/{id}")
    public InvoiceResponseDto getInvoice(@PathVariable String id) {
        return invoiceService.getInvoice(id);
    }

    @GetMapping
    public List<InvoiceResponseDto> getInvoices(@RequestParam(defaultValue = "50") int limit, @RequestParam(defaultValue = "0") int offset) {
        return invoiceService.getInvoices(limit, offset);
    }

    @PatchMapping("/{id}")
    public InvoiceResponseDto patchInvoice(@PathVariable String id, @Valid @RequestBody InvoiceRequestDto request) {
        return invoiceService.patchInvoice(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInvoice(@PathVariable String id) {
        invoiceService.deleteInvoice(id);
        return ResponseEntity.noContent().build();
    }
}
