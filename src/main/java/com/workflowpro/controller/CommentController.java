package com.workflowpro.controller;

import com.workflowpro.dto.CommentDtos.CommentRequest;
import com.workflowpro.dto.CommentDtos.CommentResponse;
import com.workflowpro.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks/{taskId}/comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse add(@PathVariable Long taskId, @Valid @RequestBody CommentRequest request) {
        return commentService.add(taskId, request);
    }

    @GetMapping
    public List<CommentResponse> list(@PathVariable Long taskId) {
        return commentService.list(taskId);
    }
}
