package com.freelancemusiccrm.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.freelancemusiccrm.dto.task.TaskFolderPathUpdateDto;
import com.freelancemusiccrm.dto.task.TaskResponseDto;
import com.freelancemusiccrm.dto.task.TaskStatusUpdateDto;
import com.freelancemusiccrm.entity.Task;
import com.freelancemusiccrm.entity.TaskStatus;
import com.freelancemusiccrm.exception.ResourceNotFoundException;
import com.freelancemusiccrm.repository.TaskRepository;

@Service
public class TaskService {

    private static final Logger logger = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository taskRepository;
    private final DiscordNotifierService discordNotifierService;

    public TaskService(TaskRepository taskRepository, DiscordNotifierService discordNotifierService) {
        this.taskRepository = taskRepository;
        this.discordNotifierService = discordNotifierService;
    }

    @Transactional(readOnly = true)
    public List<TaskResponseDto> findAll(Long categoryId) {
        List<Task> tasks = categoryId == null
                ? taskRepository.findAll()
                : taskRepository.findByOrderCategoryId(categoryId);

        return tasks.stream().map(this::toDto).toList();
    }

    @Transactional
    public TaskResponseDto updateStatus(Long taskId, TaskStatusUpdateDto request) {
        Long id = Objects.requireNonNull(taskId);
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("タスクが見つかりません"));

        TaskStatus previousStatus = task.getStatus();

        task.setStatus(request.status());
        task.setStatusUpdatedAt(LocalDateTime.now());

        Task saved = taskRepository.save(task);
        if (previousStatus != TaskStatus.COMPLETED && request.status() == TaskStatus.COMPLETED) {
            try {
                discordNotifierService.notifyTaskCompleted(saved.getOrder().getSubject());
            } catch (RuntimeException ex) {
                logger.warn("Discord通知呼び出しで例外が発生しましたが、タスク更新は継続します: {}", ex.getMessage(), ex);
            }
        }
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<TaskResponseDto> findCalendarTasks(LocalDate start, LocalDate end) {
        List<Task> tasks = taskRepository.findByOrderDesiredDeliveryDateBetween(start, end);
        return tasks.stream().map(this::toDto).toList();
    }

    @Transactional
    public TaskResponseDto updateFolderPath(Long taskId, TaskFolderPathUpdateDto request) {
        Long id = Objects.requireNonNull(taskId);
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("タスクが見つかりません"));

        String normalizedFolderPath = request.folderPath();
        if (normalizedFolderPath != null) {
            normalizedFolderPath = normalizedFolderPath.trim();
            if (normalizedFolderPath.isEmpty()) {
                normalizedFolderPath = null;
            }
        }

        task.setFolderPath(normalizedFolderPath);
        Task saved = taskRepository.save(task);
        return toDto(saved);
    }

    private TaskResponseDto toDto(Task task) {
        return new TaskResponseDto(
                task.getId(),
                task.getOrder().getId(),
                task.getOrder().getCategory().getId(),
                task.getOrder().getCategory().getName(),
                task.getOrder().getSubject(),
                task.getOrder().getClientName(),
                task.getOrder().getClientEmail(),
                task.getOrder().getDesiredDeliveryDate(),
                task.getOrder().getFilePathUrl(),
                task.getOrder().getComment(),
                task.getFolderPath(),
                task.getStatus(),
                task.getStatusUpdatedAt(),
                task.getCreatedAt()
        );
    }
}
