package com.freelancemusiccrm.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequestDto(
        @NotBlank(message = "メールアドレスは必須です")
        @Email(message = "メールアドレスの形式が不正です")
        String email,

        @NotBlank(message = "パスワードは必須です")
        String password
) {
}
