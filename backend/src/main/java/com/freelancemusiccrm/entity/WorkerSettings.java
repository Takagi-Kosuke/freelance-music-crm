package com.freelancemusiccrm.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "worker_settings")
public class WorkerSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;

    @Column(name = "discord_webhook_url")
    private String discordWebhookUrl;

    @Column(name = "discord_enabled", nullable = false)
    private boolean discordEnabled = false;

    @Column(name = "smtp_host")
    private String smtpHost;

    @Column(name = "smtp_port")
    private Integer smtpPort;

    @Column(name = "smtp_username")
    private String smtpUsername;

    @Column(name = "smtp_password_encrypted")
    private String smtpPasswordEncrypted;

    @Column(name = "mail_enabled", nullable = false)
    private boolean mailEnabled = false;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public WorkerSettings() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Worker getWorker() { return worker; }
    public void setWorker(Worker worker) { this.worker = worker; }

    public String getDiscordWebhookUrl() { return discordWebhookUrl; }
    public void setDiscordWebhookUrl(String discordWebhookUrl) { this.discordWebhookUrl = discordWebhookUrl; }

    public boolean isDiscordEnabled() { return discordEnabled; }
    public void setDiscordEnabled(boolean discordEnabled) { this.discordEnabled = discordEnabled; }

    public String getSmtpHost() { return smtpHost; }
    public void setSmtpHost(String smtpHost) { this.smtpHost = smtpHost; }

    public Integer getSmtpPort() { return smtpPort; }
    public void setSmtpPort(Integer smtpPort) { this.smtpPort = smtpPort; }

    public String getSmtpUsername() { return smtpUsername; }
    public void setSmtpUsername(String smtpUsername) { this.smtpUsername = smtpUsername; }

    public String getSmtpPasswordEncrypted() { return smtpPasswordEncrypted; }
    public void setSmtpPasswordEncrypted(String smtpPasswordEncrypted) { this.smtpPasswordEncrypted = smtpPasswordEncrypted; }

    public boolean isMailEnabled() { return mailEnabled; }
    public void setMailEnabled(boolean mailEnabled) { this.mailEnabled = mailEnabled; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
