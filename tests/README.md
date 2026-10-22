# 🧪 Testing Strategy (Spec-Aligned)

In **Spec-Driven Development**, the test suite mirrors the specifications directly.

```text
tests/
├── contract/       # Validates that the server API strictly satisfies specs/api/openapi.yaml
├── acceptance/     # Runs executable Gherkin scenarios from specs/acceptance/features/
├── integration/    # Tests use cases integrated with database and infrastructure
└── unit/           # Fast unit tests for pure domain entities and value objects
```

---

## 🎯 The Spec-First Testing Pyramid

```text
           / \
          /   \     Acceptance Tests (Gherkin BDD)
         /-----\    Contract Tests (OpenAPI schema conformance)
        /       \   Integration Tests (Use Cases + Repositories)
       /---------\  Unit Tests (Domain entities & business rules)
```

1. **Unit Tests**: Verify domain invariants without dependencies.
2. **Integration Tests**: Verify database persistence adapters and application use cases.
3. **Contract Tests**: Ensure zero schema drift between implementation and `specs/api/openapi.yaml`.
4. **Acceptance Tests**: Verify end-to-end user journeys defined in `specs/acceptance/`.
