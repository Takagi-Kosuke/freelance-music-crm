package com.freelancemusiccrm.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "quote_responses")
public class QuoteResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quote_request_id", nullable = false, unique = true)
    private QuoteRequest quoteRequest;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "response_delivery_date", nullable = false)
    private LocalDate responseDeliveryDate;

    @Column(name = "response_comment", columnDefinition = "TEXT")
    private String responseComment;

    @Column(name = "approval_token", nullable = false, unique = true)
    private String approvalToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "token_status", nullable = false)
    private TokenStatus tokenStatus = TokenStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public QuoteResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public QuoteRequest getQuoteRequest() { return quoteRequest; }
    public void setQuoteRequest(QuoteRequest quoteRequest) { this.quoteRequest = quoteRequest; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public LocalDate getResponseDeliveryDate() { return responseDeliveryDate; }
    public void setResponseDeliveryDate(LocalDate responseDeliveryDate) { this.responseDeliveryDate = responseDeliveryDate; }

    public String getResponseComment() { return responseComment; }
    public void setResponseComment(String responseComment) { this.responseComment = responseComment; }

    public String getApprovalToken() { return approvalToken; }
    public void setApprovalToken(String approvalToken) { this.approvalToken = approvalToken; }

    public TokenStatus getTokenStatus() { return tokenStatus; }
    public void setTokenStatus(TokenStatus tokenStatus) { this.tokenStatus = tokenStatus; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
