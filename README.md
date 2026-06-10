# Banking App

A full-stack banking application with a Spring Boot backend and Vue.js frontend.

## Development & Testing

The application comes with pre-seeded mock data for testing. All users use the same default password unless otherwise specified.

**Default Password:** `Password@123`

### Employee Accounts (Admins)
| Email | Username | Role |
| :--- | :--- | :--- |
| `employee01@bank.com` | `employee01` | Employee |
| `employee02@bank.com` | `employee02` | Employee |
| `employee03@bank.com` | `employee03` | Employee (Password: `Test1234!`) |

### Customer Accounts
| Email | Username | IBAN (Checking) |
| :--- | :--- | :--- |
| `john.doe@gmail.com` | `johndoe` | `NL10INHO0000000001` |
| `jane.smith@gmail.com` | `janesmith` | `NL10INHO0000000002` |
| `bob.jones@hotmail.com` | `bobjones` | `NL10INHO0000000003` |

## Setup

### Backend
1. Navigate to `backend/`
2. Run `./mvnw spring-boot:run`

### Frontend
1. Navigate to `frontend/`
2. Run `npm install`
3. Run `npm run dev`
