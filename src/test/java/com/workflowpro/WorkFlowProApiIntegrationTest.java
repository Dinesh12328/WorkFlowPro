package com.workflowpro;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WorkFlowProApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void frontendLandingPageIsPublic() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));

        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("WorkFlowPro")));
    }

    @Test
    void coreWorkflowCreatesProjectTaskCommentAndDashboardStats() throws Exception {
        JsonNode auth = register("Ava Manager", "ava@example.com", "password123");
        String token = auth.get("token").asText();
        long userId = auth.path("user").path("id").asLong();

        JsonNode project = postJson(
                "/api/projects",
                token,
                Map.of(
                        "name", "Website Relaunch",
                        "description", "Public site refresh",
                        "memberIds", List.of()
                ),
                201
        );
        long projectId = project.get("id").asLong();

        JsonNode task = postJson(
                "/api/tasks",
                token,
                Map.of(
                        "title", "Create launch checklist",
                        "description", "Prepare release tasks",
                        "priority", "HIGH",
                        "status", "TODO",
                        "dueDate", LocalDate.now().plusDays(3).toString(),
                        "projectId", projectId,
                        "assigneeId", userId
                ),
                201
        );
        long taskId = task.get("id").asLong();

        postJson(
                "/api/tasks/" + taskId + "/comments",
                token,
                Map.of("content", "Kickoff notes added."),
                201
        );

        mockMvc.perform(get("/api/tasks/status/TODO").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].priority").value("HIGH"));

        mockMvc.perform(get("/api/dashboard/stats").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProjects").value(1))
                .andExpect(jsonPath("$.totalTasks").value(1))
                .andExpect(jsonPath("$.assignedToMe").value(1))
                .andExpect(jsonPath("$.byStatus.TODO").value(1))
                .andExpect(jsonPath("$.byPriority.HIGH").value(1));
    }

    private JsonNode register(String name, String email, String password) throws Exception {
        return postJson(
                "/api/auth/register",
                null,
                Map.of("name", name, "email", email, "password", password),
                201
        );
    }

    private JsonNode postJson(String path, String token, Object body, int expectedStatus) throws Exception {
        var request = post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body));
        if (token != null) request.header("Authorization", bearer(token));

        String response = mockMvc.perform(request)
                .andExpect(status().is(expectedStatus))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return response.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(response);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
