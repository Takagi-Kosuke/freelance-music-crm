package com.freelancemusiccrm.dto.invoice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record InvoiceResponseDto(
        Long id,
        Long taskId,
        String subject,
        String clientName,
        String clientEmail,
        String categoryName,
        LocalDate deliveryDate,
        BigDecimal amount,
        LocalDate issueDate,
        String workerName,
        String workerContact,
        LocalDateTime createdAt
) {
}
