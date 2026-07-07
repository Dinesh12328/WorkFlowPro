package com.workflowpro.dto;

import com.workflowpro.entity.Comment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class CommentDtos {
    private CommentDtos() {}

    public record CommentRequest(@NotBlank @Size(max = 3000) String content) {}

    public record CommentResponse(
            Long id,
            String content,
            UserResponse author,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static CommentResponse from(Comment comment) {
            return new CommentResponse(
                    comment.getId(),
                    comment.getContent(),
                    UserResponse.from(comment.getAuthor()),
                    comment.getCreatedAt(),
                    comment.getUpdatedAt()
            );
        }
    }
}
