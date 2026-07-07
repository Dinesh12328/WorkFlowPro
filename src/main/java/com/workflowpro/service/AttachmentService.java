package com.workflowpro.service;

import com.workflowpro.dto.AttachmentDtos.AttachmentRequest;
import com.workflowpro.dto.AttachmentDtos.AttachmentResponse;
import com.workflowpro.entity.Attachment;
import com.workflowpro.entity.Task;
import com.workflowpro.entity.User;
import com.workflowpro.repository.AttachmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AttachmentService {
    private final AttachmentRepository attachmentRepository;
    private final CurrentUserService currentUserService;
    private final TaskService taskService;

    @Transactional
    public AttachmentResponse add(Long taskId, AttachmentRequest request) {
        User user = currentUserService.get();
        Task task = taskService.requireAccessible(taskId, user);
        Attachment attachment = new Attachment();
        attachment.setFileName(request.fileName().trim());
        attachment.setUrl(request.url().trim());
        attachment.setContentType(request.contentType());
        attachment.setSizeBytes(request.sizeBytes());
        attachment.setTask(task);
        attachment.setUploadedBy(user);
        return AttachmentResponse.from(attachmentRepository.save(attachment));
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponse> list(Long taskId) {
        User user = currentUserService.get();
        taskService.requireAccessible(taskId, user);
        return attachmentRepository.findByTaskIdOrderByCreatedAtDesc(taskId).stream()
                .map(AttachmentResponse::from)
                .toList();
    }
}
