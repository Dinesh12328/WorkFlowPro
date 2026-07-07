package com.workflowpro.dto;

import com.workflowpro.entity.Task;
import com.workflowpro.entity.TaskPriority;
import com.workflowpro.entity.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;

public final class TaskDtos {
    private TaskDtos() {}

    public record TaskCreateRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 5000) String description,
            TaskPriority priority,
            TaskStatus status,
            LocalDate dueDate,
            @NotNull Long projectId,
            Long assigneeId
    ) {}

    public record TaskUpdateRequest(
            @Size(min = 1, max = 200) String title,
            @Size(max = 5000) String description,
            TaskPriority priority,
            TaskStatus status,
            LocalDate dueDate,
            Long assigneeId,
            Boolean clearAssignee,
            Boolean clearDueDate
    ) {}

    public record TaskResponse(
            Long id,
            String title,
            String description,
            TaskPriority priority,
            TaskStatus status,
            LocalDate dueDate,
            Instant completedAt,
            Long projectId,
            String projectName,
            UserResponse assignee,
            UserResponse createdBy,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static TaskResponse from(Task task) {
            return new TaskResponse(
                    task.getId(),
                    task.getTitle(),
                    task.getDescription(),
                    task.getPriority(),
                    task.getStatus(),
                    task.getDueDate(),
                    task.getCompletedAt(),
                    task.getProject().getId(),
                    task.getProject().getName(),
                    task.getAssignee() == null ? null : UserResponse.from(task.getAssignee()),
                    UserResponse.from(task.getCreatedBy()),
                    task.getCreatedAt(),
                    task.getUpdatedAt()
            );
        }
    }
}
