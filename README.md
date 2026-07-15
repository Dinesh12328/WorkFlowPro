# WorkFlowPro Task Management System

WorkFlowPro is a full-stack task and project management application built with Spring Boot. It allows users to create projects, add project members, create and assign tasks, track task status, manage priorities and due dates, add comments, save attachment links, receive notifications, and view dashboard statistics from an interactive web interface.

The application includes both:

- A secured REST API backend.
- An interactive frontend served directly from the Spring Boot app.

Open the app in the browser after running it:

```text
http://localhost:8080/
```

## Project overview

WorkFlowPro is designed for teams that need a simple project workspace. A user can register, log in, create projects, add members, create tasks inside projects, assign tasks to project users, update progress, add comments, attach useful file links, and monitor work from a dashboard.

The backend uses JWT authentication, Spring Security, Spring Data JPA, MySQL, and optional email notifications. The frontend uses plain HTML, CSS, and JavaScript, so no separate Node/React setup is required.

## Main features

### Authentication

- User registration
- User login
- JWT token generation
- Protected API routes
- Current user endpoint
- Clean unauthorized/error responses

### Project management

- Create projects
- View all accessible projects
- View project details
- Update project name, description, and members
- Delete projects
- Owner/member based access control

### Task management

- Create tasks inside projects
- Assign tasks to project owner or project members
- Set priority: `LOW`, `MEDIUM`, `HIGH`
- Set status: `TODO`, `IN_PROGRESS`, `COMPLETED`
- Set and clear due dates
- Edit task title, description, priority, status, due date, and assignee
- Delete tasks
- Filter tasks by status, priority, project, assignee, and due date

### Collaboration

- Add comments on tasks
- Add attachment metadata/links on tasks
- View project team members
- Receive in-app notifications when tasks are assigned
- Optional email notification support using Java Mail Sender

### Dashboard

- Total projects
- Total tasks
- Tasks assigned to current user
- Overdue tasks
- Tasks due in the next seven days
- Unread notifications
- Task counts by status
- Task counts by priority

## Interactive frontend

The frontend is available at:

```text
http://localhost:8080/
```

It connects directly to the backend APIs and keeps the login token in browser local storage after login.

Frontend screens and interactions:

- Login/register page
- Sidebar navigation
- Dashboard overview
- Kanban task board
- Drag-and-drop task status updates
- Project creation modal
- Task creation modal
- Task detail drawer
- Task editing from the drawer
- Comments inside task details
- Attachment links inside task details
- Project list view
- Team/user list view
- Notification list view
- Mark notification as read
- Search and filter tasks
- Auto-refresh with visible sync status
- Responsive layout for smaller screens

## Tech stack

### Backend

- Java 21
- Spring Boot
- Spring Web
- Spring Security
- Spring Data JPA
- Jakarta Validation
- JWT using JJWT
- Java Mail Sender
- MySQL
- H2 for dev/testing

### Frontend

- HTML
- CSS
- Vanilla JavaScript
- Fetch API
- Browser local storage for JWT

### DevOps/testing

- Maven
- Docker
- Docker Compose
- JUnit/Spring Boot integration tests

## Project structure

```text
src/main/java/com/workflowpro
  config/          Security configuration
  controller/      REST API controllers
  dto/             Request and response DTOs
  entity/          JPA entities and enums
  exception/       Global error handling
  repository/      Spring Data JPA repositories
  security/        JWT and authentication classes
  service/         Business logic

src/main/resources
  application.yml      Main MySQL configuration
  application-dev.yml  IntelliJ/local H2 configuration
  static/              Interactive frontend

src/test
  Integration tests using H2
```

## Core entities

- `User`
- `Project`
- `Task`
- `Comment`
- `Notification`
- `Attachment`

## API overview

All routes except authentication and frontend static files require:

```http
Authorization: Bearer <token>
```

### Authentication APIs

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/auth/register` | Register a new user |
| `POST` | `/api/auth/login` | Login and receive JWT |

### User APIs

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/users/me` | Get current logged-in user |
| `GET` | `/api/users?query=` | Search/list users |

### Project APIs

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/projects` | Create project |
| `GET` | `/api/projects` | List accessible projects |
| `GET` | `/api/projects/{id}` | Get project details |
| `PUT` | `/api/projects/{id}` | Update project |
| `DELETE` | `/api/projects/{id}` | Delete project |

### Task APIs

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/tasks` | Create task |
| `GET` | `/api/tasks` | Filter/list tasks |
| `GET` | `/api/tasks/{id}` | Get task details |
| `PUT` | `/api/tasks/{id}` | Update task |
| `DELETE` | `/api/tasks/{id}` | Delete task |
| `GET` | `/api/tasks/status/{status}` | List tasks by status |
| `GET` | `/api/tasks/priority/{priority}` | List tasks by priority |

Task filter query parameters:

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

### Comment APIs

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/tasks/{taskId}/comments` | Add comment to task |
| `GET` | `/api/tasks/{taskId}/comments` | List task comments |

### Attachment APIs

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/tasks/{taskId}/attachments` | Add attachment metadata/link |
| `GET` | `/api/tasks/{taskId}/attachments` | List task attachment links |

### Notification APIs

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/notifications` | List notifications |
| `PATCH` | `/api/notifications/{id}/read` | Mark notification as read |

### Dashboard APIs

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/dashboard/stats` | Get dashboard statistics |

## Example API payloads

### Register

```json
{
  "name": "Dinesh",
  "email": "dinesh@example.com",
  "password": "password123"
}
```

### Create project

```json
{
  "name": "Website Relaunch",
  "description": "Public website refresh",
  "memberIds": [2, 3]
}
```

### Create task

```json
{
  "title": "Create launch checklist",
  "description": "Prepare all release tasks",
  "priority": "HIGH",
  "status": "TODO",
  "dueDate": "2026-07-30",
  "projectId": 1,
  "assigneeId": 2
}
```

### Update task

```json
{
  "title": "Create final launch checklist",
  "priority": "MEDIUM",
  "status": "IN_PROGRESS",
  "dueDate": "2026-08-01",
  "assigneeId": 2
}
```

## Run locally with Docker

Docker is the easiest way to run the full app with MySQL.

1. Create a `.env` file from `.env.example`.

2. Start the application:

```bash
docker compose up --build
```

3. Open the frontend:

```text
http://localhost:8080/
```

4. Register a user and start creating projects/tasks.

Stop the containers:

```bash
docker compose down
```

Stop and remove database volume:

```bash
docker compose down -v
```

## Run from IntelliJ IDEA without MySQL

For quick development in IntelliJ, use the `dev` profile. This profile uses an in-memory H2 database, so MySQL is not required.

Steps:

1. Open IntelliJ IDEA.
2. Click `Open`.
3. Select the project folder.
4. Wait for Maven indexing/import.
5. Open:

```text
src/main/java/com/workflowpro/WorkflowProApplication.java
```

6. Open the Run Configuration.
7. Add this VM option:

```text
-Dspring.profiles.active=dev
```

8. Run the application.
9. Open:

```text
http://localhost:8080/
```

The H2 console is available in dev mode:

```text
http://localhost:8080/h2-console
```

Use:

```text
JDBC URL: jdbc:h2:mem:workflowpro-dev
User: sa
Password:
```

## Run with Maven and local MySQL

If MySQL is already installed/running locally:

```bash
mvn spring-boot:run
```

Default database values:

```text
URL: jdbc:mysql://localhost:3306/workflowpro
Username: workflowpro
Password: workflowpro
```

You can override them using environment variables.

## Run tests

```bash
mvn test
```

The test suite uses H2 and disabled mail sending.

Current integration test coverage includes:

- Frontend public files
- Protected API security
- Register/login/duplicate account/bad credentials
- Current user endpoint
- User search endpoint
- Project create/list/get/update/delete
- Task create/get/update/delete
- Task status/priority/filter endpoints
- Comment create/list
- Attachment create/list
- Notification list/mark-read
- Dashboard stats
- Access control and invalid assignment checks

## Environment variables

Main variables used by Docker and Spring Boot:

| Variable | Purpose | Default |
|---|---|---|
| `MYSQL_DATABASE` | MySQL database name | `workflowpro` |
| `MYSQL_USER` | MySQL username | `workflowpro` |
| `MYSQL_PASSWORD` | MySQL password | `workflowpro` |
| `MYSQL_ROOT_PASSWORD` | MySQL root password | `root` |
| `DB_URL` | Spring datasource URL | Local MySQL URL |
| `DB_USERNAME` | Spring datasource username | `workflowpro` |
| `DB_PASSWORD` | Spring datasource password | `workflowpro` |
| `JWT_SECRET` | JWT signing secret | Development secret |
| `JWT_EXPIRATION` | JWT expiry in milliseconds | `86400000` |
| `MAIL_ENABLED` | Enable assignment emails | `false` |
| `MAIL_HOST` | SMTP host | `smtp.gmail.com` |
| `MAIL_PORT` | SMTP port | `587` |
| `MAIL_USERNAME` | SMTP username | Empty |
| `MAIL_PASSWORD` | SMTP password | Empty |

## Security notes

- API routes are protected with JWT.
- Frontend files and `/api/auth/**` are public.
- Password hashes use BCrypt.
- Users can only view projects where they are the owner or member.
- Only project owners can manage projects.
- Task assignees must be the project owner or a project member.
- For production, replace the development JWT secret with a strong private value.

## Troubleshooting

### Browser says localhost is denied

Make sure the Spring Boot app is running and open:

```text
http://localhost:8080/
```

Do not open protected API URLs directly before login, such as:

```text
http://localhost:8080/api/projects
```

Those require JWT authentication.

### App fails in IntelliJ because MySQL is not running

Use the dev profile:

```text
-Dspring.profiles.active=dev
```

Then run the app again.

### Docker port already in use

Another app may already be using port `8080` or `3306`.

Stop the other app or change the port mapping in `compose.yaml`.

### Login works but data looks empty

Data is user/project access based. A user only sees projects they own or where they are a member.

### Email notifications are not sending

Email is disabled by default:

```text
MAIL_ENABLED=false
```

Set SMTP values and enable mail:

```text
MAIL_ENABLED=true
MAIL_USERNAME=your-email
MAIL_PASSWORD=your-app-password
```

## Repository

GitHub:

```text
https://github.com/Dinesh12328/WorkFlowPro
```

## Summary

WorkFlowPro is a complete task management system with a secure Spring Boot backend, MySQL persistence, JWT authentication, dashboard statistics, project/task collaboration features, and a browser-based interactive frontend.
