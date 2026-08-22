package com.freelancemusiccrm.dto.auth;

public record CsrfTokenResponseDto(
        String token,
        String headerName,
        String parameterName
) {
}
