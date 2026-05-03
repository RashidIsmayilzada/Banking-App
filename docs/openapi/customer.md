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
| `PUT` | `/customers/me/profile` | Update customer profile information (phone number) |
| `PUT` | `/customers/me/password` | Change password |
| `GET` | `/customers/me/accounts` | List customer accounts and combined totals |
| `GET` | `/customers/me/accounts/{accountId}` | Get a specific owned account |
| `GET` | `/customers/me/accounts/{accountId}/balance` | Get current balance for a specific account |
| `GET` | `/customers/me/accounts/{accountId}/transactions` | Get transaction history for a specific account |
| `GET` | `/customers/iban-search` | Find another customer's IBAN by first and last name |

## Transfer Endpoints

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/customers/me/transfers/internal` | Transfer between own checking and savings accounts |
| `POST` | `/customers/me/transfers/external` | Transfer from customer checking account to another customer's account |

## Transaction Endpoints

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/customers/me/transactions` | View paginated and filterable transaction history (all accounts) |
| `GET` | `/customers/me/transactions/{transactionId}` | Get details of a specific transaction |

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
- `userId` - Primary key, foreign key to User
- `firstName` - Customer's first name
- `lastName` - Customer's last name
- `bsn` - Dutch social security number (Burgerservicenummer), unique 9-digit identifier
- `phoneNumber` - Contact phone number
- `status` - Customer status (e.g., ACTIVE, PENDING, SUSPENDED)
- `registeredAt` - Timestamp when customer registered

**Note:** `email` and `username` are stored in the related `User` model, accessible via the user relationship.

### `UpdateProfileRequest`
- `phoneNumber` - New phone number (optional)

### `ChangePasswordRequest`
- `currentPassword` - Current password for verification
- `newPassword` - New password (minimum 8 characters)

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

### `AccountBalanceResponse`
- `accountId` - Account identifier
- `iban` - Account IBAN
- `balance` - Current balance
- `currency` - Currency code (e.g., EUR)
- `asOfTimestamp` - Timestamp when balance was retrieved

### `CustomerAccountListResponse`
- `customer`
- `totals`
- `accounts`

### `PaginatedResponse`
Used for transaction history and other paginated endpoints:
- `content` - Array of items (transactions, etc.)
- `page` - Current page number (0-indexed)
- `size` - Items per page
- `totalElements` - Total number of items across all pages
- `totalPages` - Total number of pages
- `first` - Whether this is the first page
- `last` - Whether this is the last page
- `numberOfElements` - Number of items in current page

### `TransactionDetailResponse`
- `id` - Transaction identifier
- `transactionType` - Type of transaction (TRANSFER, DEPOSIT, WITHDRAWAL, etc.)
- `fromAccount` - Source account details (if applicable)
- `toAccount` - Destination account details (if applicable)
- `amount` - Transaction amount
- `currency` - Currency code
- `channel` - Channel used (WEB, MOBILE, ATM, etc.)
- `description` - Transaction description
- `createdAt` - Timestamp when transaction was created
- `status` - Transaction status

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

## Business Rules

### Transfer Limits
- **Daily Transfer Limit**: Maximum amount that can be transferred in a single day (cumulative)
- **Absolute Transfer Limit**: Maximum amount per single transaction
- **Account Minimum Balance**: Accounts may have minimum balance requirements
- **Active Account Requirement**: Both source and destination accounts must be active

### Transfer Validation
- Internal transfers require both accounts to belong to the same customer
- External transfers can only be initiated from CHECKING accounts
- Transfers to SAVINGS accounts may have restrictions
- Amount must be greater than 0
- Source account must have sufficient balance

### Transaction History
- Customers can only view transactions for their own accounts
- Transactions are paginated (default 20 per page)
- Filters are applied with AND logic
- Date filters use ISO 8601 format (e.g., 2024-01-01T00:00:00)

## Common Responses

- `200`: profile, accounts, account detail, IBAN lookup, balance, or transaction page returned
- `201`: transfer recorded successfully
- `400`: bad request (invalid input format)
- `401`: invalid or missing JWT
- `403`: authenticated user is not allowed to access the resource
- `404`: account or transaction not found
- `422`: validation or transfer-rule failure (e.g., insufficient balance, limit exceeded)

## Error Response Format

```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Insufficient balance for transfer",
  "path": "/api/customers/me/transfers/external"
}
```

## Example Requests

### Update Profile
```json
PUT /api/customers/me/profile
{
  "phoneNumber": "+31612345678"
}
```

### Change Password
```json
PUT /api/customers/me/password
{
  "currentPassword": "OldPassword123!",
  "newPassword": "NewPassword456!"
}
```

### Internal Transfer
```json
POST /api/customers/me/transfers/internal
{
  "fromAccountId": 123,
  "toAccountId": 124,
  "amount": 500.00,
  "description": "Savings transfer"
}
```

### External Transfer
```json
POST /api/customers/me/transfers/external
{
  "fromAccountId": 123,
  "toIban": "NL91INHO0123456789",
  "amount": 250.00,
  "description": "Payment for services"
}
```

### Get Transactions with Filters
```
GET /api/customers/me/transactions?page=0&size=20&startDateTime=2024-01-01T00:00:00&endDateTime=2024-01-31T23:59:59&amountMin=50&sort=createdAt,desc
```

## Example Responses

### Get Profile - Success (200)
```json
{
  "userId": 42,
  "firstName": "John",
  "lastName": "Doe",
  "bsn": "123456789",
  "phoneNumber": "+31612345678",
  "email": "john.doe@example.com",
  "username": "johndoe",
  "status": "ACTIVE",
  "registeredAt": "2024-01-15T10:30:00"
}
```

### Get Accounts - Success (200)
```json
{
  "customer": {
    "userId": 42,
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com"
  },
  "totals": {
    "totalBalance": 5250.00,
    "currency": "EUR"
  },
  "accounts": [
    {
      "accountId": 123,
      "iban": "NL91INHO0123456789",
      "accountType": "CHECKING",
      "balance": 5000.00,
      "absoluteTransferLimit": 10000.00,
      "dailyTransferLimit": 5000.00,
      "active": true,
      "createdAt": "2024-01-15T10:30:00",
      "closedAt": null
    },
    {
      "accountId": 124,
      "iban": "NL91INHO0123456790",
      "accountType": "SAVINGS",
      "balance": 250.00,
      "absoluteTransferLimit": 1000.00,
      "dailyTransferLimit": 1000.00,
      "active": true,
      "createdAt": "2024-01-15T10:30:00",
      "closedAt": null
    }
  ]
}
```

### Get Account Balance - Success (200)
```json
{
  "accountId": 123,
  "iban": "NL91INHO0123456789",
  "balance": 5000.00,
  "currency": "EUR",
  "asOfTimestamp": "2024-01-20T14:25:30"
}
```

### Internal Transfer - Success (201)
```json
{
  "id": 456,
  "transactionType": "TRANSFER",
  "fromAccount": {
    "accountId": 123,
    "iban": "NL91INHO0123456789"
  },
  "toAccount": {
    "accountId": 124,
    "iban": "NL91INHO0123456790"
  },
  "amount": 500.00,
  "currency": "EUR",
  "channel": "WEB",
  "description": "Savings transfer",
  "createdAt": "2024-01-20T14:25:30"
}
```

### Get Transactions - Success (200) - Paginated
```json
{
  "content": [
    {
      "id": 456,
      "transactionType": "TRANSFER",
      "fromAccount": {
        "accountId": 123,
        "iban": "NL91INHO0123456789"
      },
      "toAccount": {
        "accountId": 124,
        "iban": "NL91INHO0123456790"
      },
      "amount": 500.00,
      "currency": "EUR",
      "channel": "WEB",
      "description": "Savings transfer",
      "createdAt": "2024-01-20T14:25:30"
    },
    {
      "id": 455,
      "transactionType": "DEPOSIT",
      "fromAccount": null,
      "toAccount": {
        "accountId": 123,
        "iban": "NL91INHO0123456789"
      },
      "amount": 1000.00,
      "currency": "EUR",
      "channel": "ATM",
      "description": "Cash deposit",
      "createdAt": "2024-01-19T09:15:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 45,
  "totalPages": 3,
  "first": true,
  "last": false,
  "numberOfElements": 20
}
```

### IBAN Search - Success (200)
```json
{
  "iban": "NL91INHO0987654321",
  "firstName": "Jane",
  "lastName": "Smith"
}
```

### Change Password - Success (200)
```json
{
  "message": "Password updated successfully"
}
```

### Error - Insufficient Balance (422)
```json
{
  "timestamp": "2024-01-20T14:25:30",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Insufficient balance for transfer. Available: 5000.00 EUR, Required: 6000.00 EUR",
  "path": "/api/customers/me/transfers/external"
}
```

### Error - Daily Limit Exceeded (422)
```json
{
  "timestamp": "2024-01-20T14:25:30",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Daily transfer limit exceeded. Limit: 5000.00 EUR, Already used today: 4800.00 EUR, Attempted: 500.00 EUR",
  "path": "/api/customers/me/transfers/external"
}
```

### Error - Account Not Found (404)
```json
{
  "timestamp": "2024-01-20T14:25:30",
  "status": 404,
  "error": "Not Found",
  "message": "Account with ID 999 not found or does not belong to you",
  "path": "/api/customers/me/accounts/999"
}
```

### Error - Unauthorized (401)
```json
{
  "timestamp": "2024-01-20T14:25:30",
  "status": 401,
  "error": "Unauthorized",
  "message": "JWT token is missing or invalid",
  "path": "/api/customers/me/profile"
}
```