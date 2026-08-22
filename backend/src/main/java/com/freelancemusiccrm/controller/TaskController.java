package com.freelancemusiccrm.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.freelancemusiccrm.dto.task.TaskResponseDto;
import com.freelancemusiccrm.dto.task.TaskFolderPathUpdateDto;
import com.freelancemusiccrm.dto.task.TaskStatusUpdateDto;
import com.freelancemusiccrm.service.TaskService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public ResponseEntity<List<TaskResponseDto>> findAll(
            @RequestParam(required = false) Long categoryId
    ) {
        return ResponseEntity.ok(taskService.findAll(categoryId));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TaskResponseDto> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody TaskStatusUpdateDto request
    ) {
        return ResponseEntity.ok(taskService.updateStatus(id, request));
    }

    @PatchMapping("/{id}/folder-path")
    public ResponseEntity<TaskResponseDto> updateFolderPath(
            @PathVariable Long id,
            @Valid @RequestBody TaskFolderPathUpdateDto request
    ) {
        return ResponseEntity.ok(taskService.updateFolderPath(id, request));
    }

    @GetMapping("/calendar")
    public ResponseEntity<List<TaskResponseDto>> findCalendarTasks(
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        return ResponseEntity.ok(taskService.findCalendarTasks(start, end));
    }
}
