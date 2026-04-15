# Employee API

Source YAML:
- [paths/employee.yaml](/Users/rashidismayilzade/Projects/Banking-App/backend/src/main/resources/static/openapi/paths/employee.yaml)
- [schemas/employee.yaml](/Users/rashidismayilzade/Projects/Banking-App/backend/src/main/resources/static/openapi/components/schemas/employee.yaml)
- [schemas/customer.yaml](/Users/rashidismayilzade/Projects/Banking-App/backend/src/main/resources/static/openapi/components/schemas/customer.yaml)
- [schemas/transactions.yaml](/Users/rashidismayilzade/Projects/Banking-App/backend/src/main/resources/static/openapi/components/schemas/transactions.yaml)

## Purpose

Employee-only endpoints for customer approval, customer/account management, and full transaction oversight.

## Authentication

All employee endpoints require `Bearer <JWT>` and an employee role.

## Customer Management Endpoints

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/employees/customers` | List customers with filters for status and account existence |
| `POST` | `/employees/customers/{customerUserId}/approval` | Approve or reject signup and create accounts on approval |
| `PATCH` | `/employees/accounts/{accountId}/limits` | Update account transfer limits |
| `POST` | `/employees/accounts/{accountId}/close` | Close a customer account |

Supported filters for `/employees/customers`:
- `page`
- `size`
- `sort`
- `status`
- `hasAccounts`
- `q`

## Transaction Endpoints

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/employees/customers/{customerUserId}/transactions` | View one customer's paginated transaction history |
| `POST` | `/employees/transfers` | Transfer funds between customer accounts as an employee |
| `GET` | `/employees/transactions` | View all transactions in the system |

Supported filters for `/employees/customers/{customerUserId}/transactions`:
- `page`
- `size`
- `sort`
- `startDateTime`
- `endDateTime`

Supported filters for `/employees/transactions`:
- `page`
- `size`
- `sort`
- `startDateTime`
- `endDateTime`
- `amountMin`
- `amountMax`
- `initiatedByUserId`
- `channel`

## Approval Rules

- Approving a customer must create both checking and savings accounts
- Approval request can include transfer limits for each new account
- Customer status changes according to the approval decision
- Duplicate or conflicting approval actions should return `409`

## Key Models

### `EmployeeCustomerSummary`
- `customerUserId`
- `firstName`
- `lastName`
- `email`
- `username`
- `phoneNumber`
- `status`
- `hasAccounts`
- `accountCount`
- `combinedBalance`

### `CustomerApprovalRequest`
- `decision`
- `note`
- `checkingAbsoluteTransferLimit`
- `checkingDailyTransferLimit`
- `savingsAbsoluteTransferLimit`
- `savingsDailyTransferLimit`

### `CustomerApprovalResponse`
- `approvalId`
- `customerUserId`
- `decision`
- `resultingCustomerStatus`
- `createdAccounts`
- `decidedAt`

### `AccountLimitUpdateRequest`
- `absoluteTransferLimit`
- `dailyTransferLimit`

### `EmployeeTransferRequest`
- `fromAccountId`
- `toAccountId`
- `amount`
- `description`

## Common Responses

- `200`: list, approval, limit update, close account, or transaction page returned
- `201`: employee transfer recorded
- `401`: invalid or missing JWT
- `403`: authenticated user lacks employee authorization
- `404`: customer or account not found
- `409`: conflicting approval or account state
- `422`: validation or business-rule failure
