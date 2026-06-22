# Admin API Refactor Plan

## Context

`AdminController` and `AdminService` currently handle employee management, account lifecycle controls,
transaction reversal, and audit log access all in one place. That violates separation of concerns and
does not follow REST resource ownership.

This refactor moves account and transaction behavior to the controllers and services that own those
resources. Admin authorization and Swagger documentation are preserved at every moved endpoint.

---

## Strict Implementation Rules

These rules apply to every file touched during this refactor.

### Research First
- Before touching any file, read all affected controllers, services, DTOs, entities, enums, repositories, and tests.
- Search the codebase before introducing any new identifier.
- Do not invent names, enum values, DTO fields, repository method names, or entity properties.

### Code Fidelity
- Compare logic line-by-line with the original service method before moving it.
- Do not optimize, simplify, or restructure logic during the move.
- Preserve existing business logic exactly.
- Preserve audit logging behavior exactly.
- Preserve existing exception types and error responses exactly.
- Preserve existing security behavior; only relocate `@PreAuthorize` annotations to the new endpoints.

### Contract Preservation
- Do not change existing DTOs, request bodies, response bodies, or JSON field names.
- Do not modify database entities or repository interfaces unless compilation proves it is required.
- Never create new enums, DTO fields, repository methods, or entity properties unless compilation proves they are required.
- If a required method does not exist in the codebase, stop and report it rather than guessing an implementation.

### Commenting Convention
- Do not delete moved or replaced code. Comment it out.
- Add `// --Efe(Admin)` on the line immediately above every commented-out block and above every new block added as part of this refactor.

---

## Endpoint Ownership After Refactor

### Keep in `AdminController` — `/admin`
| Method | Path | Description |
|--------|------|-------------|
| GET | `/admin/ping` | Health check |
| POST | `/admin/employees` | Create employee |
| GET | `/admin/employees` | List all employees |
| GET | `/admin/employees/{id}` | Get employee by ID |
| PUT | `/admin/employees/{id}` | Update employee |
| PATCH | `/admin/employees/{id}/status` | Enable or disable employee |
| DELETE | `/admin/employees/{id}` | Soft delete employee |

### Move to `AccountController` — `/accounts`
| Method | Path | Auth | Was |
|--------|------|------|-----|
| PATCH | `/accounts/{iban}/freeze` | `ADMIN` | `/admin/accounts/{iban}/freeze` |
| PATCH | `/accounts/{iban}/unfreeze` | `ADMIN` | `/admin/accounts/{iban}/unfreeze` |
| PATCH | `/accounts/{iban}/close` | `ADMIN` | `/admin/accounts/{iban}/close` |

### Move to `TransactionController` — `/transactions`
| Method | Path | Auth | Was |
|--------|------|------|-----|
| POST | `/transactions/{id}/reverse` | `ADMIN` | `/admin/transactions/{id}/reverse` |

### New `AuditLogController` — `/audit-logs`
| Method | Path | Auth | Was |
|--------|------|------|-----|
| GET | `/audit-logs` | `ADMIN` | `/admin/audit-logs` |

Uses the same `AuditLogResponse` DTO and JSON fields as the current admin endpoint.

---

## Service Ownership After Refactor

### `AdminService` — employee administration only
Keep:
- `createEmployee`
- `getAllEmployees`
- `getEmployee`
- `updateEmployee`
- `setEmployeeStatus`
- `deleteEmployee`

Comment out (with `// --Efe(Admin)` marker): all account, transaction, and audit methods.

### `AccountService` — add account lifecycle methods
Move from `AdminService` without any logic change:
- `freezeAccount(String iban)` — validates not closed, not already frozen; calls `markFrozen()`; saves; records audit log
- `unfreezeAccount(String iban)` — validates is frozen; calls `unfreeze()`; saves; records audit log
- `closeAccount(String iban)` — validates not already closed; calls `markClosed()`; saves; records audit log

`AuditService` is already injected-by-dependency in the project; inject it into `AccountService` the same way.

### `TransactionService` — add reversal method
Move from `AdminService` without any logic change:
- `reverseTransaction(Long id, String adminUsername)` — finds original transaction; blocks reversing a `REVERSAL`; blocks double reversal; restores balances per type (`TRANSFER`, `DEPOSIT`, `WITHDRAWAL`); saves reversal `Transaction` record; records audit log; returns `TransactionReversalResponse`

### `AuditService` — add audit log query method
Add to `AuditService`:
- `getAuditLogs()` — queries `AuditLogRepository.findAll()`, maps to `AuditLogResponse` list

The mapping logic is moved from `AdminService.toAuditLogResponse` without any change.

---

## Swagger Documentation

Every moved endpoint must carry an `@Operation` that explicitly states it requires `ADMIN` role.

Examples:
- `"Admin operation: freezes a bank account, blocking all outgoing transfers. Requires ADMIN role."`
- `"Admin operation: removes the freeze from a bank account. Requires ADMIN role."`
- `"Admin operation: permanently closes a bank account. Requires ADMIN role."`
- `"Admin operation: reverses a completed transaction and restores balances. Requires ADMIN role."`
- `"Admin operation: retrieves the immutable system audit log. Requires ADMIN role."`

---

## Implementation Order

1. **Move account operations** — add `freezeAccount`, `unfreezeAccount`, `closeAccount` to `AccountService`; add three `PATCH` endpoints to `AccountController`; comment out originals in `AdminService` and `AdminController`.
2. **Move transaction reversal** — add `reverseTransaction` to `TransactionService`; add `POST /transactions/{id}/reverse` to `TransactionController`; comment out originals in `AdminService` and `AdminController`.
3. **Move audit log querying** — add `getAuditLogs` to `AuditService`; create `AuditLogController` with `GET /audit-logs`; comment out originals in `AdminService` and `AdminController`.
4. **Slim down `AdminService` and `AdminController`** — verify only employee administration remains; confirm dead code is commented with `// --Efe(Admin)`.
5. **Update existing controller tests** — `AdminControllerTest`, `AccountControllerTest`, `TransactionControllerTest`.
6. **Update existing service tests** — `AdminServiceTest`, `AccountServiceTest`, `TransactionServiceTest`.
7. **Add new unit tests** — cover `freezeAccount`, `unfreezeAccount`, `closeAccount` in `AccountServiceTest`; cover `reverseTransaction` in `TransactionServiceTest`; cover `getAuditLogs` in an audit service test.
8. **Add new functional tests** — HTTP routing, authorization, response body, and error response for all three moved endpoint groups and the new audit log endpoint.

---

## Verification

After all changes, run:

```bash
./mvnw test
```

At minimum, run the directly affected test classes:

- `AdminControllerTest`
- `AccountControllerTest`
- `TransactionControllerTest`
- `AuditLogControllerTest`
- `AdminServiceTest`
- `AccountServiceTest`
- `TransactionServiceTest`
