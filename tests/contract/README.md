# 🤝 Contract Tests

Contract tests verify that the running API conforms strictly to the contract specified in [`specs/api/openapi.yaml`](../../specs/api/openapi.yaml).

## Recommended Tooling:
- **Prism** (`@stoplight/prism-cli`): Mock server generation and HTTP request/response proxy validation.
- **Dredd** / **Schemathesis**: Property-based contract testing against OpenAPI specs.
- **Pact**: Consumer-driven contract testing when integrating between microservices.
