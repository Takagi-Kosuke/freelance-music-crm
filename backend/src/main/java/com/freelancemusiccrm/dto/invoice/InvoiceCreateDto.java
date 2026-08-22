package com.freelancemusiccrm.dto.invoice;

import jakarta.validation.constraints.NotNull;

public record InvoiceCreateDto(
        @NotNull(message = "taskIdは必須です")
        Long taskId
) {
}
