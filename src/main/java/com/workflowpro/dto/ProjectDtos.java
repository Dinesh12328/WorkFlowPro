package com.workflowpro.dto;

import com.workflowpro.entity.Project;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public final class ProjectDtos {
    private ProjectDtos() {}

    public record ProjectRequest(
            @NotBlank @Size(max = 150) String name,
            @Size(max = 2000) String description,
            Set<Long> memberIds
    ) {}

    public record ProjectResponse(
            Long id,
            String name,
            String description,
            UserResponse owner,
            List<UserResponse> members,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static ProjectResponse from(Project project) {
            return new ProjectResponse(
                    project.getId(),
                    project.getName(),
                    project.getDescription(),
                    UserResponse.from(project.getOwner()),
                    project.getMembers().stream().map(UserResponse::from).toList(),
                    project.getCreatedAt(),
                    project.getUpdatedAt()
            );
        }
    }
}
