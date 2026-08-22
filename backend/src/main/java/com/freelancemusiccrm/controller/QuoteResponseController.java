package com.freelancemusiccrm.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.freelancemusiccrm.dto.quote.QuoteResponseCreateDto;
import com.freelancemusiccrm.dto.quote.QuoteResponseDto;
import com.freelancemusiccrm.service.QuoteResponseService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/quote-responses")
public class QuoteResponseController {

    private final QuoteResponseService quoteResponseService;

    public QuoteResponseController(QuoteResponseService quoteResponseService) {
        this.quoteResponseService = quoteResponseService;
    }

    @PostMapping
    public ResponseEntity<QuoteResponseDto> create(@Valid @RequestBody QuoteResponseCreateDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(quoteResponseService.create(request));
    }

    @GetMapping("/token/{token}")
    public ResponseEntity<QuoteResponseDto> findByToken(@PathVariable String token) {
        return ResponseEntity.ok(quoteResponseService.findByToken(token));
    }

    @GetMapping("/quote-request/{quoteRequestId}")
    public ResponseEntity<QuoteResponseDto> findByQuoteRequestId(@PathVariable Long quoteRequestId) {
        return ResponseEntity.ok(quoteResponseService.findByQuoteRequestId(quoteRequestId));
    }
}
