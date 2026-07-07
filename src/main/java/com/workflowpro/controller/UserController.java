package com.workflowpro.controller;

import com.workflowpro.dto.UserResponse;
import com.workflowpro.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public UserResponse me() {
        return userService.me();
    }

    @GetMapping
    public Page<UserResponse> search(@RequestParam(required = false) String query, Pageable pageable) {
        return userService.search(query, pageable);
    }
}
