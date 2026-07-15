# WorkFlowPro Task Management System

WorkFlowPro is a Spring Boot backend for task and project management. It supports user authentication, projects, assignable tasks, comments, attachment metadata, notifications, and dashboard statistics.

## Features

- User registration and login with JWT authentication
- Project creation, listing, updating, and deletion
- Project members and owner-based access control
- Task creation, assignment, status tracking, priority tracking, due dates, and filtering
- Task comments
- Attachment metadata for task files
- In-app notification records plus optional email notifications for assigned tasks
- Dashboard totals for projects, tasks, assigned work, overdue work, upcoming deadlines, unread notifications, status counts, and priority counts
- Interactive frontend with login/register, sidebar navigation, dashboard cards, Kanban task board, drag-and-drop status updates, task detail drawer, project/team views, comments, attachment links, notifications, and auto-refresh
- Docker setup with MySQL

## Tech stack

- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA
- MySQL
- JWT
- Java Mail Sender
- Docker

## Run locally with Docker

Create a `.env` file from `.env.example`, then start the app:

```bash
docker compose up --build
```

The API will be available at:

```text
http://localhost:8080
```

The interactive frontend is served by the same Spring Boot app:

```text
http://localhost:8080/
```

It uses the backend APIs directly and stores the JWT in browser local storage after login.

Frontend highlights:

- Dashboard overview with live project/task/notification stats
- Kanban task board with drag-and-drop status changes
- Modal forms for creating projects and tasks
- Task detail drawer for editing tasks, comments, and attachment links
- Project, team, notification, search, and filter views
- Auto-refresh with visible sync status

## Run tests

```bash
mvn test
```

Tests use an in-memory H2 database and disabled email sending.

## Run from IntelliJ IDEA without MySQL

For quick local testing in IntelliJ, use the `dev` profile. This uses an in-memory H2 database, so you can press Run without starting MySQL.

In IntelliJ:

1. Open `WorkflowProApplication.java`.
2. Open the run configuration.
3. Add this VM option:

```text
-Dspring.profiles.active=dev
```

4. Run the app.
5. Open:

```text
http://localhost:8080/
```

If you open API URLs like `/api/projects` directly in the browser before login, Spring Security will deny access. Open `/`, register/login, then use the UI.

## Authentication

Register or log in first:

```http
POST /api/auth/register
POST /api/auth/login
```

Use the returned JWT on protected routes:

```http
Authorization: Bearer <token>
```

## Main API examples

```http
POST /api/projects
GET /api/projects
GET /api/projects/{id}

POST /api/tasks
PUT /api/tasks/{id}
GET /api/tasks?status=TODO&priority=HIGH
GET /api/tasks/status/TODO
GET /api/tasks/priority/HIGH

POST /api/tasks/{id}/comments
GET /api/tasks/{id}/comments

GET /api/dashboard/stats
GET /api/notifications
```

## Core entities

- User
- Project
- Task
- Comment
- Notification
- Attachment

## Configuration

Important environment variables:

```text
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
APP_JWT_SECRET
APP_MAIL_ENABLED
SPRING_MAIL_HOST
SPRING_MAIL_PORT
SPRING_MAIL_USERNAME
SPRING_MAIL_PASSWORD
```
