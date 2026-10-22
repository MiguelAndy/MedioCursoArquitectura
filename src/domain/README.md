# 🏛️ Domain Layer

The **Domain Layer** contains the core business rules, entities, and domain exceptions.

## Principles:
- **Zero External Dependencies**: This layer does NOT depend on frameworks, databases, HTTP libraries, or presentation code.
- **Driven by Specifications**: Directly reflects the entities defined in `specs/data-models/` and business invariants in `specs/requirements/`.
- **Ports (Interfaces)**: Defines repository interfaces and external service contracts that outer layers must implement.

## Structure:
- `entities/`: Core business models with state and behaviors.
- `value-objects/`: Immutable objects defined by attributes (e.g., Email, Money, UUID).
- `repositories/`: Repository interfaces (e.g., `item.repository.interface`).
- `exceptions/`: Domain-specific business exceptions.
