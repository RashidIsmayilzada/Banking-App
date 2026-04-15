# OpenAPI Documentation

This folder splits the API documentation by domain so it is easier to navigate than the single top-level OpenAPI entrypoint.

Source of truth:
- [openapi.yaml](/Users/rashidismayilzade/Projects/Banking-App/backend/src/main/resources/static/openapi/openapi.yaml)
- [paths/auth.yaml](/Users/rashidismayilzade/Projects/Banking-App/backend/src/main/resources/static/openapi/paths/auth.yaml)
- [paths/customer.yaml](/Users/rashidismayilzade/Projects/Banking-App/backend/src/main/resources/static/openapi/paths/customer.yaml)
- [paths/employee.yaml](/Users/rashidismayilzade/Projects/Banking-App/backend/src/main/resources/static/openapi/paths/employee.yaml)
- [paths/atm.yaml](/Users/rashidismayilzade/Projects/Banking-App/backend/src/main/resources/static/openapi/paths/atm.yaml)

Available sections:
- [Authentication](./auth.md)
- [Customer API](./customer.md)
- [Employee API](./employee.md)
- [ATM API](./atm.md)

Shared conventions:
- Web API auth uses `Bearer <JWT>` via `bearerAuth`
- ATM API auth uses `X-ATM-Session-Token`
- Common error responses: `401 Unauthorized`, `403 Forbidden`, `404 NotFound`, `409 Conflict`, `422 ValidationError`
- Pagination query params: `page`, `size`, `sort`
- Common filters where applicable: `startDateTime`, `endDateTime`, `amountMin`, `amountMax`
