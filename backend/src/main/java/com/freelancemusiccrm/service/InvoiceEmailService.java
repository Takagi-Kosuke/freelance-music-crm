package com.freelancemusiccrm.service;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.freelancemusiccrm.dto.invoice.InvoiceResponseDto;
import com.freelancemusiccrm.entity.Invoice;
import com.freelancemusiccrm.entity.Worker;
import com.freelancemusiccrm.entity.WorkerSettings;
import com.freelancemusiccrm.exception.AuthenticationFailedException;
import com.freelancemusiccrm.exception.ResourceNotFoundException;
import com.freelancemusiccrm.exception.UnprocessableEntityException;
import com.freelancemusiccrm.repository.InvoiceRepository;
import com.freelancemusiccrm.repository.WorkerRepository;
import com.freelancemusiccrm.repository.WorkerSettingsRepository;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class InvoiceEmailService {

    private final InvoiceRepository invoiceRepository;
    private final WorkerRepository workerRepository;
    private final WorkerSettingsRepository workerSettingsRepository;
    private final SecretEncryptionService secretEncryptionService;
    private final PdfGeneratorService pdfGeneratorService;

    public InvoiceEmailService(
            InvoiceRepository invoiceRepository,
            WorkerRepository workerRepository,
            WorkerSettingsRepository workerSettingsRepository,
            SecretEncryptionService secretEncryptionService,
            PdfGeneratorService pdfGeneratorService
    ) {
        this.invoiceRepository = invoiceRepository;
        this.workerRepository = workerRepository;
        this.workerSettingsRepository = workerSettingsRepository;
        this.secretEncryptionService = secretEncryptionService;
        this.pdfGeneratorService = pdfGeneratorService;
    }

    @Transactional(readOnly = true)
    public void sendInvoiceEmail(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("請求書が見つかりません"));

        Worker worker = getCurrentWorker();
        WorkerSettings settings = workerSettingsRepository.findByWorkerId(worker.getId())
                .orElseThrow(() -> new UnprocessableEntityException("メール送信設定がありません"));

        if (!settings.isMailEnabled()) {
            throw new UnprocessableEntityException("メール送信機能が無効です");
        }

        validateSmtpSettings(settings);

        byte[] pdf = pdfGeneratorService.generateInvoicePdf(toResponseDto(invoice));
        JavaMailSenderImpl mailSender = createMailSender(settings);
        MimeMessage message = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setTo(invoice.getClientEmail());
            helper.setFrom(settings.getSmtpUsername());
            helper.setSubject("請求書送付: " + invoice.getSubject());
            helper.setText(buildMailBody(invoice), false);
            helper.addAttachment("invoice-" + invoice.getId() + ".pdf", new org.springframework.core.io.ByteArrayResource(pdf));
            mailSender.send(message);
        } catch (MessagingException ex) {
            throw new IllegalStateException("請求書メールの送信に失敗しました", ex);
        }
    }

    private JavaMailSenderImpl createMailSender(WorkerSettings settings) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(settings.getSmtpHost());
        mailSender.setPort(settings.getSmtpPort());
        mailSender.setUsername(settings.getSmtpUsername());
        mailSender.setPassword(decryptPassword(settings));

        Properties properties = mailSender.getJavaMailProperties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.starttls.required", "true");
        properties.put("mail.smtp.ssl.trust", settings.getSmtpHost());
        return mailSender;
    }

    private String decryptPassword(WorkerSettings settings) {
        if (settings.getSmtpPasswordEncrypted() == null || settings.getSmtpPasswordEncrypted().isBlank()) {
            return null;
        }
        return secretEncryptionService.decrypt(settings.getSmtpPasswordEncrypted());
    }

    private void validateSmtpSettings(WorkerSettings settings) {
        if (settings.getSmtpHost() == null || settings.getSmtpHost().isBlank()) {
            throw new UnprocessableEntityException("SMTPホストが設定されていません");
        }
        if (settings.getSmtpPort() == null) {
            throw new UnprocessableEntityException("SMTPポートが設定されていません");
        }
        if (settings.getSmtpUsername() == null || settings.getSmtpUsername().isBlank()) {
            throw new UnprocessableEntityException("SMTPユーザー名が設定されていません");
        }
    }

    private Worker getCurrentWorker() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof String principalEmail)) {
            throw new AuthenticationFailedException("認証が必要です");
        }

        return workerRepository.findByEmail(principalEmail)
                .orElseThrow(() -> new ResourceNotFoundException("作業者情報が見つかりません"));
    }

    private InvoiceResponseDto toResponseDto(Invoice invoice) {
        return new InvoiceResponseDto(
                invoice.getId(),
                invoice.getTask().getId(),
                invoice.getSubject(),
                invoice.getClientName(),
                invoice.getClientEmail(),
                invoice.getCategoryName(),
                invoice.getDeliveryDate(),
                invoice.getAmount(),
                invoice.getIssueDate(),
                invoice.getWorkerName(),
                invoice.getWorkerContact(),
                invoice.getCreatedAt()
        );
    }

    private String buildMailBody(Invoice invoice) {
        return "請求書を送付します。\n\n" +
                "件名: " + invoice.getSubject() + "\n" +
                "請求先: " + invoice.getClientName() + " 様\n" +
                "請求金額: " + invoice.getAmount().toPlainString() + "\n" +
                "PDF を添付しています。";
    }
}