package com.freelancemusiccrm.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.freelancemusiccrm.entity.QuoteResponse;

public interface QuoteResponseRepository extends JpaRepository<QuoteResponse, Long> {

    Optional<QuoteResponse> findByApprovalToken(String token);

    Optional<QuoteResponse> findByQuoteRequestId(Long quoteRequestId);
}
