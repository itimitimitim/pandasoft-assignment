# Leave Management API

REST API for employee leave requests and manager decisions. Requires Docker with Compose and Java 21 if running Maven locally.

## MVC structure

- `entity` — JPA model classes (`Employee`, `LeaveType`, `LeaveRequest`) with Lombok-generated getters, setters, and constructors.
- `dto` — request and response data objects such as login and leave request payloads.
- `controller` — REST controllers; `api` contains centralized API exception handling.
- `service` — business rules and transaction boundaries.
- `service.data` — entity-specific data access services (`EmployeeDataService`, `LeaveTypeDataService`, `LeaveRequestDataService`, `LeaveRequestLogDataService`, `IdempotencyDataService`).
- `service.IdempotencyService` — idempotency key validation, reservation, replay, and completion.
- `persistence` — Spring Data JPA repositories and database queries.
- `security` — JWT issuing, validation, and role-based authorization.
- `util` — shared date utilities for holiday parsing and working-day calculation.

Controllers only map HTTP input/output. Data services handle repository access. Validation, authorization, status transitions, balance checks, overlap checks, and working-day calculations stay in the business service layer.

## Run

```bash
docker compose up --build
```

The API is at `http://localhost:8080`; Swagger UI is at `/swagger-ui.html` and the OpenAPI JSON is at `/v3/api-docs`. PostgreSQL is exposed on port 5432. Login first, then send the returned JWT as `Authorization: Bearer <token>` on API requests. When a manager approves a request, the service publishes a JSON `LeaveApproved` event to Kafka topic `leave.approved` (configurable with `KAFKA_LEAVE_APPROVED_TOPIC`).

Demo credentials use password `password` for seeded employees:

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"employeeId":1,"password":"password"}'
```

## Endpoints

- `POST /api/v1/auth/login` — exchange employee ID and password for a JWT.
- `POST /api/v1/leave-requests` — create a request (JSON: `leaveType`, `startDate`, `endDate`, `reason`). Send a unique `Idempotency-Key` header; retries with the same key return the original request.
- `GET /api/v1/leave-requests` — list requests with `employeeId`, `status`, `from`, `to`, `page`, `size`, and `sort` parameters. Without `employeeId`, an employee sees their own requests and a manager sees requests from all direct reports.
- `GET /api/v1/leave-requests/{id}` — view an owned or direct-report request.
- `POST /api/v1/leave-requests/{id}/approve` — manager approval.
- `POST /api/v1/leave-requests/{id}/reject` — manager rejection, JSON `{"reason":"..."}`.
- `POST /api/v1/leave-requests/{id}/cancel` — owner cancellation before start.
- `GET /api/v1/employees/{id}/leave-balance?year=2026` — leave quotas, use, and remaining balances.

Errors share `timestamp`, `status`, `error`, and `message`; Bean Validation errors add `fieldErrors`. Conflicts use HTTP 409. JWT roles are derived from the employee record and enforced by Spring Security.

## Assumptions and design

- `from` and `to` list filters include requests intersecting the supplied range; omitted `employeeId` means the caller's own requests. A manager can query direct reports only.
- Annual quota is applied to the calendar year of `startDate`. If a request crosses New Year, its full number of working days is charged to its start year; balance display counts the working days that land within the requested balance year.
- Public holidays are configured as a comma-separated list in `LEAVE_HOLIDAYS_2026` (ISO dates). The defaults are the 2026 Thailand public holiday dates; replace with the authoritative company calendar before production.
- A range containing only weekends or configured holidays is invalid. Overlap is checked only for working days.
- Optimistic version locking ensures one concurrent manager decision commits; the losing update receives `409 INVALID_STATUS_TRANSITION`.
- Liquibase owns both the schema and seed data; JPA validates the schema. Employees and leave types are inserted through versioned changesets.

## Tests

Run `./mvnw test` (Windows: `mvnw.cmd test`). Unit tests cover working-day calculation and status transition rules. Testcontainers integration tests cover request creation, retrieval/listing, validation errors, and approval authorization. Docker must be available for integration tests.

## With more time

Add employee provisioning, model regional/company holiday calendars in the database, strengthen concurrent quota reservation with a per-employee/year lock, and add richer API contract tests and observability.
