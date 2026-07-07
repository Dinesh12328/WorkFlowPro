package com.workflowpro.dto;

import com.workflowpro.entity.TaskPriority;
import com.workflowpro.entity.TaskStatus;

import java.util.Map;

public record DashboardStatsResponse(
        long totalProjects,
        long totalTasks,
        long assignedToMe,
        long overdue,
        long dueInNextSevenDays,
        long unreadNotifications,
        Map<TaskStatus, Long> byStatus,
        Map<TaskPriority, Long> byPriority
) {}
