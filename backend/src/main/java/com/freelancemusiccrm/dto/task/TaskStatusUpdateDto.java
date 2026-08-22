package com.freelancemusiccrm.dto.task;

import com.freelancemusiccrm.entity.TaskStatus;

import jakarta.validation.constraints.NotNull;

public record TaskStatusUpdateDto(
        @NotNull(message = "ステータスは必須です")
        TaskStatus status
) {
}
