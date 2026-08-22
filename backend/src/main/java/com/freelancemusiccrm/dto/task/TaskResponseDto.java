package com.freelancemusiccrm.dto.task;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.freelancemusiccrm.entity.TaskStatus;

public record TaskResponseDto(
        Long id,
        Long orderId,
        Long categoryId,
        String categoryName,
        String orderSubject,
        String clientName,
        String clientEmail,
        LocalDate desiredDeliveryDate,
        String filePathUrl,
        String comment,
        String folderPath,
        TaskStatus status,
        LocalDateTime statusUpdatedAt,
        LocalDateTime createdAt
) {
}
