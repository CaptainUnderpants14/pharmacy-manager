# Pharmacy Management System API

Spring Boot 3 / Java 21 backend for pharmacy master data, batch inventory, purchases, JWT authentication, permissions, audit records, and dashboard metrics.

## Setup

Requirements: Java 21+, PostgreSQL 14+, and Maven (the wrapper is included). Create a database named `pharma_db`, then set these environment variables before starting:

```powershell
$env:DB_PASSWORD = "your-postgres-password"
$env:JWT_SECRET = "a-long-random-secret-of-at-least-32-characters"
$env:INITIAL_ADMIN_USERNAME = "admin"
$env:INITIAL_ADMIN_PASSWORD = "choose-a-strong-password"
$env:INITIAL_ADMIN_EMAIL = "admin@example.com"
.\mvnw.cmd spring-boot:run
```

Optional settings: `DB_URL`, `DB_USERNAME`, `JWT_EXPIRATION` (seconds), and `CORS_ALLOWED_ORIGINS`. Flyway automatically applies `V1__pharmacy_core.sql`; Hibernate validates the resulting schema.

## Authentication

`POST /api/auth/login` with `{"username":"admin","password":"..."}` returns a Bearer JWT. Send it as `Authorization: Bearer <token>` for protected endpoints. Logout is currently client-side token disposal, ready to be extended with token revocation or refresh tokens.

The first-run owner is created only when both `INITIAL_ADMIN_USERNAME` and `INITIAL_ADMIN_PASSWORD` are supplied. Default roles are OWNER, ADMIN, PHARMACIST, CASHIER, INVENTORY_MANAGER, ACCOUNTANT, and STAFF. Permissions are seeded at startup; OWNER receives all initial permissions.

## API documentation

With the service running, visit `/swagger-ui.html`. Main paths are `/api/auth`, `/api/employees`, `/api/medicines`, `/api/categories`, `/api/manufacturers`, `/api/suppliers`, `/api/inventory`, `/api/purchases`, `/api/dashboard`, and `/api/audit-logs`.

Purchase receiving is transactional: it creates batches, creates PURCHASE movements, and updates batch stock together. Stock adjustments are transactionally recorded and cannot make inventory negative. FEFO selection is provided at `GET /api/inventory/fefo/{medicineId}`.
