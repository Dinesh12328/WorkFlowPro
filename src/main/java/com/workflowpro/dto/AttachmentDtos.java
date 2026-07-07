package com.workflowpro.dto;

import com.workflowpro.entity.Attachment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class AttachmentDtos {
    private AttachmentDtos() {}

    public record AttachmentRequest(
            @NotBlank @Size(max = 255) String fileName,
            @NotBlank @Size(max = 1000) String url,
            @Size(max = 100) String contentType,
            @PositiveOrZero Long sizeBytes
    ) {}

    public record AttachmentResponse(
            Long id,
            String fileName,
            String url,
            String contentType,
            Long sizeBytes,
            UserResponse uploadedBy,
            Instant createdAt
    ) {
        public static AttachmentResponse from(Attachment attachment) {
            return new AttachmentResponse(
                    attachment.getId(),
                    attachment.getFileName(),
                    attachment.getUrl(),
                    attachment.getContentType(),
                    attachment.getSizeBytes(),
                    UserResponse.from(attachment.getUploadedBy()),
                    attachment.getCreatedAt()
            );
        }
    }
}
