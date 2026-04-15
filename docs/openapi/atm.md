# ATM API

Source YAML:
- [paths/atm.yaml](/Users/rashidismayilzade/Projects/Banking-App/backend/src/main/resources/static/openapi/paths/atm.yaml)
- [schemas/atm.yaml](/Users/rashidismayilzade/Projects/Banking-App/backend/src/main/resources/static/openapi/components/schemas/atm.yaml)
- [schemas/transactions.yaml](/Users/rashidismayilzade/Projects/Banking-App/backend/src/main/resources/static/openapi/components/schemas/transactions.yaml)

## Purpose

ATM-specific authentication and cash transaction endpoints.

## Authentication

- `POST /atm/sessions` requires no prior auth
- All other ATM endpoints require `X-ATM-Session-Token`

## Endpoints

| Method | Path | Purpose | Auth |
|---|---|---|---|
| `POST` | `/atm/sessions` | Start ATM session using email and password | None |
| `DELETE` | `/atm/sessions/{sessionId}` | End ATM session | `X-ATM-Session-Token` |
| `POST` | `/atm/transactions/deposits` | Deposit money into customer account | `X-ATM-Session-Token` |
| `POST` | `/atm/transactions/withdrawals` | Withdraw money from customer account | `X-ATM-Session-Token` |

## Key Models

### `AtmSessionStartRequest`
- `email`
- `password`

### `AtmSessionResponse`
- `sessionId`
- `sessionToken`
- `customerUserId`
- `startedAt`
- `successfulLogin`

### `AtmDepositRequest`
- `accountId`
- `amount`
- `description`

### `AtmWithdrawalRequest`
- `accountId`
- `amount`
- `description`

## Common Responses

- `201`: ATM session started, deposit recorded, or withdrawal recorded
- `204`: ATM session ended
- `401`: invalid login or missing/invalid ATM session token
- `403`: authenticated ATM user is not allowed to perform the action
- `404`: ATM session not found
- `422`: validation or transaction-rule failure
