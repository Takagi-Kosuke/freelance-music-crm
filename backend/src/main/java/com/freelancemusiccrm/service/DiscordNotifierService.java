package com.freelancemusiccrm.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.freelancemusiccrm.entity.Order;
import com.freelancemusiccrm.entity.QuoteRequest;
import com.freelancemusiccrm.entity.WorkerSettings;
import com.freelancemusiccrm.repository.WorkerSettingsRepository;

@Service
public class DiscordNotifierService {

    private static final Logger logger = LoggerFactory.getLogger(DiscordNotifierService.class);

    private final WebClient webClient;
    private final WorkerSettingsRepository workerSettingsRepository;

    public DiscordNotifierService(WebClient.Builder webClientBuilder, WorkerSettingsRepository workerSettingsRepository) {
        this.webClient = webClientBuilder.build();
        this.workerSettingsRepository = workerSettingsRepository;
    }

    @Async
    public void notifyQuoteRequestCreated(QuoteRequest quoteRequest) {
        sendBestEffort("新しい見積依頼が届きました: " + quoteRequest.getSubject());
    }

    @Async
    public void notifyOrderCreated(Order order) {
        sendBestEffort("正式依頼が発行されました: " + order.getSubject());
    }

    @Async
    public void notifyTaskCompleted(String subject) {
        sendBestEffort("タスクが完了しました: " + subject);
    }

    private void sendBestEffort(String content) {
        WorkerSettings settings = workerSettingsRepository.findAll().stream().findFirst().orElse(null);
        if (settings == null || !settings.isDiscordEnabled()) {
            return;
        }

        String webhookUrl = settings.getDiscordWebhookUrl();
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }

        webClient.post()
                .uri(webhookUrl)
                .bodyValue(Map.of("content", content))
                .retrieve()
                .toBodilessEntity()
                .doOnError(error -> logger.warn("Discord通知の送信に失敗しました: {}", error.getMessage(), error))
                .subscribe();
    }
}