# Transactions API

Source YAML:
- [paths/transactions.yaml](../../backend/src/main/resources/static/openapi/paths/transactions.yaml)
- [schemas/transactions.yaml](../../backend/src/main/resources/static/openapi/components/schemas/transactions.yaml)
- [schemas/common.yaml](../../backend/src/main/resources/static/openapi/components/schemas/common.yaml)

## Purpose

Unified transaction endpoints that cover all three transaction types in a single resource:

- **TRANSFER** — funds between accounts (internal via `toAccountId`, external via `toIban`)
- **DEPOSIT** — ATM cash deposit to `accountId`
- **WITHDRAWAL** — ATM cash withdrawal from `accountId`

## Authentication

All endpoints require `Bearer <JWT>` (`bearerAuth`).

## Endpoints

| Method | Path | Purpose | Auth |
|---|---|---|---|
| `GET` | `/transactions` | List transactions with filters & pagination | `bearerAuth` |
| `POST` | `/transactions` | Create a transaction (TRANSFER / DEPOSIT / WITHDRAWAL) | `bearerAuth` |

### Authorization scope

- **Customers**: GET returns only transactions where the caller is the initiator or owns the source/destination account. POST allows TRANSFER from an account they own, or DEPOSIT/WITHDRAWAL on an account they own.
- **Employees**: GET returns all transactions and accepts the `userId` filter. POST may operate on any customer's account.

## Query Parameters (GET)

| Name | Type | Notes |
|---|---|---|
| `page` | integer | 0-indexed, default `0` |
| `size` | integer | 1..100, default `20` |
| `sort` | string | e.g. `createdAt,desc` |
| `startDateTime` | date-time | Inclusive lower bound on `createdAt` |
| `endDateTime` | date-time | Inclusive upper bound on `createdAt` |
| `amountMin` | number | |
| `amountMax` | number | |
| `amountEquals` | number | Exact amount |
| `iban` | string | Matches `fromAccount.iban` or `toAccount.iban` |
| `accountId` | integer | Matches source or destination account |
| `userId` | integer | Initiator; **employee-only** |
| `channel` | string | `WEB` \| `ATM` \| `EMPLOYEE` |

## Key Models

### `TransactionRequest`

Required: `type`, `amount`.

| Field | Required when | Notes |
|---|---|---|
| `type` | always | `TRANSFER` \| `DEPOSIT` \| `WITHDRAWAL` |
| `fromAccountId` | `TRANSFER` | Source account |
| `toAccountId` | internal `TRANSFER` | Destination account (mutually exclusive with `toIban`) |
| `toIban` | external `TRANSFER` | INHO IBAN, mutually exclusive with `toAccountId` |
| `accountId` | `DEPOSIT`, `WITHDRAWAL` | Operating account |
| `amount` | always | Greater than `0` |
| `description` | optional | Max length 255 |

### `TransferResult`
- `transaction` — the created `Transaction`
- `sourceBalance` — `Money`, balance of the source/operating account after the operation
- `destinationBalance` — `Money`, present for `TRANSFER`

### `Transaction`
- `transactionId`, `transactionType`, `amount` (`Money`)
- `fromAccount`, `toAccount` (`TransactionParty`)
- `channel`, `initiatedByUserId`, `createdAt`, `description`

### `TransactionPage`
- `items: Transaction[]`
- `page: PageMetadata` (`page`, `size`, `totalElements`, `totalPages`)

## Business Rules

- **Absolute transfer limit**: balance after a `TRANSFER` / `WITHDRAWAL` may not drop below `account.absoluteTransferLimit`.
- **Daily transfer limit**: cumulative outgoing amount per source account per calendar day may not exceed `account.dailyTransferLimit`.
- **Account status**: both source and destination accounts must be active.
- **Channel inference**: `DEPOSIT`/`WITHDRAWAL` → `ATM`; `TRANSFER` → `EMPLOYEE` for employee callers, otherwise `WEB`.

## Common Responses

- `201`: transaction recorded
- `200`: transaction page returned
- `400`: business-rule violation (closed account, limit exceeded, missing field for type)
- `401`: missing or invalid JWT
- `403`: customer attempting to view/operate outside their own scope
- `422`: input validation failure
