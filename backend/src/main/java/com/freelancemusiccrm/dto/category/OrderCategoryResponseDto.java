package com.freelancemusiccrm.dto.category;

public record OrderCategoryResponseDto(
        Long id,
        String name,
        boolean isDefault
) {
}
