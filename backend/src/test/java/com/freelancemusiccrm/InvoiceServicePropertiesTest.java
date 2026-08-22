package com.freelancemusiccrm;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Tag;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.stubbing.Answer;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.freelancemusiccrm.dto.invoice.InvoiceCreateDto;
import com.freelancemusiccrm.dto.invoice.InvoiceResponseDto;
import com.freelancemusiccrm.entity.Invoice;
import com.freelancemusiccrm.entity.Order;
import com.freelancemusiccrm.entity.OrderCategory;
import com.freelancemusiccrm.entity.OrderStatus;
import com.freelancemusiccrm.entity.QuoteRequest;
import com.freelancemusiccrm.entity.QuoteRequestStatus;
import com.freelancemusiccrm.entity.QuoteResponse;
import com.freelancemusiccrm.entity.Task;
import com.freelancemusiccrm.entity.TaskStatus;
import com.freelancemusiccrm.entity.TokenStatus;
import com.freelancemusiccrm.entity.Worker;
import com.freelancemusiccrm.exception.ConflictException;
import com.freelancemusiccrm.repository.InvoiceRepository;
import com.freelancemusiccrm.repository.TaskRepository;
import com.freelancemusiccrm.repository.WorkerRepository;
import com.freelancemusiccrm.service.InvoiceService;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.DoubleRange;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;
import net.jqwik.api.constraints.StringLength;

class InvoiceServicePropertiesTest {

    @Property(tries = 100)
    @Tag("Feature: freelance-music-crm, Property 19: Invoice データの完全性（ラウンドトリップ）")
    void invoiceRoundTrip(
            @ForAll @LongRange(min = 1, max = 100000) long taskId,
            @ForAll @StringLength(min = 1, max = 100) String subject,
            @ForAll @StringLength(min = 1, max = 100) String clientName,
            @ForAll @IntRange(min = 1, max = 365) int deliveryDays,
            @ForAll @DoubleRange(min = 0.0, max = 1000000.0) double amountRaw
    ) {
        InvoiceRepository invoiceRepository = mock(InvoiceRepository.class);
        TaskRepository taskRepository = mock(TaskRepository.class);
        WorkerRepository workerRepository = mock(WorkerRepository.class);
        InvoiceService service = new InvoiceService(invoiceRepository, taskRepository, workerRepository);

        Task task = buildCompletedTask(taskId, subject, clientName, deliveryDays, amountRaw);
        Worker worker = buildWorker();

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(worker.getEmail(), null));
        SecurityContextHolder.setContext(context);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(invoiceRepository.findByTaskId(taskId)).thenReturn(Optional.empty());
        when(workerRepository.findByEmail(worker.getEmail())).thenReturn(Optional.of(worker));

        AtomicLong idGenerator = new AtomicLong(1L);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer((Answer<Invoice>) invocation -> {
            Invoice saved = invocation.getArgument(0);
            saved.setId(idGenerator.getAndIncrement());
            saved.setCreatedAt(LocalDateTime.now());
            return saved;
        });
        when(invoiceRepository.findById(anyLong())).thenAnswer((Answer<Optional<Invoice>>) invocation -> {
            Invoice invoice = new Invoice();
            invoice.setId(invocation.getArgument(0));
            invoice.setTask(task);
            invoice.setSubject(subject);
            invoice.setClientName(clientName);
            invoice.setClientEmail(task.getOrder().getClientEmail());
            invoice.setCategoryName(task.getOrder().getCategory().getName());
            invoice.setDeliveryDate(task.getOrder().getDesiredDeliveryDate());
            invoice.setAmount(task.getOrder().getQuoteResponse().getAmount());
            invoice.setIssueDate(LocalDate.now());
            invoice.setWorkerName(worker.getName());
            invoice.setWorkerContact(worker.getContact());
            invoice.setCreatedAt(LocalDateTime.now());
            return Optional.of(invoice);
        });

        InvoiceResponseDto created;
        InvoiceResponseDto found;
        try {
            created = service.create(new InvoiceCreateDto(taskId));
            found = service.findById(created.id());
        } finally {
            SecurityContextHolder.clearContext();
        }

        assertThat(found.taskId()).isEqualTo(taskId);
        assertThat(found.subject()).isEqualTo(subject);
        assertThat(found.clientName()).isEqualTo(clientName);
        assertThat(found.categoryName()).isEqualTo(task.getOrder().getCategory().getName());
        assertThat(found.deliveryDate()).isEqualTo(task.getOrder().getDesiredDeliveryDate());
        assertThat(found.amount()).isEqualTo(task.getOrder().getQuoteResponse().getAmount());
        assertThat(found.workerName()).isEqualTo(worker.getName());
        assertThat(found.workerContact()).isEqualTo(worker.getContact());
    }

    @Property(tries = 100)
    @Tag("Feature: freelance-music-crm, Property 21: Invoice の重複発行防止")
    void duplicateInvoiceRejected(@ForAll @LongRange(min = 1, max = 100000) long taskId) {
        InvoiceRepository invoiceRepository = mock(InvoiceRepository.class);
        TaskRepository taskRepository = mock(TaskRepository.class);
        WorkerRepository workerRepository = mock(WorkerRepository.class);
        InvoiceService service = new InvoiceService(invoiceRepository, taskRepository, workerRepository);

        Task task = buildCompletedTask(taskId, "件名", "依頼者", 30, 1000.0);
        Invoice existing = new Invoice();
        existing.setId(999L);
        Worker worker = buildWorker();

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(worker.getEmail(), null));
        SecurityContextHolder.setContext(context);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(invoiceRepository.findByTaskId(taskId)).thenReturn(Optional.of(existing));
        when(workerRepository.findByEmail(worker.getEmail())).thenReturn(Optional.of(worker));

        ConflictException ex;
        try {
            ex = assertThrows(ConflictException.class, () -> service.create(new InvoiceCreateDto(taskId)));
        } finally {
            SecurityContextHolder.clearContext();
        }
        assertThat(ex.getMessage()).contains("既に発行済み");
    }

    private Task buildCompletedTask(long taskId, String subject, String clientName, int deliveryDays, double amountRaw) {
        OrderCategory category = new OrderCategory();
        category.setId(1L);
        category.setName("作曲");

        QuoteRequest quoteRequest = new QuoteRequest();
        quoteRequest.setId(taskId + 1000);
        quoteRequest.setSubject(subject);
        quoteRequest.setClientName(clientName);
        quoteRequest.setClientEmail("client@example.com");
        quoteRequest.setCategory(category);
        quoteRequest.setDesiredDeliveryDate(LocalDate.now().plusDays(deliveryDays));
        quoteRequest.setStatus(QuoteRequestStatus.APPROVED);

        QuoteResponse quoteResponse = new QuoteResponse();
        quoteResponse.setId(taskId + 2000);
        quoteResponse.setQuoteRequest(quoteRequest);
        quoteResponse.setAmount(BigDecimal.valueOf(amountRaw));
        quoteResponse.setResponseDeliveryDate(LocalDate.now().plusDays(deliveryDays));
        quoteResponse.setApprovalToken("token-" + taskId);
        quoteResponse.setTokenStatus(TokenStatus.USED);
        quoteResponse.setCreatedAt(LocalDateTime.now());

        Order order = new Order();
        order.setId(taskId + 3000);
        order.setQuoteResponse(quoteResponse);
        order.setSubject(subject);
        order.setClientName(clientName);
        order.setClientEmail("client@example.com");
        order.setCategory(category);
        order.setDesiredDeliveryDate(LocalDate.now().plusDays(deliveryDays));
        order.setStatus(OrderStatus.RECEIVED);
        order.setCreatedAt(LocalDateTime.now());

        Task task = new Task();
        task.setId(taskId);
        task.setOrder(order);
        task.setStatus(TaskStatus.COMPLETED);
        task.setStatusUpdatedAt(LocalDateTime.now());
        task.setCreatedAt(LocalDateTime.now());
        return task;
    }

    private Worker buildWorker() {
        Worker worker = new Worker();
        worker.setId(1L);
        worker.setEmail("worker@example.com");
        worker.setName("作業者");
        worker.setContact("contact@example.com");
        return worker;
    }
}
