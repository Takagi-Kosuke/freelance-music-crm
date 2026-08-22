package com.freelancemusiccrm.dto.order;

public record OrderActionResponseDto(
        Long orderId,
        Long taskId,
        String message
) {
}
