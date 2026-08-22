package com.freelancemusiccrm.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrderCategoryUpsertDto(
        @NotBlank(message = "区分名は必須です")
        @Size(min = 1, max = 50, message = "区分名は1文字以上50文字以内で入力してください")
        String name
) {
}
