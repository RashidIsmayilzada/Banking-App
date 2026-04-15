# Customer API

Source YAML:
- [paths/customer.yaml](/Users/rashidismayilzade/Projects/Banking-App/backend/src/main/resources/static/openapi/paths/customer.yaml)
- [schemas/customer.yaml](/Users/rashidismayilzade/Projects/Banking-App/backend/src/main/resources/static/openapi/components/schemas/customer.yaml)
- [schemas/transactions.yaml](/Users/rashidismayilzade/Projects/Banking-App/backend/src/main/resources/static/openapi/components/schemas/transactions.yaml)

## Purpose

Customer-facing banking endpoints for profile data, account overview, IBAN lookup, transfers, and transaction history.

## Authentication

All customer endpoints require `Bearer <JWT>`.

## Account and Profile Endpoints

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/customers/me/profile` | Get current customer profile |
| `GET` | `/customers/me/accounts` | List customer accounts and combined totals |
| `GET` | `/customers/me/accounts/{accountId}` | Get a specific owned account |
| `GET` | `/customers/iban-search` | Find another customer's IBAN by first and last name |

## Transfer Endpoints

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/customers/me/transfers/internal` | Transfer between own checking and savings accounts |
| `POST` | `/customers/me/transfers/external` | Transfer from customer checking account to another customer's account |

## Transaction Endpoint

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/customers/me/transactions` | View paginated and filterable transaction history |

Supported filters for `/customers/me/transactions`:
- `page`
- `size`
- `sort`
- `startDateTime`
- `endDateTime`
- `amountMin`
- `amountMax`
- `iban`
- `accountId`
- `amountEquals`

## Key Models

### `CustomerProfile`
- `userId`
- `firstName`
- `lastName`
- `email`
- `username`
- `phoneNumber`
- `status`
- `registeredAt`

### `CustomerAccount`
- `accountId`
- `iban`
- `accountType`
- `balance`
- `absoluteTransferLimit`
- `dailyTransferLimit`
- `active`
- `createdAt`
- `closedAt`

### `CustomerAccountListResponse`
- `customer`
- `totals`
- `accounts`

### `InternalTransferRequest`
- `fromAccountId`
- `toAccountId`
- `amount`
- `description`

### `ExternalTransferRequest`
- `fromAccountId`
- `toIban`
- `amount`
- `description`

## Common Responses

- `200`: profile, accounts, account detail, IBAN lookup, or transaction page returned
- `201`: transfer recorded
- `401`: invalid or missing JWT
- `403`: authenticated user is not allowed to access the resource
- `404`: account not found
- `422`: validation or transfer-rule failure
