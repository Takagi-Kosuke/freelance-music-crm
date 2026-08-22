package com.freelancemusiccrm.dto.settings;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record SettingsUpdateDto(
        @Size(max = 255, message = "Discord Webhook URLは255文字以内で入力してください")
        String discordWebhookUrl,
        boolean discordEnabled,
        @Size(max = 255, message = "SMTPホストは255文字以内で入力してください")
        String smtpHost,
        @Min(value = 1, message = "SMTPポートは1以上で入力してください")
        @Max(value = 65535, message = "SMTPポートは65535以下で入力してください")
        Integer smtpPort,
        @Size(max = 255, message = "SMTPユーザー名は255文字以内で入力してください")
        String smtpUsername,
        @Size(max = 255, message = "SMTPパスワードは255文字以内で入力してください")
        String smtpPassword,
        boolean mailEnabled
) {
}
