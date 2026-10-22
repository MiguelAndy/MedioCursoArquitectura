# 🌐 API Specifications & Contracts

This directory contains formal API contracts (e.g., OpenAPI 3.x, AsyncAPI, GraphQL, Protobuf).

In a **Spec-First** architecture:
1. **The Contract is Written First**: Before building routes or controllers, the API contract is designed and agreed upon.
2. **Mock Servers**: Frontend and consumer teams generate mock servers directly from `openapi.yaml` (e.g., using Prism: `npx @stoplight/prism-cli mock specs/api/openapi.yaml`).
3. **Automated Validation & Linting**: Use tools like [Spectral](https://stoplight.io/open-source/spectral) to ensure standard naming, error responses, and semantic versioning.
4. **Contract Testing**: Automated test suites in `tests/contract/` execute requests against the running app and validate that the payloads, HTTP status codes, and headers match this specification.

## 📁 Files

- [`openapi.yaml`](openapi.yaml): Main REST API contract definition.
