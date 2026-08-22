package com.freelancemusiccrm.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quote_response_id", nullable = false, unique = true)
    private QuoteResponse quoteResponse;

    @Column(name = "subject", nullable = false)
    private String subject;

    @Column(name = "client_name", nullable = false)
    private String clientName;

    @Column(name = "client_email")
    private String clientEmail;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private OrderCategory category;

    @Column(name = "desired_delivery_date", nullable = false)
    private LocalDate desiredDeliveryDate;

    @Column(name = "file_path_url", columnDefinition = "TEXT")
    private String filePathUrl;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Order() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public QuoteResponse getQuoteResponse() { return quoteResponse; }
    public void setQuoteResponse(QuoteResponse quoteResponse) { this.quoteResponse = quoteResponse; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public String getClientEmail() { return clientEmail; }
    public void setClientEmail(String clientEmail) { this.clientEmail = clientEmail; }

    public OrderCategory getCategory() { return category; }
    public void setCategory(OrderCategory category) { this.category = category; }

    public LocalDate getDesiredDeliveryDate() { return desiredDeliveryDate; }
    public void setDesiredDeliveryDate(LocalDate desiredDeliveryDate) { this.desiredDeliveryDate = desiredDeliveryDate; }

    public String getFilePathUrl() { return filePathUrl; }
    public void setFilePathUrl(String filePathUrl) { this.filePathUrl = filePathUrl; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
