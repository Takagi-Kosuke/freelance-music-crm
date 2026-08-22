package com.freelancemusiccrm.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.freelancemusiccrm.dto.quote.QuoteResponseCreateDto;
import com.freelancemusiccrm.dto.quote.QuoteResponseDto;
import com.freelancemusiccrm.entity.QuoteRequest;
import com.freelancemusiccrm.entity.QuoteRequestStatus;
import com.freelancemusiccrm.entity.QuoteResponse;
import com.freelancemusiccrm.entity.TokenStatus;
import com.freelancemusiccrm.exception.ConflictException;
import com.freelancemusiccrm.exception.ResourceNotFoundException;
import com.freelancemusiccrm.repository.QuoteRequestRepository;
import com.freelancemusiccrm.repository.QuoteResponseRepository;

@Service
public class QuoteResponseService {

    private final QuoteResponseRepository quoteResponseRepository;
    private final QuoteRequestRepository quoteRequestRepository;

    public QuoteResponseService(QuoteResponseRepository quoteResponseRepository,
                                QuoteRequestRepository quoteRequestRepository) {
        this.quoteResponseRepository = quoteResponseRepository;
        this.quoteRequestRepository = quoteRequestRepository;
    }

    @Transactional
    public QuoteResponseDto create(QuoteResponseCreateDto dto) {
        QuoteRequest quoteRequest = quoteRequestRepository.findById(dto.quoteRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("見積依頼が見つかりません"));

        quoteResponseRepository.findByQuoteRequestId(dto.quoteRequestId())
                .ifPresent(existing -> {
                    throw new ConflictException("この見積依頼には既に回答が存在します");
                });

        QuoteResponse response = new QuoteResponse();
        response.setQuoteRequest(quoteRequest);
        response.setAmount(dto.amount());
        response.setResponseDeliveryDate(dto.responseDeliveryDate());
        response.setResponseComment(dto.responseComment());
        response.setApprovalToken(generateUniqueToken());
        response.setTokenStatus(TokenStatus.ACTIVE);

        QuoteResponse saved = quoteResponseRepository.save(response);

        quoteRequest.setStatus(QuoteRequestStatus.RESPONDED);
        quoteRequestRepository.save(quoteRequest);

        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public QuoteResponseDto findByToken(String token) {
        QuoteResponse quoteResponse = quoteResponseRepository.findByApprovalToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("見積回答が見つかりません"));
        return toDto(quoteResponse);
    }

    @Transactional(readOnly = true)
    public QuoteResponseDto findByQuoteRequestId(Long quoteRequestId) {
        QuoteResponse quoteResponse = quoteResponseRepository.findByQuoteRequestId(quoteRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("見積回答が見つかりません"));
        return toDto(quoteResponse);
    }

    private String generateUniqueToken() {
        String token;
        do {
            token = UUID.randomUUID().toString();
        } while (quoteResponseRepository.findByApprovalToken(token).isPresent());
        return token;
    }

    private QuoteResponseDto toDto(QuoteResponse quoteResponse) {
        return new QuoteResponseDto(
                quoteResponse.getId(),
                quoteResponse.getQuoteRequest().getId(),
                quoteResponse.getAmount(),
                quoteResponse.getResponseDeliveryDate(),
                quoteResponse.getResponseComment(),
                quoteResponse.getApprovalToken(),
                quoteResponse.getTokenStatus(),
                quoteResponse.getCreatedAt()
        );
    }
}
