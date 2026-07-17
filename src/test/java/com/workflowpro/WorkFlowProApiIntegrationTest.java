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
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    void frontendLandingPageAndAssetsArePublic() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));

        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("WorkFlowPro")))
                .andExpect(content().string(containsString("projectModalTitle")))
                .andExpect(content().string(containsString("projectSubmitButton")))
                .andExpect(content().string(containsString("Close")));

        mockMvc.perform(get("/styles.css"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(":root")));

        mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("workflowpro.token")))
                .andExpect(content().string(containsString("data-edit-project")))
                .andExpect(content().string(containsString("data-project-task")))
                .andExpect(content().string(containsString("data-task-status")))
                .andExpect(content().string(containsString("project-actions")));
    }

    @Test
    void protectedApiRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required"));

        mockMvc.perform(get("/api/dashboard/stats"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticationLifecycleHandlesRegisterLoginDuplicatesAndBadCredentials() throws Exception {
        JsonNode auth = register("Auth User", "auth.user@example.com", "password123");
        String token = auth.get("token").asText();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Auth User Again",
                                "email", "AUTH.USER@example.com",
                                "password", "password123"
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("An account with this email already exists"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "auth.user@example.com",
                                "password", "wrong-password"
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));

        postJson(
                "/api/auth/login",
                null,
                Map.of("email", "auth.user@example.com", "password", "password123"),
                200
        ).path("token").asText().contains(".");

        mockMvc.perform(get("/api/users/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("auth.user@example.com"));
    }

    @Test
    void projectTaskCommentAttachmentNotificationAndDashboardEndpointsWorkTogether() throws Exception {
        JsonNode ownerAuth = register("Flow Owner", "flow.owner@example.com", "password123");
        JsonNode memberAuth = register("Flow Member", "flow.member@example.com", "password123");
        String ownerToken = ownerAuth.get("token").asText();
        String memberToken = memberAuth.get("token").asText();
        long memberId = memberAuth.path("user").path("id").asLong();

        JsonNode project = postJson(
                "/api/projects",
                ownerToken,
                Map.of(
                        "name", "Website Relaunch",
                        "description", "Public site refresh",
                        "memberIds", List.of(memberId)
                ),
                201
        );
        long projectId = project.get("id").asLong();

        putJson(
                "/api/projects/" + projectId,
                ownerToken,
                Map.of(
                        "name", "Website Relaunch 2.0",
                        "description", "Sharper project plan",
                        "memberIds", List.of(memberId)
                ),
                200
        );

        mockMvc.perform(get("/api/projects").header("Authorization", ownerToken(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Website Relaunch 2.0"));

        mockMvc.perform(get("/api/projects/" + projectId).header("Authorization", bearer(memberToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members", hasSize(1)));

        mockMvc.perform(get("/api/users?query=flow.member&size=20").header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].email").value("flow.member@example.com"));

        JsonNode task = postJson(
                "/api/tasks",
                ownerToken,
                Map.of(
                        "title", "Create launch checklist",
                        "description", "Prepare release tasks",
                        "priority", "HIGH",
                        "status", "TODO",
                        "dueDate", LocalDate.now().plusDays(3).toString(),
                        "projectId", projectId,
                        "assigneeId", memberId
                ),
                201
        );
        long taskId = task.get("id").asLong();

        mockMvc.perform(get("/api/tasks/" + taskId).header("Authorization", bearer(memberToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignee.email").value("flow.member@example.com"));

        mockMvc.perform(get("/api/tasks")
                        .param("status", "TODO")
                        .param("priority", "HIGH")
                        .param("projectId", String.valueOf(projectId))
                        .param("assigneeId", String.valueOf(memberId))
                        .param("dueFrom", LocalDate.now().toString())
                        .param("dueTo", LocalDate.now().plusDays(7).toString())
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Create launch checklist"));

        mockMvc.perform(get("/api/tasks/status/TODO").header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get("/api/tasks/priority/HIGH").header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        postJson("/api/tasks/" + taskId + "/comments", memberToken, Map.of("content", "Kickoff notes added."), 201);
        mockMvc.perform(get("/api/tasks/" + taskId + "/comments").header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].author.email").value("flow.member@example.com"));

        postJson(
                "/api/tasks/" + taskId + "/attachments",
                ownerToken,
                Map.of(
                        "fileName", "brief.pdf",
                        "url", "https://example.com/brief.pdf",
                        "contentType", "application/pdf",
                        "sizeBytes", 2048
                ),
                201
        );
        mockMvc.perform(get("/api/tasks/" + taskId + "/attachments").header("Authorization", bearer(memberToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].fileName").value("brief.pdf"));

        JsonNode notifications = getJson("/api/notifications?size=10", memberToken, 200);
        long notificationId = notifications.path("content").get(0).path("id").asLong();

        mockMvc.perform(get("/api/notifications?size=10").header("Authorization", bearer(memberToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].read").value(false));

        mockMvc.perform(patch("/api/notifications/" + notificationId + "/read").header("Authorization", bearer(memberToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));

        mockMvc.perform(get("/api/dashboard/stats").header("Authorization", bearer(memberToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProjects").value(1))
                .andExpect(jsonPath("$.totalTasks").value(1))
                .andExpect(jsonPath("$.assignedToMe").value(1))
                .andExpect(jsonPath("$.unreadNotifications").value(0))
                .andExpect(jsonPath("$.byStatus.TODO").value(1))
                .andExpect(jsonPath("$.byPriority.HIGH").value(1));

        putJson("/api/tasks/" + taskId, ownerToken, Map.of("status", "IN_PROGRESS", "priority", "MEDIUM"), 200);
        mockMvc.perform(get("/api/tasks/" + taskId).header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.priority").value("MEDIUM"));

        mockMvc.perform(put("/api/tasks/" + taskId)
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "COMPLETED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedAt").exists());

        mockMvc.perform(put("/api/tasks/" + taskId)
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "status", "TODO",
                                "clearDueDate", true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.dueDate").value(nullValue()))
                .andExpect(jsonPath("$.completedAt").value(nullValue()));

        putJson(
                "/api/projects/" + projectId,
                ownerToken,
                Map.of(
                        "name", "Website Relaunch 2.1",
                        "description", "Member removed from project",
                        "memberIds", List.of()
                ),
                200
        );

        mockMvc.perform(get("/api/projects/" + projectId).header("Authorization", bearer(memberToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/tasks/" + taskId).header("Authorization", bearer(memberToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/tasks/" + taskId).header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignee").value(nullValue()));

        JsonNode disposableTask = postJson(
                "/api/tasks",
                ownerToken,
                Map.of("title", "Disposable task", "projectId", projectId, "priority", "LOW"),
                201
        );
        long disposableTaskId = disposableTask.get("id").asLong();
        postJson("/api/tasks/" + disposableTaskId + "/comments", ownerToken, Map.of("content", "Can be deleted."), 201);

        mockMvc.perform(delete("/api/tasks/" + disposableTaskId).header("Authorization", bearer(ownerToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tasks/" + disposableTaskId).header("Authorization", bearer(ownerToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/projects/" + projectId).header("Authorization", bearer(ownerToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/projects/" + projectId).header("Authorization", bearer(ownerToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void projectAccessControlBlocksOutsidersAndInvalidAssignments() throws Exception {
        JsonNode ownerAuth = register("Secure Owner", "secure.owner@example.com", "password123");
        JsonNode outsiderAuth = register("Secure Outsider", "secure.outsider@example.com", "password123");
        String ownerToken = ownerAuth.get("token").asText();
        String outsiderToken = outsiderAuth.get("token").asText();
        long outsiderId = outsiderAuth.path("user").path("id").asLong();

        JsonNode project = postJson(
                "/api/projects",
                ownerToken,
                Map.of("name", "Private Project", "memberIds", List.of()),
                201
        );
        long projectId = project.get("id").asLong();

        mockMvc.perform(get("/api/projects/" + projectId).header("Authorization", bearer(outsiderToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", bearer(outsiderToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Sneaky task",
                                "projectId", projectId
                        ))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Invalid assignment",
                                "projectId", projectId,
                                "assigneeId", outsiderId
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Assignee must be a project owner or member"));
    }

    private JsonNode register(String name, String email, String password) throws Exception {
        return postJson(
                "/api/auth/register",
                null,
                Map.of("name", name, "email", email, "password", password),
                201
        );
    }

    private JsonNode getJson(String path, String token, int expectedStatus) throws Exception {
        String response = mockMvc.perform(get(path).header("Authorization", bearer(token)))
                .andExpect(status().is(expectedStatus))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return response.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(response);
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

    private JsonNode putJson(String path, String token, Object body, int expectedStatus) throws Exception {
        String response = mockMvc.perform(put(path)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().is(expectedStatus))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return response.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(response);
    }

    private String ownerToken(String token) {
        return bearer(token);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
