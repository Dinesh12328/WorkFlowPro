package com.workflowpro.service;

import com.workflowpro.dto.CommentDtos.CommentRequest;
import com.workflowpro.dto.CommentDtos.CommentResponse;
import com.workflowpro.entity.Comment;
import com.workflowpro.entity.NotificationType;
import com.workflowpro.entity.Task;
import com.workflowpro.entity.User;
import com.workflowpro.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final CurrentUserService currentUserService;
    private final TaskService taskService;
    private final NotificationService notificationService;

    @Transactional
    public CommentResponse add(Long taskId, CommentRequest request) {
        User author = currentUserService.get();
        Task task = taskService.requireAccessible(taskId, author);
        Comment comment = new Comment();
        comment.setContent(request.content().trim());
        comment.setTask(task);
        comment.setAuthor(author);
        comment = commentRepository.save(comment);

        if (task.getAssignee() != null && !task.getAssignee().getId().equals(author.getId())) {
            notificationService.create(
                    task.getAssignee(),
                    NotificationType.COMMENT_ADDED,
                    author.getName() + " commented on task \"" + task.getTitle() + "\"",
                    task.getId()
            );
        }
        return CommentResponse.from(comment);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> list(Long taskId) {
        User user = currentUserService.get();
        taskService.requireAccessible(taskId, user);
        return commentRepository.findByTaskIdOrderByCreatedAtAsc(taskId).stream()
                .map(CommentResponse::from)
                .toList();
    }
}
