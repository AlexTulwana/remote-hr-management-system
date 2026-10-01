# WorkFlowTech - Remote HR Management System

Full-stack HR platform for managing a distributed workforce - recruitment, onboarding, attendance, leave, payslips, performance reviews, and admin analytics, with role-based access (Employee / Manager / HR / Admin).

**Demo video:** https://youtu.be/UhOLkwa4hL0

## Verification code
WTC-KGNDGDSN

## Tech stack

- Backend: Java, Spring Boot 4, MySQL, JWT auth, RabbitMQ (async email/notifications)
- Frontend: React (Vite) + Tailwind CSS
- Analytics: Python (nightly turnover/headcount metrics job)
- Testing: JUnit + Mockito (service/controller), Testcontainers (integration), GitHub Actions CI

## Features

Employee: dashboard, leave requests, attendance clock-in/out, document uploads, payslips, performance reviews, messages, calendar, announcements, profile + photo

Manager: everything above, plus team view, leave approvals, team requests, team disciplinary cases, team performance reviews

HR / Admin: everything above, plus employee directory and org chart, recruitment (postings, applications, interviews), onboarding/offboarding, company-wide leave and payslip management, disciplinary cases and hearings, escalations, branches

Admin only: executive overview, turnover and headcount analytics

## Run it locally

### 1. Backend

cd backend
cp .env.example .env
edit .env with real values, then: source .env
make check-env
make run

Runs on http://localhost:8080

### 2. Frontend

cd frontend
npm install
npm run dev

Runs on http://localhost:5173 (or next free port)

### 3. Supporting services (MySQL + RabbitMQ)

Either run them yourself, or via Docker:

source backend/.env
docker-compose up mysql rabbitmq

(docker-compose up alone also builds and runs the backend container - the frontend is not containerized yet)

## Testing

cd backend
make test          full suite (unit, controller, integration)
make test-unit      service and controller tests only
make services        just service-layer tests
make controllers      just controller-layer tests

## Project structure

remote-hr-management-system/
  backend/       Spring Boot API
  frontend/      React + Vite app
  analytics/      Python nightly metrics job
  docker-compose.yml

## Roles and test accounts

See backend/.env.example for required environment variables. Test account credentials are not included here - see the demo video for a full walkthrough of each role.
