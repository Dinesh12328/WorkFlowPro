const TOKEN_KEY = "workflowpro.token";
const USER_KEY = "workflowpro.user";
const REFRESH_INTERVAL_MS = 30000;
const STAT_KEYS = [
    "totalProjects",
    "totalTasks",
    "assignedToMe",
    "overdue",
    "dueInNextSevenDays",
    "unreadNotifications"
];

const state = {
    token: localStorage.getItem(TOKEN_KEY),
    me: readStoredUser(),
    users: [],
    projects: [],
    tasks: [],
    notifications: [],
    comments: new Map(),
    attachments: new Map(),
    refreshTimer: null
};

const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => Array.from(document.querySelectorAll(selector));

document.addEventListener("DOMContentLoaded", () => {
    bindAuth();
    bindApp();

    if (state.token) {
        startApp().catch((error) => {
            endSession(false);
            showAuth();
            notify(error.message || "Please login again.", "error");
        });
    } else {
        showAuth();
    }
});

function bindAuth() {
    $$("[data-auth-tab]").forEach((button) => {
        button.addEventListener("click", () => {
            const mode = button.dataset.authTab;
            $$("[data-auth-tab]").forEach((tab) => tab.classList.toggle("active", tab === button));
            $("#loginForm").hidden = mode !== "login";
            $("#registerForm").hidden = mode !== "register";
        });
    });

    $("#loginForm").addEventListener("submit", async (event) => {
        event.preventDefault();
        const payload = formValues(event.currentTarget);
        try {
            const auth = await api("/api/auth/login", {
                method: "POST",
                body: JSON.stringify(payload)
            }, false);
            setSession(auth);
            await startApp();
            notify("Welcome back.", "success");
        } catch (error) {
            notify(error.message, "error");
        }
    });

    $("#registerForm").addEventListener("submit", async (event) => {
        event.preventDefault();
        const payload = formValues(event.currentTarget);
        try {
            const auth = await api("/api/auth/register", {
                method: "POST",
                body: JSON.stringify(payload)
            }, false);
            setSession(auth);
            await startApp();
            notify("Account created. You are signed in.", "success");
        } catch (error) {
            notify(error.message, "error");
        }
    });
}

function bindApp() {
    $("#logoutButton").addEventListener("click", () => {
        endSession(true);
        showAuth();
    });

    $("#refreshButton").addEventListener("click", async () => {
        await refreshAll();
        notify("Latest data loaded.", "success");
    });

    $("#taskForm select[name='projectId']").addEventListener("change", renderTaskAssigneeOptions);

    $("#projectForm").addEventListener("submit", async (event) => {
        event.preventDefault();
        const values = formValues(event.currentTarget);
        const memberIds = $$("input[name='projectMembers']:checked").map((input) => Number(input.value));
        try {
            await api("/api/projects", {
                method: "POST",
                body: JSON.stringify({
                    name: values.name,
                    description: values.description || null,
                    memberIds
                })
            });
            event.currentTarget.reset();
            await refreshAll();
            notify("Project created.", "success");
        } catch (error) {
            notify(error.message, "error");
        }
    });

    $("#taskForm").addEventListener("submit", async (event) => {
        event.preventDefault();
        const values = formValues(event.currentTarget);
        if (!values.projectId) {
            notify("Create a project before adding tasks.", "error");
            return;
        }
        try {
            await api("/api/tasks", {
                method: "POST",
                body: JSON.stringify({
                    title: values.title,
                    description: values.description || null,
                    priority: values.priority || "MEDIUM",
                    status: values.status || "TODO",
                    dueDate: values.dueDate || null,
                    projectId: Number(values.projectId),
                    assigneeId: values.assigneeId ? Number(values.assigneeId) : null
                })
            });
            event.currentTarget.reset();
            await refreshAll();
            renderTaskAssigneeOptions();
            notify("Task created.", "success");
        } catch (error) {
            notify(error.message, "error");
        }
    });

    $("#filterForm").addEventListener("submit", async (event) => {
        event.preventDefault();
        await loadTasks();
    });

    $("#clearFiltersButton").addEventListener("click", async () => {
        $("#filterForm").reset();
        await loadTasks();
    });

    $("#taskList").addEventListener("click", handleTaskAction);
    $("#taskList").addEventListener("submit", handleTaskForm);
    $("#notificationsList").addEventListener("click", handleNotificationAction);
}

async function startApp() {
    showApp();
    state.me = await api("/api/users/me");
    localStorage.setItem(USER_KEY, JSON.stringify(state.me));
    renderSession();
    await refreshAll();
    startAutoRefresh();
}

async function refreshAll() {
    if (!state.token) return;
    try {
        await Promise.all([
            loadUsers(),
            loadProjects(),
            loadDashboard(),
            loadNotifications()
        ]);
        await loadTasks();
        setLastUpdated();
    } catch (error) {
        notify(error.message, "error");
    }
}

async function loadUsers() {
    const page = await api("/api/users?size=100&sort=name,asc");
    state.users = page.content || [];
    renderUsers();
    renderMemberChoices();
    renderFilterAssigneeOptions();
    renderTaskAssigneeOptions();
}

async function loadProjects() {
    state.projects = await api("/api/projects");
    renderProjects();
    renderProjectOptions();
    renderTaskAssigneeOptions();
}

async function loadTasks() {
    const params = new URLSearchParams({ size: "50", sort: "createdAt,desc" });
    const filters = formValues($("#filterForm"));
    Object.entries(filters).forEach(([key, value]) => {
        if (value) params.set(key, value);
    });
    const page = await api(`/api/tasks?${params.toString()}`);
    state.tasks = page.content || [];
    renderTasks();
}

async function loadDashboard() {
    const stats = await api("/api/dashboard/stats");
    STAT_KEYS.forEach((key) => {
        const element = document.querySelector(`[data-stat="${key}"]`);
        if (element) element.textContent = stats[key] ?? 0;
    });
}

async function loadNotifications() {
    const page = await api("/api/notifications?size=10&sort=createdAt,desc");
    state.notifications = page.content || [];
    renderNotifications();
}

async function handleTaskAction(event) {
    const button = event.target.closest("[data-task-action]");
    if (!button) return;

    const taskId = Number(button.dataset.taskId);
    const action = button.dataset.taskAction;

    try {
        if (action === "start") {
            await updateTask(taskId, { status: "IN_PROGRESS" });
            notify("Task moved to in progress.", "success");
        }
        if (action === "complete") {
            await updateTask(taskId, { status: "COMPLETED" });
            notify("Task completed.", "success");
        }
        if (action === "todo") {
            await updateTask(taskId, { status: "TODO" });
            notify("Task moved back to todo.", "success");
        }
        if (action === "comments") {
            await toggleTaskPanel(taskId, "comments");
            return;
        }
        if (action === "attachments") {
            await toggleTaskPanel(taskId, "attachments");
            return;
        }
        await refreshAll();
    } catch (error) {
        notify(error.message, "error");
    }
}

async function handleTaskForm(event) {
    const form = event.target;
    if (!form.matches("[data-comment-form]") && !form.matches("[data-attachment-form]")) return;

    event.preventDefault();
    const taskId = Number(form.dataset.taskId);
    const values = formValues(form);

    try {
        if (form.matches("[data-comment-form]")) {
            await api(`/api/tasks/${taskId}/comments`, {
                method: "POST",
                body: JSON.stringify({ content: values.content })
            });
            form.reset();
            await loadComments(taskId);
            notify("Comment added.", "success");
        }

        if (form.matches("[data-attachment-form]")) {
            await api(`/api/tasks/${taskId}/attachments`, {
                method: "POST",
                body: JSON.stringify({
                    fileName: values.fileName,
                    url: values.url,
                    contentType: values.contentType || null,
                    sizeBytes: values.sizeBytes ? Number(values.sizeBytes) : null
                })
            });
            form.reset();
            await loadAttachments(taskId);
            notify("Attachment link added.", "success");
        }
    } catch (error) {
        notify(error.message, "error");
    }
}

async function handleNotificationAction(event) {
    const button = event.target.closest("[data-notification-id]");
    if (!button) return;

    try {
        await api(`/api/notifications/${button.dataset.notificationId}/read`, { method: "PATCH" });
        await Promise.all([loadNotifications(), loadDashboard()]);
        notify("Notification marked as read.", "success");
    } catch (error) {
        notify(error.message, "error");
    }
}

async function updateTask(taskId, payload) {
    await api(`/api/tasks/${taskId}`, {
        method: "PUT",
        body: JSON.stringify(payload)
    });
}

async function toggleTaskPanel(taskId, panelName) {
    const panel = document.querySelector(`[data-${panelName}-panel="${taskId}"]`);
    if (!panel) return;
    panel.hidden = !panel.hidden;
    if (panel.hidden) return;
    if (panelName === "comments") await loadComments(taskId);
    if (panelName === "attachments") await loadAttachments(taskId);
}

async function loadComments(taskId) {
    const comments = await api(`/api/tasks/${taskId}/comments`);
    state.comments.set(taskId, comments);
    const target = document.querySelector(`[data-comments-list="${taskId}"]`);
    if (!target) return;
    target.innerHTML = comments.length
        ? comments.map(renderComment).join("")
        : `<div class="empty-state">No comments yet.</div>`;
}

async function loadAttachments(taskId) {
    const attachments = await api(`/api/tasks/${taskId}/attachments`);
    state.attachments.set(taskId, attachments);
    const target = document.querySelector(`[data-attachments-list="${taskId}"]`);
    if (!target) return;
    target.innerHTML = attachments.length
        ? attachments.map(renderAttachment).join("")
        : `<div class="empty-state">No attachment links yet.</div>`;
}

function renderSession() {
    $("#userName").textContent = state.me ? `${state.me.name} · ${state.me.role}` : "Signed in";
}

function renderUsers() {
    $("#userCount").textContent = String(state.users.length);
    $("#userList").innerHTML = state.users.length
        ? state.users.map((user) => `
            <div class="mini-item">
                <div>
                    <strong>${escapeHtml(user.name)}</strong>
                    <small>${escapeHtml(user.email)} · ID ${user.id}</small>
                </div>
            </div>
        `).join("")
        : "No users loaded.";
}

function renderMemberChoices() {
    const candidates = state.users.filter((user) => !state.me || user.id !== state.me.id);
    $("#projectMembers").innerHTML = candidates.length
        ? candidates.map((user) => `
            <label class="choice-item">
                <input type="checkbox" name="projectMembers" value="${user.id}">
                <span>${escapeHtml(user.name)} <small>(${escapeHtml(user.email)})</small></span>
            </label>
        `).join("")
        : `<span class="muted">Register more users to add project members.</span>`;
}

function renderProjectOptions() {
    const taskProject = $("#taskForm select[name='projectId']");
    const filterProject = $("#filterForm select[name='projectId']");
    const selectedTaskProject = taskProject.value;
    const selectedFilterProject = filterProject.value;
    const options = state.projects.map((project) => `<option value="${project.id}">${escapeHtml(project.name)}</option>`).join("");
    taskProject.innerHTML = options || `<option value="">Create a project first</option>`;
    filterProject.innerHTML = `<option value="">Any</option>${options}`;
    if (selectedTaskProject && state.projects.some((project) => String(project.id) === selectedTaskProject)) {
        taskProject.value = selectedTaskProject;
    }
    if (selectedFilterProject && state.projects.some((project) => String(project.id) === selectedFilterProject)) {
        filterProject.value = selectedFilterProject;
    }
}

function renderFilterAssigneeOptions() {
    const options = state.users.map((user) => `<option value="${user.id}">${escapeHtml(user.name)}</option>`).join("");
    $("#filterForm select[name='assigneeId']").innerHTML = `<option value="">Any</option>${options}`;
}

function renderTaskAssigneeOptions() {
    const taskProject = $("#taskForm select[name='projectId']");
    const assigneeSelect = $("#taskForm select[name='assigneeId']");
    const project = state.projects.find((item) => String(item.id) === taskProject.value);
    const previous = assigneeSelect.value;
    const allowedUsers = project
        ? [project.owner, ...project.members].filter(Boolean)
        : state.me ? [state.me] : [];
    const uniqueUsers = Array.from(new Map(allowedUsers.map((user) => [user.id, user])).values());
    const options = uniqueUsers
        .map((user) => `<option value="${user.id}">${escapeHtml(user.name)}</option>`)
        .join("");
    assigneeSelect.innerHTML = `<option value="">Unassigned</option>${options}`;
    if (previous && uniqueUsers.some((user) => String(user.id) === previous)) {
        assigneeSelect.value = previous;
    }
}

function renderProjects() {
    $("#projectCount").textContent = String(state.projects.length);
    $("#projectList").innerHTML = state.projects.length
        ? state.projects.map((project) => `
            <div class="mini-item">
                <div>
                    <strong>${escapeHtml(project.name)}</strong>
                    <small>${project.members.length} member${project.members.length === 1 ? "" : "s"} · owner ${escapeHtml(project.owner.name)}</small>
                </div>
            </div>
        `).join("")
        : "No projects yet.";
}

function renderNotifications() {
    $("#notificationCount").textContent = String(state.notifications.length);
    $("#notificationsList").innerHTML = state.notifications.length
        ? state.notifications.map((notification) => `
            <div class="mini-item ${notification.read ? "" : "notification-unread"}">
                <div>
                    <strong>${escapeHtml(notification.type)}</strong>
                    <small>${escapeHtml(notification.message)}</small>
                    <small>${formatDateTime(notification.createdAt)}</small>
                </div>
                ${notification.read ? "" : `<button class="link-button" data-notification-id="${notification.id}" type="button">Read</button>`}
            </div>
        `).join("")
        : "No notifications.";
}

function renderTasks() {
    $("#taskCount").textContent = `${state.tasks.length} visible`;
    $("#taskList").innerHTML = state.tasks.length
        ? state.tasks.map(renderTask).join("")
        : `<div class="empty-state">No tasks match this view. Create one or clear filters.</div>`;
}

function renderTask(task) {
    const description = task.description
        ? `<p>${escapeHtml(task.description)}</p>`
        : `<p class="muted">No description yet.</p>`;
    const assignee = task.assignee ? task.assignee.name : "Unassigned";
    const cardClass = task.status === "COMPLETED"
        ? "completed"
        : task.status === "IN_PROGRESS" ? "in-progress" : "";

    return `
        <article class="task-card ${cardClass}">
            <div class="task-top">
                <div>
                    <h3>${escapeHtml(task.title)}</h3>
                    <small>${escapeHtml(task.projectName)} · assigned to ${escapeHtml(assignee)}</small>
                </div>
                <span class="status-pill">${formatEnum(task.status)}</span>
            </div>
            <div class="meta-row">
                <span class="priority-pill priority-${task.priority.toLowerCase()}">${task.priority}</span>
                <small>Due: ${task.dueDate ? formatDate(task.dueDate) : "No deadline"}</small>
                ${task.completedAt ? `<small>Completed: ${formatDateTime(task.completedAt)}</small>` : ""}
            </div>
            ${description}
            <div class="task-actions">
                <button type="button" data-task-action="todo" data-task-id="${task.id}">Todo</button>
                <button type="button" data-task-action="start" data-task-id="${task.id}">Start</button>
                <button type="button" data-task-action="complete" data-task-id="${task.id}">Complete</button>
                <button type="button" data-task-action="comments" data-task-id="${task.id}">Comments</button>
                <button type="button" data-task-action="attachments" data-task-id="${task.id}">Attachments</button>
            </div>

            <div class="task-panel" data-comments-panel="${task.id}" hidden>
                <strong>Comments</strong>
                <div class="mini-list" data-comments-list="${task.id}"></div>
                <form class="form-stack compact" data-comment-form data-task-id="${task.id}">
                    <label>
                        Add comment
                        <textarea name="content" maxlength="3000" rows="2" required placeholder="Write an update..."></textarea>
                    </label>
                    <button class="small-button" type="submit">Add comment</button>
                </form>
            </div>

            <div class="task-panel" data-attachments-panel="${task.id}" hidden>
                <strong>Attachment links</strong>
                <div class="mini-list" data-attachments-list="${task.id}"></div>
                <form class="form-stack compact" data-attachment-form data-task-id="${task.id}">
                    <div class="two-column">
                        <label>
                            File name
                            <input name="fileName" maxlength="255" required placeholder="brief.pdf">
                        </label>
                        <label>
                            URL
                            <input name="url" maxlength="1000" required placeholder="https://...">
                        </label>
                    </div>
                    <div class="two-column">
                        <label>
                            Content type
                            <input name="contentType" maxlength="100" placeholder="application/pdf">
                        </label>
                        <label>
                            Size bytes
                            <input name="sizeBytes" type="number" min="0" placeholder="Optional">
                        </label>
                    </div>
                    <button class="small-button" type="submit">Add attachment</button>
                </form>
            </div>
        </article>
    `;
}

function renderComment(comment) {
    return `
        <div class="mini-item">
            <div>
                <strong>${escapeHtml(comment.author.name)}</strong>
                <small>${escapeHtml(comment.content)}</small>
                <small>${formatDateTime(comment.createdAt)}</small>
            </div>
        </div>
    `;
}

function renderAttachment(attachment) {
    return `
        <div class="mini-item">
            <div>
                <strong>${escapeHtml(attachment.fileName)}</strong>
                <small>${escapeHtml(attachment.contentType || "link")} · ${attachment.sizeBytes ?? "unknown"} bytes</small>
                <small>Uploaded by ${escapeHtml(attachment.uploadedBy.name)}</small>
            </div>
            <a class="link-button" href="${escapeAttribute(attachment.url)}" target="_blank" rel="noreferrer">Open</a>
        </div>
    `;
}

async function api(path, options = {}, includeAuth = true) {
    const headers = new Headers(options.headers || {});
    headers.set("Accept", "application/json");
    if (options.body && !headers.has("Content-Type")) {
        headers.set("Content-Type", "application/json");
    }
    if (includeAuth && state.token) {
        headers.set("Authorization", `Bearer ${state.token}`);
    }

    const response = await fetch(path, { ...options, headers });
    const text = await response.text();
    let data = null;
    try {
        data = text ? JSON.parse(text) : null;
    } catch {
        data = text;
    }

    if (response.status === 401 && includeAuth) {
        endSession(false);
        showAuth();
        throw new Error("Session expired. Please login again.");
    }

    if (!response.ok) {
        throw new Error(errorMessage(data, response.status));
    }

    return data;
}

function errorMessage(data, status) {
    if (data && typeof data === "object") {
        if (data.validationErrors) {
            return Object.entries(data.validationErrors)
                .map(([field, message]) => `${field}: ${message}`)
                .join(", ");
        }
        if (data.message) return data.message;
    }
    return `Request failed with status ${status}`;
}

function setSession(auth) {
    state.token = auth.token;
    state.me = auth.user;
    localStorage.setItem(TOKEN_KEY, auth.token);
    localStorage.setItem(USER_KEY, JSON.stringify(auth.user));
}

function endSession(showMessage) {
    stopAutoRefresh();
    state.token = null;
    state.me = null;
    state.users = [];
    state.projects = [];
    state.tasks = [];
    state.notifications = [];
    state.comments.clear();
    state.attachments.clear();
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    if (showMessage) notify("Logged out.", "success");
}

function showAuth() {
    $("#authView").hidden = false;
    $("#appView").hidden = true;
    $("#sessionPill").hidden = true;
}

function showApp() {
    $("#authView").hidden = true;
    $("#appView").hidden = false;
    $("#sessionPill").hidden = false;
    renderSession();
}

function formValues(form) {
    return Object.fromEntries(new FormData(form).entries());
}

function readStoredUser() {
    try {
        return JSON.parse(localStorage.getItem(USER_KEY) || "null");
    } catch {
        return null;
    }
}

function notify(message, type = "") {
    const toast = $("#toast");
    toast.textContent = message;
    toast.className = `toast show ${type}`;
    window.clearTimeout(notify.timer);
    notify.timer = window.setTimeout(() => {
        toast.className = "toast";
    }, 3600);
}

function startAutoRefresh() {
    stopAutoRefresh();
    state.refreshTimer = window.setInterval(() => {
        if (!document.hidden && state.token) {
            refreshAll();
        }
    }, REFRESH_INTERVAL_MS);
}

function stopAutoRefresh() {
    if (state.refreshTimer) {
        window.clearInterval(state.refreshTimer);
        state.refreshTimer = null;
    }
}

function setLastUpdated() {
    const target = $("#lastUpdated");
    if (target) {
        target.textContent = `Synced ${new Intl.DateTimeFormat(undefined, {
            hour: "2-digit",
            minute: "2-digit",
            second: "2-digit"
        }).format(new Date())}`;
    }
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function escapeAttribute(value) {
    return escapeHtml(value).replaceAll("`", "&#096;");
}

function formatEnum(value) {
    return String(value || "").replaceAll("_", " ");
}

function formatDate(value) {
    return new Intl.DateTimeFormat(undefined, { dateStyle: "medium" }).format(new Date(`${value}T00:00:00`));
}

function formatDateTime(value) {
    if (!value) return "";
    return new Intl.DateTimeFormat(undefined, {
        dateStyle: "medium",
        timeStyle: "short"
    }).format(new Date(value));
}
