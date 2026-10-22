# ⚙️ Functional Requirements Specification (FRS)

## 1. Overview
High-level description of the domain, system goals, and target users.

---

## 2. User Roles & Actors
- **Admin**: Has administrative rights over resources and configuration.
- **User**: Standard authenticated consumer.
- **External System**: Third-party service or webhook consumer.

---

## 3. Functional Requirements Catalog

### RF-01: Resource Creation
- **Description**: The system must allow authorized users to create a new resource with validation.
- **Input**: Resource name (required, 1-100 characters), description (optional).
- **Output**: Created resource with generated UUID and timestamp.
- **Business Rules**:
  - Duplicate resource names in the same workspace are rejected with a 409 Conflict.
- **Associated Acceptance Criteria**: See [`specs/acceptance/features/`](../acceptance/features/).

### RF-02: Paginated Resource Retrieval
- **Description**: The system must allow users to retrieve lists of resources with pagination.
- **Input**: `page` (default 1), `limit` (default 20, max 100).
- **Output**: Array of resources, total count, current page, and limit.

### RF-03: System Health Monitoring
- **Description**: Expose an endpoint reporting operational health status of core dependencies (database, cache).

---

## 4. Traceability Matrix

| Requirement ID | API Endpoint | BDD Feature | Architecture Component |
|:---|:---|:---|:---|
| RF-01 | `POST /items` | `specs/acceptance/features/item_creation.feature` | `src/application/use-cases/create-item.ts` |
| RF-02 | `GET /items` | `specs/acceptance/features/item_listing.feature` | `src/application/use-cases/list-items.ts` |
| RF-03 | `GET /health` | N/A | `src/presentation/controllers/health.controller.ts` |
