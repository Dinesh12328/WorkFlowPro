package com.workflowpro.service;

import com.workflowpro.dto.ProjectDtos.ProjectRequest;
import com.workflowpro.dto.ProjectDtos.ProjectResponse;
import com.workflowpro.entity.Project;
import com.workflowpro.entity.User;
import com.workflowpro.exception.ApiException;
import com.workflowpro.repository.ProjectRepository;
import com.workflowpro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final ProjectAccessService accessService;

    @Transactional
    public ProjectResponse create(ProjectRequest request) {
        User owner = currentUserService.get();
        Project project = new Project();
        apply(project, request, owner);
        project.setOwner(owner);
        return ProjectResponse.from(projectRepository.save(project));
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> list() {
        User user = currentUserService.get();
        return projectRepository.findAccessibleByUserId(user.getId()).stream()
                .map(ProjectResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(Long id) {
        return ProjectResponse.from(accessService.requireView(id, currentUserService.get()));
    }

    @Transactional
    public ProjectResponse update(Long id, ProjectRequest request) {
        User user = currentUserService.get();
        Project project = accessService.requireManage(id, user);
        apply(project, request, project.getOwner());
        return ProjectResponse.from(project);
    }

    @Transactional
    public void delete(Long id) {
        Project project = accessService.requireManage(id, currentUserService.get());
        projectRepository.delete(project);
    }

    private void apply(Project project, ProjectRequest request, User owner) {
        project.setName(request.name().trim());
        project.setDescription(request.description() == null ? null : request.description().trim());
        Set<Long> memberIds = request.memberIds() == null ? Set.of() : request.memberIds();
        Set<User> members = new HashSet<>(userRepository.findAllById(memberIds));
        if (members.size() != memberIds.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "One or more project members do not exist");
        }
        members.removeIf(member -> member.getId().equals(owner.getId()));
        project.setMembers(members);
    }
}
