package com.freelancemusiccrm.controller;

import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.freelancemusiccrm.dto.invoice.InvoiceCreateDto;
import com.freelancemusiccrm.dto.invoice.InvoiceResponseDto;
import com.freelancemusiccrm.service.InvoiceEmailService;
import com.freelancemusiccrm.service.InvoiceService;
import com.freelancemusiccrm.service.PdfGeneratorService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final PdfGeneratorService pdfGeneratorService;
    private final InvoiceEmailService invoiceEmailService;

    public InvoiceController(
            InvoiceService invoiceService,
            PdfGeneratorService pdfGeneratorService,
            InvoiceEmailService invoiceEmailService
    ) {
        this.invoiceService = invoiceService;
        this.pdfGeneratorService = pdfGeneratorService;
        this.invoiceEmailService = invoiceEmailService;
    }

    @GetMapping
    public ResponseEntity<List<InvoiceResponseDto>> findAll() {
        return ResponseEntity.ok(invoiceService.findAll());
    }

    @PostMapping
    public ResponseEntity<InvoiceResponseDto> create(
            @Valid @RequestBody InvoiceCreateDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceService.create(request));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) {
        InvoiceResponseDto invoice = invoiceService.findById(id);
        byte[] pdf = pdfGeneratorService.generateInvoicePdf(invoice);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=invoice-" + id + ".pdf")
            .contentType(Objects.requireNonNull(MediaType.APPLICATION_PDF))
                .body(pdf);
    }

    @GetMapping("/{id}/pdf/preview")
    public ResponseEntity<byte[]> previewPdf(@PathVariable Long id) {
        InvoiceResponseDto invoice = invoiceService.findById(id);
        byte[] pdf = pdfGeneratorService.generateInvoicePdf(invoice);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=invoice-" + id + ".pdf")
            .contentType(Objects.requireNonNull(MediaType.APPLICATION_PDF))
                .body(pdf);
    }

    @PostMapping("/{id}/send-email")
    public ResponseEntity<Void> sendEmail(@PathVariable Long id) {
        invoiceEmailService.sendInvoiceEmail(id);
        return ResponseEntity.noContent().build();
    }
}
