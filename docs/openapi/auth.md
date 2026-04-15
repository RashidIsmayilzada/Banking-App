# Authentication API

Source YAML:
- [paths/auth.yaml](/Users/rashidismayilzade/Projects/Banking-App/backend/src/main/resources/static/openapi/paths/auth.yaml)
- [schemas/auth.yaml](/Users/rashidismayilzade/Projects/Banking-App/backend/src/main/resources/static/openapi/components/schemas/auth.yaml)

## Purpose

Handles customer registration plus login, logout, and current-user auth context.

## Endpoints

| Method | Path | Purpose | Auth |
|---|---|---|---|
| `POST` | `/auth/register/customer` | Register a new customer | None |
| `POST` | `/auth/login` | Log in with username or email and password | None |
| `POST` | `/auth/logout` | Invalidate current JWT-backed session | Bearer JWT |
| `GET` | `/auth/me` | Fetch current authenticated user context | Bearer JWT |

## Registration Flow

- Creates the user account and customer profile
- Sets role to `CUSTOMER`
- Does not create bank accounts during signup
- Customer remains in `PENDING_APPROVAL` until employee approval
- Registered customers can still log in, but before approval they should only see a basic welcome experience

## Key Models

### `CustomerRegistrationRequest`
- `firstName`
- `lastName`
- `email`
- `username`
- `password`
- `bsn`
- `phoneNumber`

### `CustomerRegistrationResponse`
- `userId`
- `role`
- `customerStatus`
- `canAccessBankingFeatures`
- `message`

### `LoginRequest`
- `email`
- `password`

### `LoginResponse`
- `accessToken`
- `tokenType`
- `expiresIn`
- `user`

### `AuthContext`
- `userId`
- `email`
- `username`
- `role`
- `active`
- `lastLoginAt`
- `customerStatus`
- `employeeEnabled`
- `authorizedFeatures`

## Common Responses

- `201`: registration succeeded
- `200`: login/logout/me succeeded
- `401`: invalid or missing authentication
- `403`: user exists but is not allowed to access the application
- `409`: duplicate registration state such as reused email or username
- `422`: validation failure
