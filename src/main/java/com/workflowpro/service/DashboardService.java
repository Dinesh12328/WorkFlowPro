package com.workflowpro.service;

import com.workflowpro.dto.DashboardStatsResponse;
import com.workflowpro.entity.Task;
import com.workflowpro.entity.TaskPriority;
import com.workflowpro.entity.TaskStatus;
import com.workflowpro.entity.User;
import com.workflowpro.repository.NotificationRepository;
import com.workflowpro.repository.ProjectRepository;
import com.workflowpro.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final CurrentUserService currentUserService;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public DashboardStatsResponse stats() {
        User user = currentUserService.get();
        List<Task> tasks = taskRepository.findAllAccessibleByUserId(user.getId());
        LocalDate today = LocalDate.now();
        LocalDate nextWeek = today.plusDays(7);

        Map<TaskStatus, Long> byStatus = enumCounts(TaskStatus.values());
        Map<TaskPriority, Long> byPriority = enumCounts(TaskPriority.values());
        tasks.forEach(task -> {
            byStatus.compute(task.getStatus(), (key, count) -> count + 1);
            byPriority.compute(task.getPriority(), (key, count) -> count + 1);
        });

        long overdue = tasks.stream()
                .filter(task -> task.getStatus() != TaskStatus.COMPLETED)
                .filter(task -> task.getDueDate() != null && task.getDueDate().isBefore(today))
                .count();
        long dueSoon = tasks.stream()
                .filter(task -> task.getStatus() != TaskStatus.COMPLETED)
                .filter(task -> task.getDueDate() != null)
                .filter(task -> !task.getDueDate().isBefore(today) && !task.getDueDate().isAfter(nextWeek))
                .count();
        long assignedToMe = tasks.stream()
                .filter(task -> task.getAssignee() != null && task.getAssignee().getId().equals(user.getId()))
                .count();

        return new DashboardStatsResponse(
                projectRepository.findAccessibleByUserId(user.getId()).size(),
                tasks.size(),
                assignedToMe,
                overdue,
                dueSoon,
                notificationRepository.countByRecipientIdAndIsReadFalse(user.getId()),
                byStatus,
                byPriority
        );
    }

    private <E extends Enum<E>> Map<E, Long> enumCounts(E[] values) {
        Map<E, Long> counts = new EnumMap<>(values[0].getDeclaringClass());
        for (E value : values) counts.put(value, 0L);
        return counts;
    }
}
