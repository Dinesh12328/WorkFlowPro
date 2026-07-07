package com.workflowpro.service;

import com.workflowpro.dto.UserResponse;
import com.workflowpro.entity.User;
import com.workflowpro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public UserResponse me() {
        return UserResponse.from(currentUserService.get());
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> search(String query, Pageable pageable) {
        Page<User> users = query == null || query.isBlank()
                ? userRepository.findAll(pageable)
                : userRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                        query.trim(), query.trim(), pageable
                );
        return users.map(UserResponse::from);
    }
}
