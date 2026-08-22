package com.freelancemusiccrm.dto.quote;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.freelancemusiccrm.entity.TokenStatus;

public record QuoteResponseDto(
        Long id,
        Long quoteRequestId,
        BigDecimal amount,
        LocalDate responseDeliveryDate,
        String responseComment,
        String approvalToken,
        TokenStatus tokenStatus,
        LocalDateTime createdAt
) {
}
