package com.freelancemusiccrm.dto.quote;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.freelancemusiccrm.entity.QuoteRequestStatus;

public record QuoteRequestResponseDto(
        Long id,
        String subject,
        String clientName,
        String clientEmail,
        Long categoryId,
        String categoryName,
        LocalDate desiredDeliveryDate,
        String filePathUrl,
        String comment,
        QuoteRequestStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
