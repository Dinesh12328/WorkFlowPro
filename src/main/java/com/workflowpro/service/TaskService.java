package com.workflowpro.service;

import com.workflowpro.dto.TaskDtos.TaskCreateRequest;
import com.workflowpro.dto.TaskDtos.TaskResponse;
import com.workflowpro.dto.TaskDtos.TaskUpdateRequest;
import com.workflowpro.entity.*;
import com.workflowpro.exception.ApiException;
import com.workflowpro.repository.TaskRepository;
import com.workflowpro.repository.UserRepository;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final ProjectAccessService projectAccessService;
    private final NotificationService notificationService;

    @Transactional
    public TaskResponse create(TaskCreateRequest request) {
        User actor = currentUserService.get();
        Project project = projectAccessService.requireView(request.projectId(), actor);
        User assignee = resolveAssignee(request.assigneeId(), project);

        Task task = new Task();
        task.setTitle(request.title().trim());
        task.setDescription(trimToNull(request.description()));
        task.setPriority(request.priority() == null ? TaskPriority.MEDIUM : request.priority());
        task.setStatus(request.status() == null ? TaskStatus.TODO : request.status());
        task.setDueDate(request.dueDate());
        task.setProject(project);
        task.setAssignee(assignee);
        task.setCreatedBy(actor);
        setCompletionTime(task, task.getStatus());
        task = taskRepository.save(task);

        if (assignee != null && !assignee.getId().equals(actor.getId())) {
            notificationService.taskAssigned(assignee, task.getId(), task.getTitle(), project.getName());
        }
        return TaskResponse.from(task);
    }

    @Transactional
    public TaskResponse update(Long id, TaskUpdateRequest request) {
        User actor = currentUserService.get();
        Task task = requireAccessible(id, actor);
        Long previousAssigneeId = task.getAssignee() == null ? null : task.getAssignee().getId();

        if (request.title() != null) task.setTitle(request.title().trim());
        if (request.description() != null) task.setDescription(trimToNull(request.description()));
        if (request.priority() != null) task.setPriority(request.priority());
        if (request.status() != null) {
            task.setStatus(request.status());
            setCompletionTime(task, request.status());
        }
        if (Boolean.TRUE.equals(request.clearDueDate())) task.setDueDate(null);
        else if (request.dueDate() != null) task.setDueDate(request.dueDate());
        if (Boolean.TRUE.equals(request.clearAssignee())) task.setAssignee(null);
        else if (request.assigneeId() != null) task.setAssignee(resolveAssignee(request.assigneeId(), task.getProject()));

        User newAssignee = task.getAssignee();
        if (newAssignee != null
                && !newAssignee.getId().equals(previousAssigneeId)
                && !newAssignee.getId().equals(actor.getId())) {
            notificationService.taskAssigned(
                    newAssignee, task.getId(), task.getTitle(), task.getProject().getName()
            );
        }
        return TaskResponse.from(task);
    }

    @Transactional(readOnly = true)
    public TaskResponse get(Long id) {
        return TaskResponse.from(requireAccessible(id, currentUserService.get()));
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> filter(
            TaskStatus status,
            TaskPriority priority,
            LocalDate dueFrom,
            LocalDate dueTo,
            Long projectId,
            Long assigneeId,
            Pageable pageable
    ) {
        User user = currentUserService.get();
        Specification<Task> spec = accessibleTo(user)
                .and(equal("status", status))
                .and(equal("priority", priority))
                .and(equal("project", projectId))
                .and(equal("assignee", assigneeId))
                .and(dateAtLeast(dueFrom))
                .and(dateAtMost(dueTo));
        return taskRepository.findAll(spec, pageable).map(TaskResponse::from);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> byStatus(TaskStatus status) {
        User user = currentUserService.get();
        return taskRepository.findAccessibleByStatus(user.getId(), status).stream().map(TaskResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> byPriority(TaskPriority priority) {
        User user = currentUserService.get();
        return taskRepository.findAccessibleByPriority(user.getId(), priority).stream().map(TaskResponse::from).toList();
    }

    @Transactional
    public void delete(Long id) {
        User user = currentUserService.get();
        Task task = requireAccessible(id, user);
        if (user.getRole() != Role.ADMIN
                && !task.getProject().getOwner().getId().equals(user.getId())
                && !task.getCreatedBy().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the task creator or project owner can delete this task");
        }
        taskRepository.delete(task);
    }

    public Task requireAccessible(Long id, User user) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Task not found"));
        if (!projectAccessService.canView(task.getProject(), user)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You do not have access to this task");
        }
        return task;
    }

    private User resolveAssignee(Long assigneeId, Project project) {
        if (assigneeId == null) return null;
        User assignee = userRepository.findById(assigneeId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Assignee not found"));
        boolean belongsToProject = project.getOwner().getId().equals(assigneeId)
                || project.getMembers().stream().anyMatch(member -> member.getId().equals(assigneeId));
        if (!belongsToProject) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Assignee must be a project owner or member");
        }
        return assignee;
    }

    private void setCompletionTime(Task task, TaskStatus status) {
        task.setCompletedAt(status == TaskStatus.COMPLETED ? Instant.now() : null);
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Specification<Task> accessibleTo(User user) {
        return (root, query, cb) -> {
            if (user.getRole() == Role.ADMIN) return cb.conjunction();
            query.distinct(true);
            var project = root.join("project");
            var members = project.join("members", JoinType.LEFT);
            return cb.or(
                    cb.equal(project.get("owner").get("id"), user.getId()),
                    cb.equal(members.get("id"), user.getId())
            );
        };
    }

    private <T> Specification<Task> equal(String associationOrField, T value) {
        if (value == null) return null;
        return (root, query, cb) -> {
            if (value instanceof Long) return cb.equal(root.get(associationOrField).get("id"), value);
            return cb.equal(root.get(associationOrField), value);
        };
    }

    private Specification<Task> dateAtLeast(LocalDate date) {
        return date == null ? null : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("dueDate"), date);
    }

    private Specification<Task> dateAtMost(LocalDate date) {
        return date == null ? null : (root, query, cb) -> cb.lessThanOrEqualTo(root.get("dueDate"), date);
    }
}
