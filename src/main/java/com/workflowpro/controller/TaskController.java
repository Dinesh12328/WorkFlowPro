package com.workflowpro.controller;

import com.workflowpro.dto.TaskDtos.TaskCreateRequest;
import com.workflowpro.dto.TaskDtos.TaskResponse;
import com.workflowpro.dto.TaskDtos.TaskUpdateRequest;
import com.workflowpro.entity.TaskPriority;
import com.workflowpro.entity.TaskStatus;
import com.workflowpro.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {
    private final TaskService taskService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(@Valid @RequestBody TaskCreateRequest request) {
        return taskService.create(request);
    }

    @PutMapping("/{id}")
    public TaskResponse update(@PathVariable Long id, @Valid @RequestBody TaskUpdateRequest request) {
        return taskService.update(id, request);
    }

    @GetMapping("/{id}")
    public TaskResponse get(@PathVariable Long id) {
        return taskService.get(id);
    }

    @GetMapping
    public Page<TaskResponse> filter(
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueTo,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long assigneeId,
            Pageable pageable
    ) {
        return taskService.filter(status, priority, dueFrom, dueTo, projectId, assigneeId, pageable);
    }

    @GetMapping("/status/{status}")
    public List<TaskResponse> byStatus(@PathVariable TaskStatus status) {
        return taskService.byStatus(status);
    }

    @GetMapping("/priority/{priority}")
    public List<TaskResponse> byPriority(@PathVariable TaskPriority priority) {
        return taskService.byPriority(priority);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        taskService.delete(id);
    }
}
