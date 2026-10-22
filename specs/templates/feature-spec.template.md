# 📋 Feature Specification: [Feature Name]

* **Feature ID:** `FEAT-XXX`
* **Status:** `[DRAFT | IN_REVIEW | APPROVED | IMPLEMENTED]`
* **Author(s):** [Author Name / Team]
* **Target Release / Milestone:** [Sprint / Milestone]
* **Related ADR:** [Link to ADR or N/A]

---

## 1. Problem Statement & Motivation
What user or business problem does this feature solve? Why now?

---

## 2. Scope

### In Scope
- [Capability 1]
- [Capability 2]

### Out of Scope (Non-Goals)
- [Future enhancement 1]
- [Edge case postponed to later phase]

---

## 3. Detailed Requirements & Domain Rules
1. **Rule 1:** [Business logic rule]
2. **Rule 2:** [Validation constraint]
3. **Rule 3:** [Permission / Security requirement]

---

## 4. Contract & Data Specification
- **API Spec Updates:** Link or inline OpenAPI path snippet in `specs/api/openapi.yaml`.
- **Data Model Updates:** Link to schema definition in `specs/data-models/schemas/`.

```yaml
# Example endpoint spec snippet
paths:
  /resource:
    post:
      summary: ...
```

---

## 5. Acceptance Criteria (Gherkin BDD)
Link to `.feature` file in `specs/acceptance/features/` or specify scenarios below:

```gherkin
Scenario: [Happy path scenario title]
  Given [initial state]
  When [action executed]
  Then [expected outcome]
```

---

## 6. Implementation Checklist
- [ ] Spec reviewed & approved by team
- [ ] Contract linting passed (`openapi.yaml`)
- [ ] Mock endpoints verified
- [ ] Acceptance / contract tests written in `tests/`
- [ ] Domain & Application layer implementation complete
- [ ] Infrastructure & Presentation adapters complete
- [ ] Verification tests passing
