package com.workflowpro.service;

import com.workflowpro.entity.Project;
import com.workflowpro.entity.Role;
import com.workflowpro.entity.User;
import com.workflowpro.exception.ApiException;
import com.workflowpro.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProjectAccessService {
    private final ProjectRepository projectRepository;

    public Project requireProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Project not found"));
    }

    public Project requireView(Long projectId, User user) {
        Project project = requireProject(projectId);
        if (!canView(project, user)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You do not have access to this project");
        }
        return project;
    }

    public Project requireManage(Long projectId, User user) {
        Project project = requireProject(projectId);
        if (user.getRole() != Role.ADMIN && !project.getOwner().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the project owner can manage this project");
        }
        return project;
    }

    public boolean canView(Project project, User user) {
        return user.getRole() == Role.ADMIN
                || project.getOwner().getId().equals(user.getId())
                || project.getMembers().stream().anyMatch(member -> member.getId().equals(user.getId()));
    }
}
