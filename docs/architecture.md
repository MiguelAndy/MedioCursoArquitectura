# 📐 Software Architecture Specification

## 1. Architectural Style: Clean / Hexagonal (Ports & Adapters)

This project follows Clean Architecture principles, ensuring that business rules are independent of frameworks, databases, and user interfaces.

```mermaid
graph TD
    subgraph Presentation ["Presentation Layer (Controllers, Routes)"]
        HTTP[HTTP REST API / OpenAPI]
    end

    subgraph Application ["Application Layer (Use Cases)"]
        UC[Use Cases & DTOs]
    end

    subgraph Domain ["Domain Layer (Core)"]
        E[Entities]
        VO[Value Objects]
        P[Repository Interfaces / Ports]
    end

    subgraph Infrastructure ["Infrastructure Layer (Adapters)"]
        DB[(Database / Persistence)]
        EXT[External Services]
    end

    HTTP --> UC
    UC --> Domain
    Infrastructure -->|Implements Ports| Domain
```

## 2. Dependency Rule
Dependencies only point inward:
- **Presentation** depends on **Application**.
- **Application** depends on **Domain**.
- **Infrastructure** depends on **Domain** (Dependency Inversion).
- **Domain** depends on **nothing**.

## 3. Spec-First Alignment
Every component in the architecture maps directly to an approved specification:
- Endpoints in Presentation map to `specs/api/openapi.yaml`.
- Use Cases in Application map to `specs/requirements/functional-requirements.md`.
- Entities in Domain map to `specs/data-models/schemas/`.
- Architectural shifts are recorded in `specs/adrs/`.
