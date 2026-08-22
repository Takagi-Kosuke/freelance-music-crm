package com.freelancemusiccrm.service;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.freelancemusiccrm.dto.settings.SettingsResponseDto;
import com.freelancemusiccrm.dto.settings.SettingsUpdateDto;
import com.freelancemusiccrm.entity.Worker;
import com.freelancemusiccrm.entity.WorkerSettings;
import com.freelancemusiccrm.exception.AuthenticationFailedException;
import com.freelancemusiccrm.exception.ResourceNotFoundException;
import com.freelancemusiccrm.repository.WorkerRepository;
import com.freelancemusiccrm.repository.WorkerSettingsRepository;

@Service
public class SettingsService {

    private final WorkerRepository workerRepository;
    private final WorkerSettingsRepository workerSettingsRepository;
    private final SecretEncryptionService secretEncryptionService;

    public SettingsService(
            WorkerRepository workerRepository,
            WorkerSettingsRepository workerSettingsRepository,
            SecretEncryptionService secretEncryptionService
    ) {
        this.workerRepository = workerRepository;
        this.workerSettingsRepository = workerSettingsRepository;
        this.secretEncryptionService = secretEncryptionService;
    }

    @Transactional(readOnly = true)
    public SettingsResponseDto getCurrentSettings() {
        Worker worker = getCurrentWorker();
        WorkerSettings settings = workerSettingsRepository.findByWorkerId(worker.getId())
                .orElseGet(() -> defaultSettings(worker));
        return toDto(settings);
    }

    @Transactional
    public SettingsResponseDto updateCurrentSettings(SettingsUpdateDto request) {
        Worker worker = getCurrentWorker();
        WorkerSettings settings = workerSettingsRepository.findByWorkerId(worker.getId())
                .orElseGet(() -> {
                    WorkerSettings created = new WorkerSettings();
                    created.setWorker(worker);
                    return created;
                });

        settings.setDiscordWebhookUrl(blankToNull(request.discordWebhookUrl()));
        settings.setDiscordEnabled(request.discordEnabled());
        settings.setSmtpHost(blankToNull(request.smtpHost()));
        settings.setSmtpPort(request.smtpPort());
        settings.setSmtpUsername(blankToNull(request.smtpUsername()));
        settings.setMailEnabled(request.mailEnabled());

        if (request.smtpPassword() != null && !request.smtpPassword().isBlank()) {
            settings.setSmtpPasswordEncrypted(secretEncryptionService.encrypt(request.smtpPassword()));
        }

        WorkerSettings saved = workerSettingsRepository.save(settings);
        return toDto(saved);
    }

    private SettingsResponseDto toDto(WorkerSettings settings) {
        return new SettingsResponseDto(
                settings.getDiscordWebhookUrl(),
                settings.isDiscordEnabled(),
                settings.getSmtpHost(),
                settings.getSmtpPort(),
                settings.getSmtpUsername(),
                settings.isMailEnabled(),
                settings.getSmtpPasswordEncrypted() != null && !settings.getSmtpPasswordEncrypted().isBlank()
        );
    }

    private Worker getCurrentWorker() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || !(authentication.getPrincipal() instanceof String principalEmail)) {
            throw new AuthenticationFailedException("認証が必要です");
        }

        return workerRepository.findByEmail(principalEmail)
                .orElseThrow(() -> new ResourceNotFoundException("作業者情報が見つかりません"));
    }

    private WorkerSettings defaultSettings(Worker worker) {
        WorkerSettings settings = new WorkerSettings();
        settings.setWorker(worker);
        settings.setDiscordEnabled(false);
        settings.setMailEnabled(false);
        return settings;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
