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

const VIEW_META = {
    dashboard: {
        title: "Dashboard",
        subtitle: "A live overview of your projects, tasks, deadlines, and notifications."
    },
    board: {
        title: "Task board",
        subtitle: "Drag tasks between columns, filter work, and open task cards for details."
    },
    projects: {
        title: "Projects",
        subtitle: "Create projects, review owners and members, and jump into project work."
    },
    notifications: {
        title: "Notifications",
        subtitle: "Track assignment alerts and mark updates as read."
    },
    team: {
        title: "Team",
        subtitle: "View registered users who can be added to projects and assigned tasks."
    }
};

const STATUS_LABELS = {
    TODO: "Todo",
    IN_PROGRESS: "In progress",
    COMPLETED: "Completed"
};

const state = {
    token: localStorage.getItem(TOKEN_KEY),
    me: readStoredUser(),
    activeView: "dashboard",
    users: [],
    projects: [],
    allTasks: [],
    tasks: [],
    notifications: [],
    stats: {},
    comments: new Map(),
    attachments: new Map(),
    selectedTaskId: null,
    refreshTimer: null
};

const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => Array.from(document.querySelectorAll(selector));

document.addEventListener("DOMContentLoaded", () => {
    bindAuth();
    bindWorkspace();

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
        await authenticate("/api/auth/login", event.currentTarget, "Welcome back.");
    });

    $("#registerForm").addEventListener("submit", async (event) => {
        event.preventDefault();
        await authenticate("/api/auth/register", event.currentTarget, "Account created. You are signed in.");
    });
}

function bindWorkspace() {
    $("#logoutButton").addEventListener("click", () => {
        endSession(true);
        showAuth();
    });

    $("#refreshButton").addEventListener("click", async () => {
        await refreshAll();
        notify("Workspace refreshed.", "success");
    });

    $$("[data-view]").forEach((button) => {
        button.addEventListener("click", () => setView(button.dataset.view));
    });

    document.addEventListener("click", async (event) => {
        const jump = event.target.closest("[data-view-jump]");
        if (jump) setView(jump.dataset.viewJump);

        const openButton = event.target.closest("[data-open-modal]");
        if (openButton) openModal(openButton.dataset.openModal);

        if (event.target.closest("[data-close-modal]")) closeModal();
    });

    $("#modalBackdrop").addEventListener("click", (event) => {
        if (event.target.id === "modalBackdrop") closeModal();
    });

    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape") {
            closeModal();
            closeTaskDrawer();
        }
    });

    $("#projectForm").addEventListener("submit", handleProjectCreate);
    $("#taskForm").addEventListener("submit", handleTaskCreate);
    $("#taskForm select[name='projectId']").addEventListener("change", renderTaskAssigneeOptions);

    $("#filterForm").addEventListener("submit", (event) => {
        event.preventDefault();
        applyTaskFilters();
    });

    $("#filterForm").addEventListener("input", debounce(applyTaskFilters, 180));
    $("#filterForm").addEventListener("change", applyTaskFilters);

    $("#clearFiltersButton").addEventListener("click", () => {
        $("#filterForm").reset();
        applyTaskFilters();
    });

    $("#kanbanBoard").addEventListener("click", handleBoardClick);
    $("#kanbanBoard").addEventListener("dragstart", handleDragStart);
    $("#kanbanBoard").addEventListener("dragend", handleDragEnd);
    $("#kanbanBoard").addEventListener("dragover", handleDragOver);
    $("#kanbanBoard").addEventListener("dragleave", handleDragLeave);
    $("#kanbanBoard").addEventListener("drop", handleDrop);

    $("#taskDrawer").addEventListener("click", handleDrawerClick);
    $("#taskDrawer").addEventListener("submit", handleDrawerSubmit);
    $("#notificationsList").addEventListener("click", handleNotificationClick);
    $("#dashboardNotifications").addEventListener("click", handleNotificationClick);
    $("#dueSoonList").addEventListener("click", handleOpenTaskClick);
    $("#projectList").addEventListener("click", handleProjectClick);
}

async function authenticate(path, form, successMessage) {
    setBusy(form, true);
    try {
        const auth = await api(path, {
            method: "POST",
            body: JSON.stringify(formValues(form))
        }, false);
        setSession(auth);
        form.reset();
        await startApp();
        notify(successMessage, "success");
    } catch (error) {
        notify(error.message, "error");
    } finally {
        setBusy(form, false);
    }
}

async function startApp() {
    showApp();
    setView(state.activeView || "dashboard");
    state.me = await api("/api/users/me");
    localStorage.setItem(USER_KEY, JSON.stringify(state.me));
    renderSession();
    await refreshAll();
    startAutoRefresh();
}

async function refreshAll() {
    if (!state.token) return;
    setConnection("Syncing", false);
    try {
        await loadUsers();
        await loadProjects();
        await Promise.all([loadDashboard(), loadNotifications(), loadTasks()]);
        renderWorkspace();
        setLastUpdated();
        setConnection("Connected", true);
    } catch (error) {
        setConnection("Disconnected", false);
        notify(error.message, "error");
    }
}

async function loadUsers() {
    const page = await api("/api/users?size=100&sort=name,asc");
    state.users = pageItems(page);
}

async function loadProjects() {
    state.projects = await api("/api/projects");
}

async function loadDashboard() {
    state.stats = await api("/api/dashboard/stats");
}

async function loadNotifications() {
    const page = await api("/api/notifications?size=20&sort=createdAt,desc");
    state.notifications = pageItems(page);
}

async function loadTasks() {
    const page = await api("/api/tasks?size=200&sort=createdAt,desc");
    state.allTasks = pageItems(page);
    applyTaskFilters(false);
}

async function handleProjectCreate(event) {
    event.preventDefault();
    const form = event.currentTarget;
    const values = formValues(form);
    const memberIds = $$("input[name='projectMembers']:checked").map((input) => Number(input.value));

    setBusy(form, true);
    try {
        await api("/api/projects", {
            method: "POST",
            body: JSON.stringify({
                name: values.name,
                description: values.description || null,
                memberIds
            })
        });
        form.reset();
        closeModal();
        await refreshAll();
        setView("projects");
        notify("Project created.", "success");
    } catch (error) {
        notify(error.message, "error");
    } finally {
        setBusy(form, false);
    }
}

async function handleTaskCreate(event) {
    event.preventDefault();
    const form = event.currentTarget;
    const values = formValues(form);

    if (!values.projectId) {
        notify("Create a project before adding tasks.", "error");
        return;
    }

    setBusy(form, true);
    try {
        const task = await api("/api/tasks", {
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
        form.reset();
        closeModal();
        await refreshAll();
        setView("board");
        openTaskDrawer(task.id);
        notify("Task created.", "success");
    } catch (error) {
        notify(error.message, "error");
    } finally {
        setBusy(form, false);
    }
}

function applyTaskFilters(render = true) {
    const filters = formValues($("#filterForm"));
    const search = (filters.search || "").trim().toLowerCase();
    state.tasks = state.allTasks.filter((task) => {
        if (filters.status && task.status !== filters.status) return false;
        if (filters.priority && task.priority !== filters.priority) return false;
        if (filters.projectId && String(task.projectId) !== filters.projectId) return false;
        if (filters.assigneeId && String(task.assignee?.id || "") !== filters.assigneeId) return false;
        if (filters.dueFrom && (!task.dueDate || task.dueDate < filters.dueFrom)) return false;
        if (filters.dueTo && (!task.dueDate || task.dueDate > filters.dueTo)) return false;
        if (!search) return true;
        return [
            task.title,
            task.description,
            task.projectName,
            task.assignee?.name,
            task.assignee?.email,
            task.priority,
            task.status
        ].some((value) => String(value || "").toLowerCase().includes(search));
    });
    if (render) renderWorkspace();
}

function renderWorkspace() {
    renderSession();
    renderStats();
    renderProjectOptions();
    renderMemberChoices();
    renderTaskAssigneeOptions();
    renderFilterAssigneeOptions();
    renderDashboard();
    renderBoard();
    renderProjects();
    renderNotifications();
    renderUsers();
    renderTaskDrawer();
}

function renderSession() {
    $("#userName").textContent = state.me ? `${state.me.name} - ${state.me.role}` : "Signed in";
}

function renderStats() {
    STAT_KEYS.forEach((key) => {
        const target = document.querySelector(`[data-stat="${key}"]`);
        if (target) target.textContent = state.stats[key] ?? 0;
    });
}

function renderDashboard() {
    const counts = countByStatus(state.allTasks);
    $("#dashboardBoardSummary").innerHTML = Object.entries(STATUS_LABELS).map(([status, label]) => `
        <article class="summary-card">
            <small>${label}</small>
            <strong>${counts[status] || 0}</strong>
            <span class="pill">${status.replaceAll("_", " ")}</span>
        </article>
    `).join("");

    const today = localDateString(new Date());
    const nextWeek = localDateString(new Date(Date.now() + 7 * 24 * 60 * 60 * 1000));
    const dueSoon = state.allTasks
        .filter((task) => task.status !== "COMPLETED")
        .filter((task) => task.dueDate && task.dueDate >= today && task.dueDate <= nextWeek)
        .slice(0, 6);
    $("#dueSoonList").innerHTML = dueSoon.length
        ? dueSoon.map((task) => miniTaskItem(task)).join("")
        : `<div class="empty-state">No tasks due in the next seven days.</div>`;

    const recent = state.notifications.slice(0, 5);
    $("#dashboardNotifications").innerHTML = recent.length
        ? recent.map((notification) => notificationItem(notification, true)).join("")
        : `<div class="empty-state">No recent notifications.</div>`;
}

function renderBoard() {
    const counts = countByStatus(state.tasks);
    Object.keys(STATUS_LABELS).forEach((status) => {
        const lane = document.querySelector(`[data-column="${status}"]`);
        const counter = document.querySelector(`[data-column-count="${status}"]`);
        const tasks = state.tasks.filter((task) => task.status === status);
        if (counter) counter.textContent = String(tasks.length);
        if (lane) {
            lane.innerHTML = tasks.length
                ? tasks.map(renderTaskCard).join("")
                : `<div class="empty-state">No ${STATUS_LABELS[status].toLowerCase()} tasks.</div>`;
        }
    });

    const boardSummary = Object.entries(counts)
        .map(([status, count]) => `${STATUS_LABELS[status]}: ${count}`)
        .join(" | ");
    $("#workspaceSubtitle").dataset.boardSummary = boardSummary;
}

function renderTaskCard(task) {
    const assignee = task.assignee ? task.assignee.name : "Unassigned";
    const dueLabel = task.dueDate ? formatDate(task.dueDate) : "No deadline";
    return `
        <article class="task-card" draggable="true" data-task-id="${task.id}">
            <div class="task-top">
                <div>
                    <h3>${escapeHtml(task.title)}</h3>
                    <small>${escapeHtml(task.projectName)} - ${escapeHtml(assignee)}</small>
                </div>
                <span class="pill priority-${task.priority.toLowerCase()}">${task.priority}</span>
            </div>
            ${task.description ? `<p>${escapeHtml(shorten(task.description, 110))}</p>` : ""}
            <div class="meta-row">
                <span class="pill">${dueLabel}</span>
                <span class="pill">${STATUS_LABELS[task.status]}</span>
            </div>
        </article>
    `;
}

function renderProjects() {
    $("#projectCount").textContent = `${state.projects.length} project${state.projects.length === 1 ? "" : "s"}`;
    $("#projectList").innerHTML = state.projects.length
        ? state.projects.map((project) => {
            const taskCount = state.allTasks.filter((task) => task.projectId === project.id).length;
            const memberNames = project.members.length
                ? project.members.map((member) => avatar(member.name, member.email)).join("")
                : `<small>No extra members yet</small>`;
            const canDelete = state.me && project.owner.id === state.me.id;
            return `
                <article class="project-card">
                    <div class="project-card-header">
                        <div>
                            <strong>${escapeHtml(project.name)}</strong>
                            <small>Owner: ${escapeHtml(project.owner.name)} - ${taskCount} task${taskCount === 1 ? "" : "s"}</small>
                        </div>
                        <span class="pill">${project.members.length} members</span>
                    </div>
                    <p class="muted">${escapeHtml(project.description || "No description added.")}</p>
                    <div class="avatar-list">${memberNames}</div>
                    <div class="drawer-actions">
                        <button class="small-button" type="button" data-project-board="${project.id}">View board</button>
                        ${canDelete ? `<button class="small-button" type="button" data-delete-project="${project.id}">Delete</button>` : ""}
                    </div>
                </article>
            `;
        }).join("")
        : `<div class="empty-state">No projects yet. Create your first project to unlock task creation.</div>`;
}

function renderUsers() {
    $("#userCount").textContent = `${state.users.length} user${state.users.length === 1 ? "" : "s"}`;
    $("#userList").innerHTML = state.users.length
        ? state.users.map((user) => `
            <article class="team-card">
                <div class="brand">
                    ${avatar(user.name, user.email)}
                    <div>
                        <strong>${escapeHtml(user.name)}</strong>
                        <small>${escapeHtml(user.email)}</small>
                    </div>
                </div>
                <span class="pill">${user.role}</span>
            </article>
        `).join("")
        : `<div class="empty-state">No team users loaded.</div>`;
}

function renderNotifications() {
    $("#notificationCount").textContent = `${state.notifications.length} notification${state.notifications.length === 1 ? "" : "s"}`;
    $("#notificationsList").innerHTML = state.notifications.length
        ? state.notifications.map((notification) => notificationItem(notification)).join("")
        : `<div class="empty-state">No notifications yet.</div>`;
}

function renderTaskDrawer() {
    const drawer = $("#taskDrawer");
    const task = selectedTask();
    if (!task) {
        drawer.classList.remove("open");
        drawer.innerHTML = `
            <div class="drawer-empty">
                <span class="drawer-icon">Open</span>
                <h3>Select a task</h3>
                <p>Open any task card to edit details, add comments, or attach useful links.</p>
            </div>
        `;
        return;
    }

    drawer.classList.add("open");
    const project = state.projects.find((item) => item.id === task.projectId);
    const assigneeOptions = taskAssigneeOptions(project, task.assignee?.id);
    const comments = state.comments.get(task.id);
    const attachments = state.attachments.get(task.id);

    drawer.innerHTML = `
        <div class="task-drawer-header">
            <div>
                <span class="eyebrow">${STATUS_LABELS[task.status]}</span>
                <h3>${escapeHtml(task.title)}</h3>
                <small>${escapeHtml(task.projectName)} - ${task.assignee ? escapeHtml(task.assignee.name) : "Unassigned"}</small>
            </div>
            <button class="icon-button" type="button" data-close-drawer aria-label="Close">&times;</button>
        </div>

        <form class="drawer-section" data-task-edit-form data-task-id="${task.id}">
            <h4>Edit task</h4>
            <label>
                Title
                <input name="title" maxlength="200" required value="${escapeAttribute(task.title)}">
            </label>
            <label>
                Description
                <textarea name="description" maxlength="5000" rows="4">${escapeHtml(task.description || "")}</textarea>
            </label>
            <div class="two-column">
                <label>
                    Status
                    <select name="status">${statusOptions(task.status)}</select>
                </label>
                <label>
                    Priority
                    <select name="priority">${priorityOptions(task.priority)}</select>
                </label>
            </div>
            <div class="two-column">
                <label>
                    Due date
                    <input name="dueDate" type="date" value="${task.dueDate || ""}">
                </label>
                <label>
                    Assignee
                    <select name="assigneeId"><option value="">Unassigned</option>${assigneeOptions}</select>
                </label>
            </div>
            <div class="drawer-actions">
                <button class="primary-button" type="submit">Save task</button>
                <button class="secondary-button" type="button" data-delete-task="${task.id}">Delete</button>
            </div>
        </form>

        <section class="drawer-section">
            <h4>Comments</h4>
            <div class="mini-list" data-comments-list="${task.id}">
                ${comments ? renderComments(comments) : `<div class="empty-state">Open comments loading...</div>`}
            </div>
            <form class="form-stack compact" data-comment-form data-task-id="${task.id}">
                <label>
                    Add comment
                    <textarea name="content" maxlength="3000" rows="2" required placeholder="Write an update..."></textarea>
                </label>
                <button class="secondary-button" type="submit">Add comment</button>
            </form>
        </section>

        <section class="drawer-section">
            <h4>Attachment links</h4>
            <div class="mini-list" data-attachments-list="${task.id}">
                ${attachments ? renderAttachments(attachments) : `<div class="empty-state">Attachment links loading...</div>`}
            </div>
            <form class="form-stack compact" data-attachment-form data-task-id="${task.id}">
                <label>
                    File name
                    <input name="fileName" maxlength="255" required placeholder="brief.pdf">
                </label>
                <label>
                    URL
                    <input name="url" maxlength="1000" required placeholder="https://...">
                </label>
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
                <button class="secondary-button" type="submit">Add attachment</button>
            </form>
        </section>
    `;
}

function renderProjectOptions() {
    const taskProject = $("#taskForm select[name='projectId']");
    const filterProject = $("#filterForm select[name='projectId']");
    const selectedTaskProject = taskProject.value;
    const selectedFilterProject = filterProject.value;
    const projectOptions = state.projects
        .map((project) => `<option value="${project.id}">${escapeHtml(project.name)}</option>`)
        .join("");

    taskProject.innerHTML = projectOptions || `<option value="">Create a project first</option>`;
    filterProject.innerHTML = `<option value="">Any</option>${projectOptions}`;
    if (selectedTaskProject && state.projects.some((project) => String(project.id) === selectedTaskProject)) {
        taskProject.value = selectedTaskProject;
    }
    if (selectedFilterProject && state.projects.some((project) => String(project.id) === selectedFilterProject)) {
        filterProject.value = selectedFilterProject;
    }
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

function renderTaskAssigneeOptions() {
    const projectId = $("#taskForm select[name='projectId']").value;
    const assigneeSelect = $("#taskForm select[name='assigneeId']");
    const previous = assigneeSelect.value;
    const project = state.projects.find((item) => String(item.id) === projectId);
    assigneeSelect.innerHTML = `<option value="">Unassigned</option>${taskAssigneeOptions(project, previous)}`;
}

function renderFilterAssigneeOptions() {
    const filterAssignee = $("#filterForm select[name='assigneeId']");
    const previous = filterAssignee.value;
    const options = state.users
        .map((user) => `<option value="${user.id}">${escapeHtml(user.name)}</option>`)
        .join("");
    filterAssignee.innerHTML = `<option value="">Any</option>${options}`;
    if (previous && state.users.some((user) => String(user.id) === previous)) {
        filterAssignee.value = previous;
    }
}

function handleBoardClick(event) {
    const card = event.target.closest("[data-task-id]");
    if (!card) return;
    openTaskDrawer(Number(card.dataset.taskId));
}

function handleOpenTaskClick(event) {
    const button = event.target.closest("[data-open-task]");
    if (!button) return;
    openTaskDrawer(Number(button.dataset.openTask));
}

function handleDragStart(event) {
    const card = event.target.closest(".task-card");
    if (!card) return;
    card.classList.add("dragging");
    event.dataTransfer.setData("text/plain", card.dataset.taskId);
    event.dataTransfer.effectAllowed = "move";
}

function handleDragEnd(event) {
    event.target.closest(".task-card")?.classList.remove("dragging");
    $$(".kanban-column").forEach((column) => column.classList.remove("drag-over"));
}

function handleDragOver(event) {
    const column = event.target.closest(".kanban-column");
    if (!column) return;
    event.preventDefault();
    column.classList.add("drag-over");
}

function handleDragLeave(event) {
    const column = event.target.closest(".kanban-column");
    if (column && !column.contains(event.relatedTarget)) {
        column.classList.remove("drag-over");
    }
}

async function handleDrop(event) {
    const column = event.target.closest(".kanban-column");
    if (!column) return;
    event.preventDefault();
    column.classList.remove("drag-over");
    const taskId = Number(event.dataTransfer.getData("text/plain"));
    const status = column.dataset.dropStatus;
    const task = state.allTasks.find((item) => item.id === taskId);
    if (!task || task.status === status) return;
    await updateTask(taskId, { status }, `Task moved to ${STATUS_LABELS[status]}.`);
}

async function handleDrawerClick(event) {
    if (event.target.closest("[data-close-drawer]")) {
        closeTaskDrawer();
        return;
    }

    const deleteButton = event.target.closest("[data-delete-task]");
    if (deleteButton) {
        const taskId = Number(deleteButton.dataset.deleteTask);
        if (!confirm("Delete this task? Comments and attachment links will also be removed.")) return;
        try {
            await api(`/api/tasks/${taskId}`, { method: "DELETE" });
            state.selectedTaskId = null;
            await refreshAll();
            notify("Task deleted.", "success");
        } catch (error) {
            notify(error.message, "error");
        }
    }
}

async function handleDrawerSubmit(event) {
    const form = event.target;
    if (!form.matches("[data-task-edit-form], [data-comment-form], [data-attachment-form]")) return;
    event.preventDefault();
    const taskId = Number(form.dataset.taskId);
    const values = formValues(form);
    setBusy(form, true);

    try {
        if (form.matches("[data-task-edit-form]")) {
            await api(`/api/tasks/${taskId}`, {
                method: "PUT",
                body: JSON.stringify({
                    title: values.title,
                    description: values.description || null,
                    priority: values.priority,
                    status: values.status,
                    dueDate: values.dueDate || null,
                    clearDueDate: !values.dueDate,
                    assigneeId: values.assigneeId ? Number(values.assigneeId) : null,
                    clearAssignee: !values.assigneeId
                })
            });
            await refreshAll();
            notify("Task updated.", "success");
        }

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
    } finally {
        setBusy(form, false);
    }
}

async function handleNotificationClick(event) {
    const button = event.target.closest("[data-notification-id]");
    if (!button) return;
    try {
        await api(`/api/notifications/${button.dataset.notificationId}/read`, { method: "PATCH" });
        await Promise.all([loadNotifications(), loadDashboard()]);
        renderWorkspace();
        notify("Notification marked as read.", "success");
    } catch (error) {
        notify(error.message, "error");
    }
}

async function handleProjectClick(event) {
    const boardButton = event.target.closest("[data-project-board]");
    if (boardButton) {
        $("#filterForm select[name='projectId']").value = boardButton.dataset.projectBoard;
        applyTaskFilters();
        setView("board");
        return;
    }

    const deleteButton = event.target.closest("[data-delete-project]");
    if (deleteButton) {
        const projectId = Number(deleteButton.dataset.deleteProject);
        if (!confirm("Delete this project and its tasks?")) return;
        try {
            await api(`/api/projects/${projectId}`, { method: "DELETE" });
            if ($("#filterForm select[name='projectId']").value === String(projectId)) {
                $("#filterForm select[name='projectId']").value = "";
            }
            await refreshAll();
            notify("Project deleted.", "success");
        } catch (error) {
            notify(error.message, "error");
        }
    }
}

async function updateTask(taskId, payload, successMessage = "Task updated.") {
    try {
        await api(`/api/tasks/${taskId}`, {
            method: "PUT",
            body: JSON.stringify(payload)
        });
        await refreshAll();
        notify(successMessage, "success");
    } catch (error) {
        notify(error.message, "error");
    }
}

async function loadComments(taskId) {
    const comments = await api(`/api/tasks/${taskId}/comments`);
    state.comments.set(taskId, comments);
    renderTaskDrawer();
}

async function loadAttachments(taskId) {
    const attachments = await api(`/api/tasks/${taskId}/attachments`);
    state.attachments.set(taskId, attachments);
    renderTaskDrawer();
}

async function openTaskDrawer(taskId) {
    state.selectedTaskId = taskId;
    renderTaskDrawer();
    await Promise.allSettled([loadComments(taskId), loadAttachments(taskId)]);
}

function closeTaskDrawer() {
    state.selectedTaskId = null;
    renderTaskDrawer();
}

function setView(view) {
    state.activeView = view;
    const meta = VIEW_META[view] || VIEW_META.dashboard;
    $("#workspaceTitle").textContent = meta.title;
    $("#workspaceSubtitle").textContent = meta.subtitle;
    $("#workspaceEyebrow").textContent = view === "board" ? "Interactive board" : "Workspace";

    $$("[data-view]").forEach((button) => button.classList.toggle("active", button.dataset.view === view));
    $$("[data-panel]").forEach((panel) => {
        const active = panel.dataset.panel === view;
        panel.hidden = !active;
        panel.classList.toggle("active", active);
    });
}

function openModal(id) {
    if (id === "taskModal" && !state.projects.length) {
        notify("Create a project first, then add tasks.", "error");
        openModal("projectModal");
        return;
    }
    $("#modalBackdrop").hidden = false;
    $$("#modalBackdrop .modal").forEach((modal) => {
        modal.hidden = modal.id !== id;
    });
    renderProjectOptions();
    renderMemberChoices();
    renderTaskAssigneeOptions();
}

function closeModal() {
    $("#modalBackdrop").hidden = true;
    $$("#modalBackdrop .modal").forEach((modal) => {
        modal.hidden = true;
    });
}

async function api(path, options = {}, includeAuth = true) {
    const headers = new Headers(options.headers || {});
    headers.set("Accept", "application/json");
    if (options.body && !headers.has("Content-Type")) headers.set("Content-Type", "application/json");
    if (includeAuth && state.token) headers.set("Authorization", `Bearer ${state.token}`);

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

    if (!response.ok) throw new Error(errorMessage(data, response.status));
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
    state.allTasks = [];
    state.tasks = [];
    state.notifications = [];
    state.stats = {};
    state.comments.clear();
    state.attachments.clear();
    state.selectedTaskId = null;
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    if (showMessage) notify("Logged out.", "success");
}

function showAuth() {
    $("#authView").hidden = false;
    $("#appView").hidden = true;
    closeModal();
}

function showApp() {
    $("#authView").hidden = true;
    $("#appView").hidden = false;
}

function startAutoRefresh() {
    stopAutoRefresh();
    state.refreshTimer = window.setInterval(() => {
        if (!document.hidden && state.token) refreshAll();
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

function setConnection(label, online) {
    $("#connectionLabel").textContent = label;
    document.querySelector(".status-dot")?.classList.toggle("offline", !online);
}

function selectedTask() {
    return state.allTasks.find((task) => task.id === state.selectedTaskId);
}

function taskAssigneeOptions(project, selectedId) {
    const users = project
        ? [project.owner, ...project.members].filter(Boolean)
        : state.me ? [state.me] : [];
    const uniqueUsers = Array.from(new Map(users.map((user) => [user.id, user])).values());
    return uniqueUsers.map((user) => `
        <option value="${user.id}" ${String(user.id) === String(selectedId || "") ? "selected" : ""}>
            ${escapeHtml(user.name)}
        </option>
    `).join("");
}

function statusOptions(selected) {
    return Object.entries(STATUS_LABELS)
        .map(([value, label]) => `<option value="${value}" ${value === selected ? "selected" : ""}>${label}</option>`)
        .join("");
}

function priorityOptions(selected) {
    return ["LOW", "MEDIUM", "HIGH"]
        .map((value) => `<option value="${value}" ${value === selected ? "selected" : ""}>${value}</option>`)
        .join("");
}

function countByStatus(tasks) {
    return tasks.reduce((counts, task) => {
        counts[task.status] = (counts[task.status] || 0) + 1;
        return counts;
    }, { TODO: 0, IN_PROGRESS: 0, COMPLETED: 0 });
}

function miniTaskItem(task) {
    return `
        <div class="mini-item">
            <div>
                <strong>${escapeHtml(task.title)}</strong>
                <small>${escapeHtml(task.projectName)} - ${task.dueDate ? formatDate(task.dueDate) : "No deadline"}</small>
            </div>
            <button class="small-button" type="button" data-open-task="${task.id}">Open</button>
        </div>
    `;
}

function notificationItem(notification, compact = false) {
    return `
        <article class="${compact ? "mini-item" : "notification-item"} ${notification.read ? "" : "notification-unread"}">
            <div>
                <strong>${escapeHtml(notification.type.replaceAll("_", " "))}</strong>
                <small>${escapeHtml(notification.message)}</small>
                <small>${formatDateTime(notification.createdAt)}</small>
            </div>
            ${notification.read ? `<span class="pill">Read</span>` : `<button class="small-button" type="button" data-notification-id="${notification.id}">Mark read</button>`}
        </article>
    `;
}

function renderComments(comments) {
    return comments.length
        ? comments.map((comment) => `
            <article class="comment-card">
                <strong>${escapeHtml(comment.author.name)}</strong>
                <small>${formatDateTime(comment.createdAt)}</small>
                <p>${escapeHtml(comment.content)}</p>
            </article>
        `).join("")
        : `<div class="empty-state">No comments yet.</div>`;
}

function renderAttachments(attachments) {
    return attachments.length
        ? attachments.map((attachment) => `
            <article class="attachment-card">
                <strong>${escapeHtml(attachment.fileName)}</strong>
                <small>${escapeHtml(attachment.contentType || "link")} - ${attachment.sizeBytes ?? "unknown"} bytes</small>
                <a class="link-button" href="${escapeAttribute(attachment.url)}" target="_blank" rel="noreferrer">Open link</a>
            </article>
        `).join("")
        : `<div class="empty-state">No attachment links yet.</div>`;
}

function avatar(name, email) {
    const initials = String(name || email || "?")
        .split(/\s+/)
        .filter(Boolean)
        .slice(0, 2)
        .map((part) => part[0])
        .join("")
        .toUpperCase();
    return `<span class="avatar" title="${escapeAttribute(email || name)}">${escapeHtml(initials || "?")}</span>`;
}

function pageItems(page) {
    if (Array.isArray(page)) return page;
    if (Array.isArray(page?.content)) return page.content;
    if (Array.isArray(page?._embedded?.items)) return page._embedded.items;
    return [];
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
    }, 3800);
}

function setBusy(element, busy) {
    element.classList.toggle("loading", busy);
    Array.from(element.querySelectorAll("button")).forEach((button) => {
        button.disabled = busy;
    });
}

function debounce(fn, delay) {
    let timer;
    return (...args) => {
        window.clearTimeout(timer);
        timer = window.setTimeout(() => fn(...args), delay);
    };
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

function shorten(value, length) {
    const text = String(value || "");
    return text.length > length ? `${text.slice(0, length - 3)}...` : text;
}

function localDateString(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
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
