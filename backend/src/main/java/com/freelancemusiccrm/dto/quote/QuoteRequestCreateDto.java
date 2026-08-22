package com.freelancemusiccrm.dto.quote;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record QuoteRequestCreateDto(
        @NotBlank(message = "依頼件名は必須です")
        String subject,

        @NotBlank(message = "依頼者名は必須です")
        String clientName,

        @Email(message = "依頼者メールアドレスの形式が不正です")
        String clientEmail,

        @NotNull(message = "依頼区分は必須です")
        Long categoryId,

        @NotNull(message = "希望納期は必須です")
        @Future(message = "希望納期は未来日を指定してください")
        LocalDate desiredDeliveryDate,

        @Pattern(regexp = "^https?://.+$", message = "ファイルURLはhttp://またはhttps://で始まる必要があります")
        String filePathUrl,

        @Size(max = 1000, message = "コメントは1000文字以内で入力してください")
        String comment
) {
}
