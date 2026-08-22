package com.freelancemusiccrm.dto.task;

import jakarta.validation.constraints.Size;

public record TaskFolderPathUpdateDto(
        @Size(max = 1000, message = "フォルダパスは1000文字以内で入力してください")
        String folderPath
) {
}
