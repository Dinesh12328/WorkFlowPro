package com.workflowpro.service;

import com.workflowpro.dto.NotificationResponse;
import com.workflowpro.entity.Notification;
import com.workflowpro.entity.NotificationType;
import com.workflowpro.entity.User;
import com.workflowpro.exception.ApiException;
import com.workflowpro.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final CurrentUserService currentUserService;
    private final EmailService emailService;

    @Transactional
    public void taskAssigned(User recipient, Long taskId, String taskTitle, String projectName) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(NotificationType.TASK_ASSIGNED);
        notification.setMessage("You were assigned task \"" + taskTitle + "\" in " + projectName);
        notification.setReferenceId(taskId);
        notificationRepository.save(notification);
        emailService.sendTaskAssignment(recipient.getEmail(), recipient.getName(), taskTitle, projectName);
    }

    @Transactional
    public void create(User recipient, NotificationType type, String message, Long referenceId) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(type);
        notification.setMessage(message);
        notification.setReferenceId(referenceId);
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> list(Pageable pageable) {
        User user = currentUserService.get();
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(user.getId(), pageable)
                .map(NotificationResponse::from);
    }

    @Transactional
    public NotificationResponse markRead(Long id) {
        User user = currentUserService.get();
        Notification notification = notificationRepository.findByIdAndRecipientId(id, user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Notification not found"));
        notification.setRead(true);
        return NotificationResponse.from(notification);
    }
}
