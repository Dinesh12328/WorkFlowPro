package com.workflowpro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class WorkflowProApplication {
    public static void main(String[] args) {
        SpringApplication.run(WorkflowProApplication.class, args);
    }
}
