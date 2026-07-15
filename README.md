# WorkFlowPro Task Management System

WorkFlowPro is a task and project management web application. It helps users create projects, add team members, assign tasks, track task progress, manage deadlines, add comments, attach useful links, and view project activity from a dashboard.

The project contains:

- A Spring Boot REST API backend.
- A browser-based interactive frontend served from the same application.
- MySQL support for normal running.
- H2 support for quick local development and testing.

After starting the application, open:

```text
http://localhost:8080/
```

## What this application does

WorkFlowPro is built around a simple project workflow:

1. A user registers or logs in.
2. The user creates a project.
3. Project members can be added.
4. Tasks are created inside the project.
5. Tasks can be assigned to project users.
6. Task status can move from `TODO` to `IN_PROGRESS` to `COMPLETED`.
7. Comments and attachment links can be added to tasks.
8. Notifications are created when a task is assigned.
9. The dashboard shows the current project/task summary.

## Main modules

### User module

Handles registration, login, current-user details, and user search.

### Project module

Handles project creation, project listing, project details, project updates, project members, and project deletion.

### Task module

Handles task creation, task assignment, priority, status, due date, task filtering, task editing, and task deletion.

### Comment module

Allows users to add and view comments on tasks.

### Attachment module

Stores attachment metadata or external file links for a task.

### Notification module

Creates and displays task assignment notifications.

### Dashboard module

Shows project and task statistics for the logged-in user.

## Frontend overview

The frontend is a single-page interface built with HTML, CSS, and JavaScript. It uses the backend APIs directly.

Main frontend screens:

- Login and registration screen
- Dashboard screen
- Kanban task board
- Project list
- Team/user list
- Notifications list
- Task detail drawer
- Project and task creation modals

Main frontend actions:

- Register and login
- Create projects
- Create tasks
- Drag tasks between board columns
- Edit task details
- Add comments
- Add attachment links
- Filter/search tasks
- Mark notifications as read
- Refresh workspace data

## Backend overview

The backend is a Spring Boot application with secured REST APIs. Authentication uses JWT. Most API routes require a bearer token.

Public routes:

```text
/
/index.html
/styles.css
/app.js
/api/auth/register
/api/auth/login
```

Protected routes require:

```http
Authorization: Bearer <token>
```

## Data model

Main entities:

- `User`
- `Project`
- `Task`
- `Comment`
- `Notification`
- `Attachment`

Main task values:

```text
Priority: LOW, MEDIUM, HIGH
Status: TODO, IN_PROGRESS, COMPLETED
```

## API endpoints

### Authentication

| Method | Endpoint | Use |
|---|---|---|
| `POST` | `/api/auth/register` | Register user |
| `POST` | `/api/auth/login` | Login user |

### Users

| Method | Endpoint | Use |
|---|---|---|
| `GET` | `/api/users/me` | Current user |
| `GET` | `/api/users` | List/search users |

### Projects

| Method | Endpoint | Use |
|---|---|---|
| `POST` | `/api/projects` | Create project |
| `GET` | `/api/projects` | List accessible projects |
| `GET` | `/api/projects/{id}` | Get project by ID |
| `PUT` | `/api/projects/{id}` | Update project |
| `DELETE` | `/api/projects/{id}` | Delete project |

### Tasks

| Method | Endpoint | Use |
|---|---|---|
| `POST` | `/api/tasks` | Create task |
| `GET` | `/api/tasks` | List/filter tasks |
| `GET` | `/api/tasks/{id}` | Get task by ID |
| `PUT` | `/api/tasks/{id}` | Update task |
| `DELETE` | `/api/tasks/{id}` | Delete task |
| `GET` | `/api/tasks/status/{status}` | List by status |
| `GET` | `/api/tasks/priority/{priority}` | List by priority |

Task filter parameters:

```text
status
priority
dueFrom
dueTo
projectId
assigneeId
page
size
sort
```

### Comments

| Method | Endpoint | Use |
|---|---|---|
| `POST` | `/api/tasks/{taskId}/comments` | Add task comment |
| `GET` | `/api/tasks/{taskId}/comments` | List task comments |

### Attachments

| Method | Endpoint | Use |
|---|---|---|
| `POST` | `/api/tasks/{taskId}/attachments` | Add attachment link |
| `GET` | `/api/tasks/{taskId}/attachments` | List attachment links |

### Notifications

| Method | Endpoint | Use |
|---|---|---|
| `GET` | `/api/notifications` | List notifications |
| `PATCH` | `/api/notifications/{id}/read` | Mark notification as read |

### Dashboard

| Method | Endpoint | Use |
|---|---|---|
| `GET` | `/api/dashboard/stats` | Dashboard statistics |

## Example requests

### Register

```json
{
  "name": "Dinesh",
  "email": "dinesh@example.com",
  "password": "password123"
}
```

### Login

```json
{
  "email": "dinesh@example.com",
  "password": "password123"
}
```

### Create project

```json
{
  "name": "Website Relaunch",
  "description": "Plan and track all launch tasks",
  "memberIds": [2, 3]
}
```

### Create task

```json
{
  "title": "Prepare launch checklist",
  "description": "Write the final launch tasks",
  "priority": "HIGH",
  "status": "TODO",
  "dueDate": "2026-07-30",
  "projectId": 1,
  "assigneeId": 2
}
```

## Run with Docker

Use Docker when you want the full application with MySQL.

1. Create a `.env` file from `.env.example`.

2. Start the app:

```bash
docker compose up --build
```

3. Open:

```text
http://localhost:8080/
```

4. Register a user and start using the application.

Stop containers:

```bash
docker compose down
```

Stop containers and remove the database volume:

```bash
docker compose down -v
```

## Run in IntelliJ IDEA

For easy local development, run with the `dev` profile. This uses H2 in-memory database, so MySQL is not required.

Steps:

1. Open IntelliJ IDEA.
2. Open this project folder.
3. Wait for Maven to import dependencies.
4. Open:

```text
src/main/java/com/workflowpro/WorkflowProApplication.java
```

5. Edit the run configuration.
6. Add this VM option:

```text
-Dspring.profiles.active=dev
```

7. Run the application.
8. Open:

```text
http://localhost:8080/
```

H2 console in dev mode:

```text
http://localhost:8080/h2-console
```

H2 connection:

```text
JDBC URL: jdbc:h2:mem:workflowpro-dev
User: sa
Password:
```

## Run with local MySQL

If MySQL is already running on your computer:

```bash
mvn spring-boot:run
```

Default database settings:

```text
Database: workflowpro
Username: workflowpro
Password: workflowpro
```

## Run tests

```bash
mvn test
```

The tests use H2 database and disabled email sending.

Current integration tests check:

- Frontend public files
- Protected API access
- Register and login
- Duplicate account handling
- Wrong password handling
- User search
- Project create/list/get/update/delete
- Task create/list/filter/get/update/delete
- Comments
- Attachments
- Notifications
- Dashboard statistics
- Project access rules
- Invalid task assignment rules

## Configuration

Common environment variables:

| Variable | Description |
|---|---|
| `MYSQL_DATABASE` | MySQL database name |
| `MYSQL_USER` | MySQL username |
| `MYSQL_PASSWORD` | MySQL password |
| `MYSQL_ROOT_PASSWORD` | MySQL root password |
| `DB_URL` | Spring datasource URL |
| `DB_USERNAME` | Spring datasource username |
| `DB_PASSWORD` | Spring datasource password |
| `JWT_SECRET` | JWT signing secret |
| `JWT_EXPIRATION` | JWT expiry time |
| `MAIL_ENABLED` | Enable/disable email notification |
| `MAIL_HOST` | SMTP host |
| `MAIL_PORT` | SMTP port |
| `MAIL_USERNAME` | SMTP username |
| `MAIL_PASSWORD` | SMTP password |

## Access rules

- A user can view projects they own or belong to.
- Only the project owner can update/delete a project.
- A task can only be assigned to the project owner or a project member.
- Comments and attachments follow task/project access.
- Notifications are visible only to the notification recipient.

## Troubleshooting

### Localhost shows access denied

Open the frontend URL:

```text
http://localhost:8080/
```

Do not open protected API routes directly before login, for example:

```text
http://localhost:8080/api/projects
```

Protected API routes need a JWT token.

### App does not start in IntelliJ

If MySQL is not running, use:

```text
-Dspring.profiles.active=dev
```

### Docker port is already used

Ports `8080` or `3306` may already be in use. Stop the other application or change the port mapping in `compose.yaml`.

### Data is empty after login

Project data is user-based. A user only sees projects they own or projects where they are added as a member.

### Emails are not sent

Email sending is disabled by default.

Enable it with:

```text
MAIL_ENABLED=true
```

Then configure SMTP values in `.env`.

## Repository

```text
https://github.com/Dinesh12328/WorkFlowPro
```
