package com.workflowpro.dto;

import com.workflowpro.entity.Role;
import com.workflowpro.entity.User;

import java.time.Instant;

public record UserResponse(Long id, String name, String email, Role role, Instant createdAt) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getCreatedAt());
    }
}
