package com.freelancemusiccrm.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.freelancemusiccrm.dto.quote.QuoteRequestCreateDto;
import com.freelancemusiccrm.dto.quote.QuoteRequestCreateResponseDto;
import com.freelancemusiccrm.dto.quote.QuoteRequestResponseDto;
import com.freelancemusiccrm.service.QuoteRequestService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/quote-requests")
public class QuoteRequestController {

    private final QuoteRequestService quoteRequestService;

    public QuoteRequestController(QuoteRequestService quoteRequestService) {
        this.quoteRequestService = quoteRequestService;
    }

    @PostMapping
    public ResponseEntity<QuoteRequestCreateResponseDto> create(@Valid @RequestBody QuoteRequestCreateDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(quoteRequestService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<QuoteRequestResponseDto>> findAll() {
        return ResponseEntity.ok(quoteRequestService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuoteRequestResponseDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(quoteRequestService.findById(id));
    }
}
