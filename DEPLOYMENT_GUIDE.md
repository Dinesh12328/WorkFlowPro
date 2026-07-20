# WorkFlowPro Setup and Deployment Guide

This guide explains how to run WorkFlowPro locally and how to deploy it on Render.

WorkFlowPro contains two parts in one Spring Boot application:

- Backend REST API built with Spring Boot.
- Frontend UI served from `src/main/resources/static`.

When the app is running, open the UI in a browser:

```text
http://localhost:8080/
```

For Render deployment, open the public `workflowpro-api` URL that Render creates.

## 1. Files used for setup

Important project files:

| File | Purpose |
|---|---|
| `pom.xml` | Maven dependencies and Java build configuration |
| `Dockerfile` | Builds the Spring Boot app into a Docker image |
| `compose.yaml` | Local Docker Compose setup for API + MySQL |
| `render.yaml` | Render Blueprint deployment file |
| `.env.example` | Example local Docker environment variables |
| `src/main/resources/application.yml` | Default local app configuration |
| `src/main/resources/application-mysql.yml` | MySQL profile configuration |

Important note:

```text
compose.yaml is for local Docker only.
render.yaml is for Render Blueprint deployment.
```

If Render asks for a Blueprint Path, use:

```text
render.yaml
```

Do not use:

```text
compose.yaml
```

## 2. Required tools

### For local IntelliJ run

Install:

- Java JDK 21 or newer
- IntelliJ IDEA
- Maven, or use IntelliJ's bundled Maven support

You do not need MySQL for the normal IntelliJ run. The default configuration uses H2 in-memory database.

### For local Docker run

Install:

- Docker Desktop
- Git

Docker will run:

- Spring Boot API/frontend container
- MySQL container

### For Render deployment

You need:

- GitHub account
- Render account
- Repository pushed to GitHub
- `render.yaml` in the repository root

The current repository is:

```text
https://github.com/Dinesh12328/WorkFlowPro
```

## 3. Run locally in IntelliJ IDEA

This is the easiest way to test the project while developing.

### Step 1: Open the project

1. Open IntelliJ IDEA.
2. Click **Open**.
3. Select the project folder:

```text
2-workflowpro-task-management-system-project
```

4. Make sure IntelliJ detects `pom.xml`.
5. Wait for Maven dependencies to finish loading.

### Step 2: Run the Spring Boot app

Open this file:

```text
src/main/java/com/workflowpro/WorkflowProApplication.java
```

Click the green run button beside the class or `main` method.

### Step 3: Open the frontend

After startup completes, open:

```text
http://localhost:8080/
```

Then:

1. Register a new user.
2. Create a project.
3. Create tasks.
4. Assign tasks.
5. Test comments, attachments, filters, notifications, and dashboard stats.

### Step 4: H2 database console

The default local run uses H2.

Open:

```text
http://localhost:8080/h2-console
```

Use:

```text
JDBC URL: jdbc:h2:mem:workflowpro-dev;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE
User: sa
Password:
```

Leave the password empty.

### If port 8080 is busy

Use another port.

In IntelliJ run configuration, add this VM option:

```text
-Dserver.port=8081
```

Then open:

```text
http://localhost:8081/
```

Or run from terminal:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

## 4. Run locally with Docker and MySQL

Use this when you want to test the app with MySQL like a production-style setup.

### Step 1: Create `.env`

Copy `.env.example` to `.env`.

Example values:

```text
MYSQL_DATABASE=workflowpro
MYSQL_USER=workflowpro
MYSQL_PASSWORD=workflowpro
MYSQL_ROOT_PASSWORD=root
JWT_SECRET=V29ya0Zsb3dQcm9EZXZlbG9wbWVudFNlY3JldEtleUZvckpXVFRva2Vucw==
MAIL_ENABLED=false
```

For real deployment, use stronger passwords and a strong JWT secret.

### Step 2: Start Docker

Run:

```bash
docker compose up --build
```

Docker starts:

- `mysql`
- `api`

The API waits for MySQL to become healthy before starting.

### Step 3: Open the app

Open:

```text
http://localhost:8080/
```

### Step 4: Stop Docker

Stop containers:

```bash
docker compose down
```

Stop containers and delete MySQL data:

```bash
docker compose down -v
```

Only use `-v` when you are okay deleting the local database.

## 5. Deploy on Render using Blueprint

Render deployment uses:

```text
render.yaml
```

The Blueprint creates:

| Render service | Type | Purpose |
|---|---|---|
| `workflowpro-api` | Web Service | Public Spring Boot API + frontend |
| `workflowpro-mysql` | Private Service | MySQL database |

The MySQL service uses a persistent disk mounted at:

```text
/var/lib/mysql
```

This mount path is important because the MySQL Docker image stores database files there.

### Cost note

The Blueprint uses a MySQL private service with a persistent disk. This may require paid Render resources.

If you only need a temporary demo, you can deploy the API alone using H2, but the data will not be reliable after restarts. For a real project demo, MySQL with persistent disk is better.

## 6. Render Blueprint steps

### Step 1: Push code to GitHub

Make sure these files are in GitHub on the `main` branch:

```text
Dockerfile
render.yaml
pom.xml
src/
```

### Step 2: Open Render

1. Go to Render dashboard.
2. Click **New**.
3. Select **Blueprint**.
4. Connect/select GitHub repository:

```text
Dinesh12328/WorkFlowPro
```

### Step 3: Enter Blueprint settings

Use:

```text
Blueprint Name: WorkFlowPro
Branch: main
Blueprint Path: render.yaml
```

If the Blueprint Path is optional, you can also leave it empty because `render.yaml` is in the repository root.

Do not enter:

```text
compose.yaml
```

### Step 4: Review services

Render should show two services:

```text
workflowpro-mysql
workflowpro-api
```

Render will generate secret values for:

```text
MYSQL_PASSWORD
MYSQL_ROOT_PASSWORD
JWT_SECRET
```

### Step 5: Apply Blueprint

Click **Apply** or **Deploy Blueprint**.

Wait for:

1. `workflowpro-mysql` to start.
2. `workflowpro-api` to build and deploy.

The first MySQL deploy can take a few minutes.

### Step 6: Open deployed app

When `workflowpro-api` is live, open its public URL.

It will look similar to:

```text
https://workflowpro-api.onrender.com/
```

Then register a user and start using the app.

## 7. Environment variables used on Render

The Blueprint sets these values:

| Variable | Value/source | Purpose |
|---|---|---|
| `PORT` | `8080` | Port Spring Boot runs on |
| `SPRING_PROFILES_ACTIVE` | `mysql` | Enables MySQL profile |
| `DB_HOST` | From `workflowpro-mysql` service | MySQL host |
| `DB_PORT` | `3306` | MySQL port |
| `DB_NAME` | `workflowpro` | Database name |
| `DB_USERNAME` | `workflowpro` | Database username |
| `DB_PASSWORD` | From MySQL generated password | Database password |
| `DDL_AUTO` | `update` | Allows Hibernate to update schema |
| `JWT_SECRET` | Generated by Render | JWT signing secret |
| `MAIL_ENABLED` | `false` | Disables email sending by default |
| `MAIL_HOST` | `smtp.gmail.com` | SMTP host if email is enabled later |
| `MAIL_PORT` | `587` | SMTP port |

## 8. Optional email setup

Email sending is disabled by default:

```text
MAIL_ENABLED=false
```

To enable email notifications on Render:

1. Open the `workflowpro-api` service.
2. Go to **Environment**.
3. Set:

```text
MAIL_ENABLED=true
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@example.com
MAIL_PASSWORD=your-app-password
```

For Gmail, use an app password, not your normal Gmail password.

After changing environment variables, redeploy the service.

## 9. Manual Render deployment alternative

Use this only if you do not want to use Blueprint.

### Step 1: Create MySQL private service

Create a Render private service using Docker image:

```text
docker.io/library/mysql:8.4
```

Set environment variables:

```text
MYSQL_DATABASE=workflowpro
MYSQL_USER=workflowpro
MYSQL_PASSWORD=<strong password>
MYSQL_ROOT_PASSWORD=<strong root password>
```

Add a persistent disk:

```text
Mount Path: /var/lib/mysql
Size: 10 GB or higher
```

Wait until MySQL is live.

### Step 2: Create API web service

Create a Render web service from the GitHub repository:

```text
Dinesh12328/WorkFlowPro
```

Use:

```text
Runtime: Docker
Branch: main
Dockerfile Path: ./Dockerfile
Health Check Path: /
```

Set environment variables:

```text
PORT=8080
SPRING_PROFILES_ACTIVE=mysql
DB_HOST=<your Render MySQL internal host>
DB_PORT=3306
DB_NAME=workflowpro
DB_USERNAME=workflowpro
DB_PASSWORD=<same MYSQL_PASSWORD>
DDL_AUTO=update
JWT_SECRET=<strong base64 or long random secret>
MAIL_ENABLED=false
```

The MySQL internal host should not include `jdbc:mysql://` and should not include `/workflowpro`.

If Render displays an internal address like this:

```text
workflowpro-mysql:3306
```

Split it into:

```text
DB_HOST=workflowpro-mysql
DB_PORT=3306
```

The application builds the JDBC URL from those variables.

## 10. After deployment testing checklist

After the app is live, test these items:

1. Open the deployed URL.
2. Register a new user.
3. Login with that user.
4. Create a project.
5. Create a task inside the project.
6. Change task status from `TODO` to `IN_PROGRESS`.
7. Complete the task.
8. Add a comment.
9. Add an attachment link.
10. Check dashboard stats.
11. Create another user.
12. Add the user as a project member.
13. Assign a task to that user.
14. Check notifications.
15. Test filters by status, priority, due date, project, and assignee.

## 11. Common problems and fixes

### Render says Blueprint file has an issue

Most common cause:

```text
Blueprint Path was set to compose.yaml
```

Fix:

```text
Blueprint Path: render.yaml
```

Then retry the Blueprint.

### Render cannot find the Blueprint file

Check:

- Branch is `main`.
- File name is exactly `render.yaml`.
- File is in the repository root.
- Latest code is pushed to GitHub.

### API deploy fails because of port

Render web services need the app to bind to the configured port.

This project supports:

```text
PORT=8080
```

The application reads it through:

```text
server.port=${PORT:8080}
```

### API cannot connect to MySQL

Check:

- `workflowpro-mysql` is live.
- MySQL and API are in the same Render region.
- `DB_HOST` points to the private MySQL service host.
- `DB_PORT` is `3306`.
- `DB_PASSWORD` matches `MYSQL_PASSWORD`.
- The API has `SPRING_PROFILES_ACTIVE=mysql`.

### MySQL data disappears

Check the disk mount path.

It must be:

```text
/var/lib/mysql
```

If the mount path is different, MySQL data may not persist correctly.

### Browser shows access denied

Open the frontend:

```text
/
```

Do not open protected API routes directly before login.

Protected routes such as:

```text
/api/projects
```

need a JWT token, which the frontend receives after login.

### App is slow after no activity

If the Render service is on a free or low-cost plan, it may sleep or take time to wake up. Wait for the service to become active, then refresh.

### Docker local run says port is already used

Another app may already use `8080` or `3306`.

Stop the other app, or change the port mapping in `compose.yaml`.

## 12. Recommended deployment path

For development:

```text
IntelliJ IDEA + H2
```

For local full-stack testing:

```text
Docker Compose + MySQL
```

For hosted demo or production-style deployment:

```text
Render Blueprint + MySQL private service
```

Use this order when checking the project:

1. Run locally in IntelliJ.
2. Run tests.
3. Run with Docker if MySQL behavior needs checking.
4. Push to GitHub.
5. Deploy on Render using `render.yaml`.
