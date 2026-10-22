# 🌐 Presentation Layer (Delivery Mechanism)

The **Presentation Layer** handles user interactions, HTTP routing, request deserialization, input validation, and response serialization.

## Principles:
- **Contract Adherence**: Handlers and controllers here are strictly governed by `specs/api/openapi.yaml`.
- **Validation**: Incoming payloads are validated against the defined schema before reaching the Application layer.
- **Thin Controllers**: Controllers only parse inputs, call application use-cases, and return formatted HTTP responses.

## Structure:
- `controllers/`: HTTP route handlers.
- `routes/`: Routing tables and path registrations matching OpenAPI paths.
- `middlewares/`: Authentication, rate limiting, CORS, error handling envelopes.
