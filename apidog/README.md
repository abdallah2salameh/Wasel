# API-Dog Notes

This project exposes OpenAPI metadata at `/v3/api-docs` and Swagger UI at `/swagger-ui`.

Recommended API-Dog workflow:

1. Import the OpenAPI document from `/v3/api-docs`.
2. Create one environment per deployment target using the variables from `.env.example`.
3. Define auth presets for:
   - `Bearer access token`
   - `Bearer refresh token` flow through `/api/v1/auth/refresh`
4. Add test cases for:
   - authentication lifecycle
   - incident filtering and pagination
   - checkpoint status history
   - report submission, moderation, and voting
   - alert subscription management
   - route estimation with and without constraints

Suggested environment variables:

- `baseUrl`
- `accessToken`
- `refreshToken`
- `adminEmail`
- `adminPassword`
