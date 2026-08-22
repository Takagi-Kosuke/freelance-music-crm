package com.freelancemusiccrm.dto.quote;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record QuoteResponseCreateDto(
        @NotNull(message = "見積依頼IDは必須です")
        Long quoteRequestId,

        @NotNull(message = "見積金額は必須です")
        @DecimalMin(value = "0", inclusive = true, message = "見積金額は0以上である必要があります")
        @Digits(integer = 10, fraction = 0, message = "見積金額は整数で入力してください")
        BigDecimal amount,

        @NotNull(message = "回答納期は必須です")
        LocalDate responseDeliveryDate,

        @Size(max = 1000, message = "回答コメントは1000文字以内で入力してください")
        String responseComment
) {
}
