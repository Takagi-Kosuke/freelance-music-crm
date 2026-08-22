package com.freelancemusiccrm.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.freelancemusiccrm.dto.invoice.InvoiceCreateDto;
import com.freelancemusiccrm.dto.invoice.InvoiceResponseDto;
import com.freelancemusiccrm.entity.Invoice;
import com.freelancemusiccrm.entity.Task;
import com.freelancemusiccrm.entity.TaskStatus;
import com.freelancemusiccrm.entity.Worker;
import com.freelancemusiccrm.exception.AuthenticationFailedException;
import com.freelancemusiccrm.exception.ConflictException;
import com.freelancemusiccrm.exception.ResourceNotFoundException;
import com.freelancemusiccrm.exception.UnprocessableEntityException;
import com.freelancemusiccrm.repository.InvoiceRepository;
import com.freelancemusiccrm.repository.TaskRepository;
import com.freelancemusiccrm.repository.WorkerRepository;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final TaskRepository taskRepository;
    private final WorkerRepository workerRepository;

    public InvoiceService(
            InvoiceRepository invoiceRepository,
            TaskRepository taskRepository,
            WorkerRepository workerRepository
    ) {
        this.invoiceRepository = invoiceRepository;
        this.taskRepository = taskRepository;
        this.workerRepository = workerRepository;
    }

    @Transactional
    public InvoiceResponseDto create(InvoiceCreateDto request) {
        Task task = taskRepository.findById(request.taskId())
                .orElseThrow(() -> new ResourceNotFoundException("タスクが見つかりません"));

        if (task.getStatus() != TaskStatus.COMPLETED) {
            throw new UnprocessableEntityException("完了済みタスクのみ請求書を発行できます");
        }

        invoiceRepository.findByTaskId(task.getId()).ifPresent(existing -> {
            throw new ConflictException("このタスクの請求書は既に発行済みです");
        });

        Worker worker = getCurrentWorker();

        Invoice invoice = new Invoice();
        invoice.setTask(task);
        invoice.setSubject(task.getOrder().getSubject());
        invoice.setClientName(task.getOrder().getClientName());
        invoice.setClientEmail(task.getOrder().getClientEmail());
        invoice.setCategoryName(task.getOrder().getCategory().getName());
        invoice.setDeliveryDate(task.getOrder().getDesiredDeliveryDate());
        invoice.setAmount(task.getOrder().getQuoteResponse().getAmount());
        invoice.setIssueDate(LocalDate.now());
        invoice.setWorkerName(worker.getName());
        invoice.setWorkerContact(worker.getContact());

        Invoice saved = invoiceRepository.save(invoice);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public InvoiceResponseDto findById(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("請求書が見つかりません"));
        return toDto(invoice);
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponseDto> findAll() {
        return invoiceRepository.findAll().stream()
                .sorted(Comparator.comparing(Invoice::getCreatedAt).reversed())
                .map(this::toDto)
                .toList();
    }

    private InvoiceResponseDto toDto(Invoice invoice) {
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
}
