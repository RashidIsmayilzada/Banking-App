# Admin Refactor — Audit Log

Prepared by: Efe (Admin)
Date: 2026-06-22
Purpose: Full record of every file created, modified, and commented out during the admin separation-of-concerns refactor.

---

## Files Created (New)

### 1. `backend/src/main/java/com/inholland/banking_app/controllers/AuditLogController.java`
New REST controller. Exposes `GET /audit-logs` restricted to `ADMIN` role.

```
Line 1   // --Efe(Admin)
Line 10  @RestController
Line 11  @RequestMapping("/audit-logs")
Line 12  @PreAuthorize("hasRole('ADMIN')")
Line 13  @RequiredArgsConstructor
Line 14  @Tag(name = "Admin Audit Logs", ...)
Line 20  private final AuditService auditService;
Line 23  // --Efe(Admin)
Line 24  @Operation(summary = "Get Audit Logs", description = "Admin operation: ... Requires ADMIN role.")
Line 26  @GetMapping
Line 27  public ResponseEntity<List<AuditLogResponse>> getAuditLogs() {
Line 28      return ResponseEntity.ok(auditService.getAuditLogs());
```

---

### 2. `backend/src/test/java/com/inholland/banking_app/controllers/AuditLogControllerTest.java`
New unit test (MockMvc, security off) for `AuditLogController`.

```
Line 1   // --Efe(Admin)
Tests:
  getAuditLogs_shouldReturn200_withAuditLogList
  getAuditLogs_shouldReturn200_withEmptyList_whenNoLogsExist
```

---

### 3. `backend/src/test/java/com/inholland/banking_app/controllers/AccountAdminFunctionalTest.java`
New functional test (full Spring context, real security) for admin account lifecycle endpoints.

```
Line 1   // --Efe(Admin)
Tests:
  freezeAccount_asAdmin_returnsOkAndStatusFrozen
  freezeAccount_asEmployee_isForbidden
  freezeAccount_asCustomer_isForbidden
  freezeAccount_anonymous_isUnauthorized
  freezeAccount_asAdmin_returns409_whenAlreadyFrozen
  unfreezeAccount_asAdmin_returnsOkAndStatusActive
  unfreezeAccount_asAdmin_returns409_whenNotFrozen
  closeAccount_asAdmin_returnsOkAndStatusClosed
  closeAccount_asAdmin_returns409_whenAlreadyClosed
```

---

## Files Modified

### 4. `backend/src/main/java/com/inholland/banking_app/services/AccountService.java`

#### Lines added at the top (new imports):
```java
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.AuditAction;
import com.inholland.banking_app.repositories.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
```

#### Lines added — new fields:
```java
// --Efe(Admin)
private final AuditService auditService;
// --Efe(Admin)
private final UserRepository userRepository;
```

#### Lines added — new methods (all marked // --Efe(Admin)):

`freezeAccount(String iban)` — Lines ~83–102
- Finds account by IBAN, throws `IllegalArgumentException` if not found
- Throws `AccountStateException` if closed or already frozen
- Calls `account.markFrozen()`, saves, records audit log `ACCOUNT_FROZEN`
- Returns `AccountResponse`

`unfreezeAccount(String iban)` — Lines ~104–122
- Finds account by IBAN, throws if not found
- Throws `AccountStateException` if not frozen
- Calls `account.unfreeze()`, saves, records audit log `ACCOUNT_UNFROZEN`
- Returns `AccountResponse`

`closeAccount(String iban)` — Lines ~124–141
- Finds account by IBAN, throws if not found
- Throws `AccountStateException` if already closed
- Calls `account.markClosed()`, saves, records audit log `ACCOUNT_CLOSED`
- Returns `AccountResponse`

`getCurrentAdmin()` (private) — Lines ~155–160
- Reads username from `SecurityContextHolder`
- Looks up `User` via `UserRepository.findByUsername`
- Throws `RuntimeException` if not found (same as original AdminService)

---

### 5. `backend/src/main/java/com/inholland/banking_app/controllers/AccountController.java`

#### Lines added — new imports:
```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
```

#### Lines added — class-level annotation:
```java
@Tag(name = "Account Management", description = "Endpoints for reading and managing bank accounts")
```

#### Lines added — three new endpoints (all marked // --Efe(Admin)):

`PATCH /accounts/{iban}/freeze` — annotated `@PreAuthorize("hasRole('ADMIN')")`
`PATCH /accounts/{iban}/unfreeze` — annotated `@PreAuthorize("hasRole('ADMIN')")`
`PATCH /accounts/{iban}/close` — annotated `@PreAuthorize("hasRole('ADMIN')")`

Each endpoint:
- Carries `@Operation` description explicitly stating "Admin operation: ... Requires ADMIN role."
- Delegates to `accountService.freezeAccount(iban)` / `unfreezeAccount` / `closeAccount`
- Returns `ResponseEntity<AccountResponse>`

No existing endpoints were changed.

---

### 6. `backend/src/main/java/com/inholland/banking_app/services/TransactionService.java`

#### Lines added — new imports:
```java
import com.inholland.banking_app.dtos.TransactionReversalResponse;
import com.inholland.banking_app.exceptions.AccountStateException;
import com.inholland.banking_app.models.enums.AuditAction;
import com.inholland.banking_app.models.enums.TransactionType;
import com.inholland.banking_app.repositories.UserRepository;
```

#### Lines added — new fields:
```java
// --Efe(Admin)
private final UserRepository userRepository;
// --Efe(Admin)
private final AuditService auditService;
```

#### Lines added — new method (marked // --Efe(Admin)):

`reverseTransaction(Long id, String adminUsername)` — ~Lines 93–148
Copied line-by-line from `AdminService.reverseTransaction`. Logic:
1. Finds transaction by ID — throws `IllegalArgumentException` if not found
2. Blocks reversing a `REVERSAL` type — throws `IllegalStateException`
3. Blocks double reversal via `existsByReversesTransactionId` — throws `IllegalStateException`
4. Finds admin `User` by username — throws `IllegalArgumentException` if not found
5. Switch on original transaction type:
   - `TRANSFER`: subtracts from toAccount, adds to fromAccount, saves both
   - `DEPOSIT`: subtracts from toAccount (with balance check), saves
   - `WITHDRAWAL`: adds to fromAccount, saves
6. Creates new `Transaction` of type `REVERSAL`, sets all fields including `reversesTransaction`
7. Records audit log `TRANSACTION_REVERSED`
8. Returns `TransactionReversalResponse`

No existing methods were changed.

---

### 7. `backend/src/main/java/com/inholland/banking_app/controllers/TransactionController.java`

#### Lines added — new imports:
```java
import com.inholland.banking_app.dtos.TransactionReversalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
```

#### Lines added — class-level annotation:
```java
@Tag(name = "Transaction Management", description = "Endpoints for creating and querying transactions")
```

#### Lines added — new endpoint (marked // --Efe(Admin)):

`POST /transactions/{id}/reverse`
- `@PreAuthorize("hasRole('ADMIN')")`
- `@Operation` description: "Admin operation: reverses a previously completed transaction ... Requires ADMIN role."
- Accepts `@PathVariable Long id` and `Authentication authentication`
- Calls `transactionService.reverseTransaction(id, authentication.getName())`
- Returns `ResponseEntity<TransactionReversalResponse>`

No existing endpoints were changed.

---

### 8. `backend/src/main/java/com/inholland/banking_app/services/AuditService.java`

#### Lines added — new imports:
```java
import com.inholland.banking_app.dtos.AuditLogResponse;
import java.util.List;
import java.util.stream.Collectors;
```

#### Lines added — new methods (all marked // --Efe(Admin)):

`getAuditLogs()` — public
- Calls `auditLogRepository.findAll()`
- Maps each `AuditLog` via `toAuditLogResponse`
- Returns `List<AuditLogResponse>`

`toAuditLogResponse(AuditLog log)` — private
- Copied line-by-line from `AdminService.toAuditLogResponse`
- Maps all fields: `id`, `actorId`, `actorUsername`, `action` (null-safe), `targetType`, `targetId`, `details`, `createdAt`

No existing `record()` method was changed.

---

### 9. `backend/src/main/java/com/inholland/banking_app/controllers/AdminController.java`

#### Lines removed (imports no longer used — deleted):
```java
import com.inholland.banking_app.dtos.AccountResponse;
import com.inholland.banking_app.dtos.TransactionReversalResponse;
import com.inholland.banking_app.dtos.AuditLogResponse;
import org.springframework.security.core.Authentication;
```

#### Lines commented out (marked // --Efe(Admin)):

Account endpoints (with marker comment above):
```java
// --Efe(Admin) — account operations moved to AccountController under /accounts/{iban}/freeze|unfreeze|close
// @GetMapping("/accounts") ...
// @GetMapping("/accounts/{iban}") ...
// @PatchMapping("/accounts/{iban}/freeze") ...
// @PatchMapping("/accounts/{iban}/unfreeze") ...
// @PatchMapping("/accounts/{iban}/close") ...
```

Transaction reversal endpoint:
```java
// --Efe(Admin) — transaction reversal moved to TransactionController under /transactions/{id}/reverse
// @PostMapping("/transactions/{id}/reverse") ...
```

Audit logs endpoint:
```java
// --Efe(Admin) — audit logs moved to AuditLogController under /audit-logs
// @GetMapping("/audit-logs") ...
```

All 7 employee endpoints and `/ping` remain unchanged and active.

---

### 10. `backend/src/main/java/com/inholland/banking_app/services/AdminService.java`

#### Lines removed (imports no longer used — deleted):
```java
import com.inholland.banking_app.models.Account;
import com.inholland.banking_app.dtos.AccountResponse;
import com.inholland.banking_app.repositories.AccountRepository;
import com.inholland.banking_app.mappers.AccountMapper;
import com.inholland.banking_app.models.Transaction;
import com.inholland.banking_app.dtos.TransactionReversalResponse;
import com.inholland.banking_app.repositories.TransactionRepository;
import com.inholland.banking_app.repositories.AuditLogRepository;
import com.inholland.banking_app.dtos.AuditLogResponse;
import com.inholland.banking_app.models.AuditLog;
import com.inholland.banking_app.models.enums.Channel;
import com.inholland.banking_app.models.enums.TransactionType;
import com.inholland.banking_app.exceptions.AccountStateException;
```

#### Lines commented out (marked // --Efe(Admin)):

Fields:
```java
// --Efe(Admin) — these dependencies were removed; their methods now live in AccountService and TransactionService
// private final AccountRepository accountRepository;
// private final AccountMapper accountMapper;
// private final TransactionRepository transactionRepository;
// private final AuditLogRepository auditLogRepository;
```

Account methods — `getAllAccounts`, `getAccount`, `freezeAccount`, `unfreezeAccount`, `closeAccount` — all commented out with `// --Efe(Admin)` marker above the block.

Transaction method — `reverseTransaction` — commented out with `// --Efe(Admin)` marker above the block.

Audit log methods — `getAuditLogs`, `toAuditLogResponse` — commented out with `// --Efe(Admin)` marker above the block.

All employee methods remain unchanged and active: `createEmployee`, `getAllEmployees`, `getEmployee`, `updateEmployee`, `setEmployeeStatus`, `deleteEmployee`, `getCurrentAdmin`, `toResponse`.

---

### 11. `backend/src/test/java/com/inholland/banking_app/controllers/AdminControllerTest.java`

#### Lines removed (imports for commented-out code):
```java
import com.inholland.banking_app.dtos.AccountResponse;
import com.inholland.banking_app.dtos.MoneyResponse;
import com.inholland.banking_app.exceptions.AccountStateException;
import com.inholland.banking_app.models.enums.AccountStatus;
```

#### Lines commented out (marked // --Efe(Admin)):

Account tests — `getAllAccounts`, `getAccount`, `freezeAccount`, `freezeAccount_conflict`, `unfreezeAccount`, `closeAccount` — commented out with marker above block.

Transaction reversal test — `reverseTransaction` — commented out with marker above block.

Audit log test — `getAuditLogs` — commented out with marker above block.

All employee and ping tests remain unchanged and active.

---

### 12. `backend/src/test/java/com/inholland/banking_app/controllers/AccountControllerTest.java`

#### Lines added — new imports:
```java
import com.inholland.banking_app.models.enums.AuditAction;
import com.inholland.banking_app.repositories.UserRepository;
// --Efe(Admin) (marker comment)
```

#### Lines added — new mocks in test class:
```java
// --Efe(Admin)
@Mock private AuditService auditService;
// --Efe(Admin)
@Mock private UserRepository userRepository;
```

#### Lines added — 6 new test methods (all marked // --Efe(Admin)):
```
freezeAccount_returns200_whenAccountIsActive
freezeAccount_returns409_whenAlreadyFrozenOrClosed
unfreezeAccount_returns200_whenAccountIsFrozen
unfreezeAccount_returns409_whenAccountIsNotFrozen
closeAccount_returns200_whenAccountIsActive
closeAccount_returns409_whenAlreadyClosed
```

No existing tests were changed.

---

### 13. `backend/src/test/java/com/inholland/banking_app/controllers/TransactionControllerTest.java`

#### Lines added — new imports:
```java
import com.inholland.banking_app.dtos.TransactionReversalResponse;
import static org.mockito.ArgumentMatchers.eq;
// --Efe(Admin) (marker comment)
```

#### Lines added — 3 new test methods (all marked // --Efe(Admin)):
```
reverseTransaction_shouldReturn200_whenSuccessful
reverseTransaction_shouldReturn500_whenAlreadyReversed   (IllegalStateException → 500, not mapped in GlobalExceptionHandler)
reverseTransaction_shouldReturn400_whenTransactionNotFound
```

No existing tests were changed.

---

### 14. `backend/src/test/java/com/inholland/banking_app/services/AdminServiceTest.java`

#### Lines removed — field mocks for moved dependencies:
```java
@Mock private AccountRepository accountRepository;
@Mock private AccountMapper accountMapper;
@Mock private TransactionRepository transactionRepository;
@Mock private AuditLogRepository auditLogRepository;
```

Replaced with comment:
```java
// --Efe(Admin) — these mocks were removed; their methods now live in AccountService and TransactionService
```

#### Lines commented out (marked // --Efe(Admin)):

Account test stubs — `getAllAccounts`, `getAccount`, `freezeAccount_Success`, `freezeAccount_AlreadyFrozen`, `unfreezeAccount_Success`, `closeAccount_Success`.

Transaction reversal test stubs — `reverseTransaction_Success`, `reverseTransaction_AlreadyReversed`.

Audit log test stub — `getAuditLogs`.

All employee tests remain unchanged and active.

---

### 15. `backend/src/test/java/com/inholland/banking_app/services/AccountServiceTest.java`

#### Lines added — new imports:
```java
import com.inholland.banking_app.models.enums.AuditAction;
import com.inholland.banking_app.repositories.UserRepository;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
// --Efe(Admin) (marker comment)
```

#### Lines added — new mocks:
```java
// --Efe(Admin)
@Mock private AuditService auditService;
// --Efe(Admin)
@Mock private UserRepository userRepository;
```

#### Lines added — 7 new test methods (all marked // --Efe(Admin)) + private helper:
```
freezeAccount_marksFrozenAndRecordsAudit_whenActiveAccount
freezeAccount_throwsConflict_whenAlreadyFrozen
freezeAccount_throwsConflict_whenAccountIsClosed
unfreezeAccount_marksActiveAndRecordsAudit_whenFrozen
unfreezeAccount_throwsConflict_whenNotFrozen
closeAccount_marksClosedAndRecordsAudit_whenActiveAccount
closeAccount_throwsConflict_whenAlreadyClosed
stubCurrentAdmin() — private helper that seeds SecurityContextHolder and mocks userRepository
```

No existing tests were changed.

---

### 16. `backend/src/test/java/com/inholland/banking_app/services/TransactionServiceTest.java`

#### Lines added — new mocks:
```java
// --Efe(Admin)
@Mock private com.inholland.banking_app.repositories.UserRepository userRepository;
// --Efe(Admin)
@Mock private AuditService auditService;
```

#### Lines added — new static import:
```java
import static org.mockito.Mockito.times;
// --Efe(Admin) (marker comment)
```

#### Lines added — 4 new test methods (all marked // --Efe(Admin)):
```
reverseTransaction_transfer_shouldRestoreBalancesAndSaveReversal
reverseTransaction_shouldThrow_whenAlreadyReversed
reverseTransaction_shouldThrow_whenOriginalIsAReversal
reverseTransaction_shouldThrow_whenTransactionNotFound
```

No existing tests were changed.

---

## No Changes Made To

The following files were read for reference but not modified:

- All DTOs (`AccountResponse`, `TransactionReversalResponse`, `AuditLogResponse`, `EmployeeResponse`, etc.)
- All entity models (`Account`, `Transaction`, `AuditLog`, `User`, `EmployeeProfile`)
- All repository interfaces (`AccountRepository`, `TransactionRepository`, `UserRepository`, `AuditLogRepository`)
- All enums (`AccountStatus`, `AuditAction`, `TransactionType`, `Channel`, `Role`)
- `SecurityConfig.java`
- `GlobalExceptionHandler.java`
- `UserService.java`
- `AuthService.java`
- `AuthController.java`
- `UserController.java`

---

## Pre-Existing Test Failures (Not Caused by This Refactor)

These failures existed before the refactor and were verified as pre-existing:

| Test | Reason |
|---|---|
| `AuthEndToEndTest` (6 tests) | Rate limiter returns 429 during test suite execution |
| `TransactionEndToEndTest` (9 tests) | Rate limiter returns 429 during test suite execution |
| `SecurityTest.postAuthLogin_isPublic` | Rate limiter returns 429 |
| `AccountTest` (6 errors) | `Account.setId(Long)` method does not exist — `@Id` is set by JPA |
| `AccountPolicyTest` (9 errors) | Same `Account.setId` issue |
| `UserServiceTest` (2 errors) | `AccountRepository.findByCustomerId` returns null in mock setup |
| `TransactionServiceTest.createTransaction_deposit_shouldPropagateException_whenDailyLimitIsExceeded` | `executeDeposit` uses `applyCredit` which never calls `checkDailyLimit` — original logic gap |
