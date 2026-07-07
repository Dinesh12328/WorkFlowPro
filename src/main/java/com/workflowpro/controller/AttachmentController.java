package com.workflowpro.controller;

import com.workflowpro.dto.AttachmentDtos.AttachmentRequest;
import com.workflowpro.dto.AttachmentDtos.AttachmentResponse;
import com.workflowpro.service.AttachmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks/{taskId}/attachments")
@RequiredArgsConstructor
public class AttachmentController {
    private final AttachmentService attachmentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AttachmentResponse add(@PathVariable Long taskId, @Valid @RequestBody AttachmentRequest request) {
        return attachmentService.add(taskId, request);
    }

    @GetMapping
    public List<AttachmentResponse> list(@PathVariable Long taskId) {
        return attachmentService.list(taskId);
    }
}
