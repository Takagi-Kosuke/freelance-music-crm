package com.freelancemusiccrm.dto.settings;

public record SettingsResponseDto(
        String discordWebhookUrl,
        boolean discordEnabled,
        String smtpHost,
        Integer smtpPort,
        String smtpUsername,
        boolean mailEnabled,
        boolean hasSmtpPassword
) {
}
