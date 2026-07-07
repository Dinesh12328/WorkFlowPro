package com.workflowpro.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled:false}")
    private boolean enabled;

    @Async
    public void sendTaskAssignment(String recipient, String assigneeName, String taskTitle, String projectName) {
        if (!enabled) {
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(recipient);
        message.setSubject("WorkFlowPro: New task assigned");
        message.setText("""
                Hi %s,

                You have been assigned the task "%s" in project "%s".

                Sign in to WorkFlowPro to view the details.
                """.formatted(assigneeName, taskTitle, projectName));
        try {
            mailSender.send(message);
        } catch (MailException ex) {
            log.warn("Unable to send assignment email to {}: {}", recipient, ex.getMessage());
        }
    }
}
