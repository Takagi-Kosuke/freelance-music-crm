package com.freelancemusiccrm;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freelancemusiccrm.entity.WorkerSettings;
import com.freelancemusiccrm.entity.OrderCategory;
import com.freelancemusiccrm.entity.Task;
import com.freelancemusiccrm.entity.Worker;
import com.freelancemusiccrm.repository.WorkerSettingsRepository;
import com.freelancemusiccrm.service.SecretEncryptionService;
import com.freelancemusiccrm.repository.OrderCategoryRepository;
import com.freelancemusiccrm.repository.TaskRepository;
import com.freelancemusiccrm.repository.WorkerRepository;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;

import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeMessage;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@Transactional
@SuppressWarnings("unused")
class WorkflowIntegrationTests {

    private static final GreenMail GREEN_MAIL = new GreenMail(ServerSetupTest.SMTP);

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @SuppressWarnings("unused")
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> true);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private OrderCategoryRepository orderCategoryRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private WorkerSettingsRepository workerSettingsRepository;

    @Autowired
    private SecretEncryptionService secretEncryptionService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeAll
    static void startMailServer() {
        GREEN_MAIL.start();
    }

    @AfterAll
    static void stopMailServer() {
        GREEN_MAIL.stop();
    }

    @Test
    @Tag("Feature: freelance-music-crm, Integration 18.1: 見積依頼フロー E2E")
    void quoteRequestToTaskWorkflow() throws Exception {
        Worker worker = seedWorker();
        OrderCategory category = orderCategoryRepository.findAllByOrderByIdAsc().getFirst();

        long quoteRequestId = createQuoteRequest(category.getId());
        MockHttpSession session = login(worker.getEmail(), "password123");
        String approvalToken = createQuoteResponse(session, quoteRequestId);

        MvcResult approveResult = mockMvc.perform(post("/api/orders/token/{token}/approve", approvalToken).with(csrf()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode approveBody = objectMapper.readTree(approveResult.getResponse().getContentAsString());
        long taskId = approveBody.get("taskId").asLong();

        Task task = taskRepository.findById(taskId).orElseThrow();
        assertThat(task.getOrder().getSubject()).isEqualTo("統合テスト案件");
        assertThat(task.getOrder().getClientName()).isEqualTo("統合テスト依頼者");
        assertThat(task.getOrder().getCategory().getId()).isEqualTo(category.getId());
    }

    @Test
    @Tag("Feature: freelance-music-crm, Integration 18.2: Invoice 発行フロー E2E")
    void taskCompletionToInvoicePdfWorkflow() throws Exception {
        Worker worker = seedWorker();
        OrderCategory category = orderCategoryRepository.findAllByOrderByIdAsc().getFirst();

        long quoteRequestId = createQuoteRequest(category.getId());
        MockHttpSession session = login(worker.getEmail(), "password123");
        String approvalToken = createQuoteResponse(session, quoteRequestId);

        MvcResult approveResult = mockMvc.perform(post("/api/orders/token/{token}/approve", approvalToken).with(csrf()))
                .andExpect(status().isOk())
                .andReturn();
        long taskId = objectMapper.readTree(approveResult.getResponse().getContentAsString()).get("taskId").asLong();

        mockMvc.perform(patch("/api/tasks/{id}/status", taskId)
                        .session(session)
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"status":"COMPLETED"}
                                """))
                .andExpect(status().isOk());

        MvcResult invoiceResult = mockMvc.perform(post("/api/invoices")
                        .session(session)
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"taskId":%d}
                                """.formatted(taskId)))
                .andExpect(status().isCreated())
                .andReturn();

        long invoiceId = objectMapper.readTree(invoiceResult.getResponse().getContentAsString()).get("id").asLong();

        byte[] pdf = mockMvc.perform(get("/api/invoices/{id}/pdf", invoiceId).session(session))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        assertThat(new String(pdf, 0, Math.min(pdf.length, 5), StandardCharsets.ISO_8859_1)).startsWith("%PDF-");
    }

    @Test
    @Tag("Feature: freelance-music-crm, Integration 18.3: メール送信フロー E2E")
    void invoiceEmailWorkflow() throws Exception {
        Worker worker = seedWorker();
        OrderCategory category = orderCategoryRepository.findAllByOrderByIdAsc().getFirst();

        long quoteRequestId = createQuoteRequest(category.getId());
        MockHttpSession session = login(worker.getEmail(), "password123");
        String approvalToken = createQuoteResponse(session, quoteRequestId);

        MvcResult approveResult = mockMvc.perform(post("/api/orders/token/{token}/approve", approvalToken).with(csrf()))
            .andExpect(status().isOk())
            .andReturn();
        long taskId = objectMapper.readTree(approveResult.getResponse().getContentAsString()).get("taskId").asLong();

        mockMvc.perform(patch("/api/tasks/{id}/status", taskId)
                .session(session)
                .with(csrf())
                .contentType("application/json")
                .content("""
                {"status":"COMPLETED"}
                """))
            .andExpect(status().isOk());

        MvcResult invoiceResult = mockMvc.perform(post("/api/invoices")
                .session(session)
                .with(csrf())
                .contentType("application/json")
                .content("""
                {"taskId":%d}
                """.formatted(taskId)))
            .andExpect(status().isCreated())
            .andReturn();

        long invoiceId = objectMapper.readTree(invoiceResult.getResponse().getContentAsString()).get("id").asLong();

        configureMailSettings(worker);

        mockMvc.perform(post("/api/invoices/{id}/send-email", invoiceId)
                .session(session)
                .with(csrf()))
            .andExpect(status().isNoContent());

        MimeMessage[] received = GREEN_MAIL.getReceivedMessages();
        assertThat(received).hasSize(1);

        MimeMessage message = received[0];
        assertThat(message.getSubject()).contains("請求書送付");
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo("client@example.com");

        Object content = message.getContent();
        assertThat(content).isInstanceOf(Multipart.class);

        Multipart multipart = (Multipart) content;
        boolean hasPdfAttachment = false;
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart part = multipart.getBodyPart(i);
            if (part.getFileName() != null && part.getFileName().endsWith(".pdf")) {
                hasPdfAttachment = true;
                byte[] attachmentBytes = part.getInputStream().readAllBytes();
                assertThat(new String(attachmentBytes, 0, Math.min(attachmentBytes.length, 5), StandardCharsets.ISO_8859_1))
                        .startsWith("%PDF-");
            }
        }

        assertThat(hasPdfAttachment).isTrue();
    }

    private Worker seedWorker() {
        Worker existing = workerRepository.findByEmail("worker@example.com").orElse(null);
        if (existing != null) {
            return existing;
        }

        Worker worker = new Worker();
        worker.setEmail("worker@example.com");
        worker.setPasswordHash(passwordEncoder.encode("password123"));
        worker.setName("統合作業者");
        worker.setContact("worker-contact@example.com");
        return workerRepository.save(worker);
    }

    private void configureMailSettings(Worker worker) {
        WorkerSettings settings = workerSettingsRepository.findByWorkerId(worker.getId()).orElseGet(() -> {
            WorkerSettings created = new WorkerSettings();
            created.setWorker(worker);
            return created;
        });

        settings.setMailEnabled(true);
        settings.setSmtpHost("localhost");
        settings.setSmtpPort(GREEN_MAIL.getSmtp().getPort());
        settings.setSmtpUsername("sender@example.com");
        settings.setSmtpPasswordEncrypted(secretEncryptionService.encrypt("password123"));
        workerSettingsRepository.save(settings);

        GREEN_MAIL.setUser("sender@example.com", "password123");
    }

    private long createQuoteRequest(Long categoryId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/quote-requests")
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "subject":"統合テスト案件",
                                  "clientName":"統合テスト依頼者",
                                  "clientEmail":"client@example.com",
                                  "categoryId":%d,
                                  "desiredDeliveryDate":"%s",
                                  "filePathUrl":"https://example.com/file",
                                  "comment":"統合テストコメント"
                                }
                                """.formatted(categoryId, LocalDate.now().plusDays(10))))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private MockHttpSession login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();

        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private String createQuoteResponse(MockHttpSession session, long quoteRequestId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/quote-responses")
                        .session(session)
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "quoteRequestId":%d,
                                  "amount":50000,
                                  "responseDeliveryDate":"%s",
                                  "responseComment":"統合テスト回答"
                                }
                                """.formatted(quoteRequestId, LocalDate.now().plusDays(14))))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("approvalToken").asText();
    }
}
