package com.freelancemusiccrm.dto.auth;

public record LoginResponseDto(
        Long workerId,
        String workerName,
        String workerEmail,
        String token,
        String message
) {
}
