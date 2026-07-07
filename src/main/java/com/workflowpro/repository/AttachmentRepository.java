package com.workflowpro.repository;

import com.workflowpro.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findByTaskIdOrderByCreatedAtDesc(Long taskId);
}
