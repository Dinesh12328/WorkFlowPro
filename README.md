# WorkFlowPro

WorkFlowPro is a full-stack task and project management application for organizing project work, assigning tasks, tracking progress, and managing team activity from one interactive workspace.

The application combines a Spring Boot REST API with a browser-based frontend. Users can create projects, add members, create and assign tasks, update task status, add comments, attach useful links, receive assignment notifications, and monitor project progress from the dashboard.

## Project summary

| Area | Details |
|---|---|
| Application type | Task and project management system |
| Backend | Java, Spring Boot, Spring Data JPA |
| Frontend | HTML, CSS, JavaScript single-page interface |
| Security | Spring Security with JWT authentication |
| Databases | H2 for local development, MySQL for Docker/Render |
| Deployment | Docker, Docker Compose, Render Blueprint |

Detailed setup and deployment instructions are available in [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md).

## Core features

- User registration and login
- JWT-based protected API access
- Project creation, update, listing, and deletion
- Project member management
- Task creation inside projects
- Task assignment to project owners or members
- Task priority levels: `LOW`, `MEDIUM`, `HIGH`
- Task status workflow: `TODO`, `IN_PROGRESS`, `COMPLETED`
- Task filtering by status, priority, due date, project, and assignee
- Kanban-style task board
- Task detail drawer for editing, comments, and attachments
- Comment support for task discussions
- Attachment-link support for related files or references
- Assignment notifications
- Dashboard statistics for projects, tasks, deadlines, and unread notifications

## Application workflow

1. A user registers or logs in.
2. The user creates a project.
3. Project members are added when collaboration is needed.
4. Tasks are created inside the project.
5. Tasks are assigned to the project owner or project members.
6. Tasks move through `TODO`, `IN_PROGRESS`, and `COMPLETED`.
7. Users add comments or attachment links to task records.
8. Assignment notifications appear for assigned users.
9. The dashboard summarizes project and task activity.

## Architecture overview

```text
Browser UI
   |
   | HTTP / JSON
   v
Spring Boot REST API
   |
   | Spring Data JPA
   v
H2 or MySQL Database
```

### Frontend

The frontend is served directly from the Spring Boot application.

Frontend files:

```text
src/main/resources/static/index.html
src/main/resources/static/styles.css
src/main/resources/static/app.js
```

Main frontend screens:

- Authentication page
- Dashboard
- Task board
- Project management view
- Team/user view
- Notifications view
- Task detail drawer
- Project and task forms

### Backend

The backend exposes REST APIs for authentication, users, projects, tasks, comments, attachments, notifications, and dashboard statistics.

Main backend packages:

```text
controller
service
repository
entity
dto
security
config
exception
```

## Domain model

Main entities:

- `User`
- `Project`
- `Task`
- `Comment`
- `Attachment`
- `Notification`

Important relationships:

- A user can own many projects.
- A project can have many members.
- A project can have many tasks.
- A task belongs to one project.
- A task can be assigned to one user.
- A task can have many comments and attachments.
- A notification belongs to one recipient.

## Access rules

- Public users can access the frontend, registration, and login.
- Authenticated users can access protected API routes with a JWT token.
- A user can view projects they own or belong to.
- Only the project owner can update or delete a project.
- A task can only be assigned to the project owner or a project member.
- Comments and attachments follow task/project access rules.
- Notifications are visible only to the notification recipient.

Protected API routes require:

```http
Authorization: Bearer <token>
```

## API overview

### Authentication

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/auth/register` | Create a user account |
| `POST` | `/api/auth/login` | Login and receive JWT token |

### Users

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/users/me` | Get current user profile |
| `GET` | `/api/users` | List/search users |

### Projects

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/projects` | Create project |
| `GET` | `/api/projects` | List accessible projects |
| `GET` | `/api/projects/{id}` | Get project details |
| `PUT` | `/api/projects/{id}` | Update project |
| `DELETE` | `/api/projects/{id}` | Delete project |

### Tasks

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/tasks` | Create task |
| `GET` | `/api/tasks` | List/filter tasks |
| `GET` | `/api/tasks/{id}` | Get task details |
| `PUT` | `/api/tasks/{id}` | Update task |
| `DELETE` | `/api/tasks/{id}` | Delete task |
| `GET` | `/api/tasks/status/{status}` | List tasks by status |
| `GET` | `/api/tasks/priority/{priority}` | List tasks by priority |

Supported task filter parameters:

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

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/tasks/{taskId}/comments` | Add task comment |
| `GET` | `/api/tasks/{taskId}/comments` | List task comments |

### Attachments

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/tasks/{taskId}/attachments` | Add attachment link |
| `GET` | `/api/tasks/{taskId}/attachments` | List attachment links |

### Notifications

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/notifications` | List notifications |
| `PATCH` | `/api/notifications/{id}/read` | Mark notification as read |

### Dashboard

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/dashboard/stats` | Get dashboard statistics |

## Sample request payloads

### Register

```json
{
  "name": "Alex Morgan",
  "email": "alex@example.com",
  "password": "password123"
}
```

### Create project

```json
{
  "name": "Website Relaunch",
  "description": "Plan, assign, and track all work for the website relaunch.",
  "memberIds": [2, 3]
}
```

### Create task

```json
{
  "title": "Prepare launch checklist",
  "description": "Create the final checklist for launch readiness.",
  "priority": "HIGH",
  "status": "TODO",
  "dueDate": "2026-07-30",
  "projectId": 1,
  "assigneeId": 2
}
```

## Run locally with IntelliJ IDEA

The default local run uses H2, so MySQL is not required.

1. Open the project in IntelliJ IDEA.
2. Wait for Maven dependencies to load.
3. Open:

```text
src/main/java/com/workflowpro/WorkflowProApplication.java
```

4. Run the application.
5. Open:

```text
http://localhost:8080/
```

H2 console:

```text
http://localhost:8080/h2-console
```

H2 connection:

```text
JDBC URL: jdbc:h2:mem:workflowpro-dev;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE
User: sa
Password:
```

If port `8080` is busy, run with another port:

```text
-Dserver.port=8081
```

Then open:

```text
http://localhost:8081/
```

## Run locally with Docker

Use Docker when testing the full application with MySQL.

1. Create a `.env` file from `.env.example`.
2. Start the services:

```bash
docker compose up --build
```

3. Open:

```text
http://localhost:8080/
```

Stop containers:

```bash
docker compose down
```

Stop containers and remove local MySQL data:

```bash
docker compose down -v
```

## Deploy on Render

Render deployment uses the Blueprint file:

```text
render.yaml
```

Do not use `compose.yaml` for Render. `compose.yaml` is only for local Docker Compose.

Render creates:

- `workflowpro-api` - public Spring Boot web service.
- `workflowpro-mysql` - private MySQL service with persistent disk.

Render Blueprint settings:

```text
Blueprint Name: WorkFlowPro
Branch: main
Blueprint Path: render.yaml
```

After deployment, open the public URL for `workflowpro-api`, register a user, and begin using the workspace.

For complete Render deployment instructions, see [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md).

## Configuration

Common environment variables:

| Variable | Description |
|---|---|
| `PORT` | HTTP port used by Spring Boot |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile, for example `mysql` |
| `MYSQL_DATABASE` | MySQL database name |
| `MYSQL_USER` | MySQL username |
| `MYSQL_PASSWORD` | MySQL password |
| `MYSQL_ROOT_PASSWORD` | MySQL root password |
| `DB_HOST` | Database host |
| `DB_PORT` | Database port |
| `DB_NAME` | Database name |
| `DB_URL` | Full Spring datasource URL override |
| `DB_USERNAME` | Spring datasource username |
| `DB_PASSWORD` | Spring datasource password |
| `DDL_AUTO` | Hibernate schema mode |
| `JWT_SECRET` | JWT signing secret |
| `JWT_EXPIRATION` | JWT expiration time |
| `MAIL_ENABLED` | Enables/disables email notification |
| `MAIL_HOST` | SMTP host |
| `MAIL_PORT` | SMTP port |
| `MAIL_USERNAME` | SMTP username |
| `MAIL_PASSWORD` | SMTP password |

## Run tests

Run:

```bash
mvn test
```

The integration tests cover:

- Public frontend assets
- Protected route security
- Registration and login
- Duplicate account handling
- User search
- Project create/list/get/update/delete
- Task create/list/filter/get/update/delete
- Task status and priority filtering
- Comments
- Attachments
- Notifications
- Dashboard statistics
- Project access rules
- Task assignment validation

## Troubleshooting

### Localhost shows access denied

Open the frontend URL:

```text
http://localhost:8080/
```

Protected API URLs such as `/api/projects` require a JWT token and should be accessed after login.

### Application does not start in IntelliJ

The default run uses H2. If the application tries to connect to MySQL unexpectedly, remove this VM option from the run configuration:

```text
-Dspring.profiles.active=mysql
```

### Docker port is already used

Ports `8080` or `3306` may already be in use. Stop the other service or change the port mapping in `compose.yaml`.

### Render Blueprint fails

Check that Render is using:

```text
Blueprint Path: render.yaml
```

Do not point Render to:

```text
compose.yaml
```

### Data is empty after login

Project data is user-specific. A user sees projects they own or projects where they are added as a member.

### Emails are not sent

Email sending is disabled by default:

```text
MAIL_ENABLED=false
```

Set SMTP environment variables and change `MAIL_ENABLED` to `true` when email delivery is required.

## Repository

```text
https://github.com/Dinesh12328/WorkFlowPro
```
