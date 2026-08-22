package com.freelancemusiccrm.dto.quote;

import com.freelancemusiccrm.entity.QuoteRequestStatus;

public record QuoteRequestCreateResponseDto(
        Long id,
        QuoteRequestStatus status,
        String message
) {
}
