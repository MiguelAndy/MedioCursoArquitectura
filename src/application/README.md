# ⚡ Application Layer

The **Application Layer** orchestrates domain objects and implements the specific use-cases defined in `specs/requirements/functional-requirements.md`.

## Principles:
- **Orchestration**: Coordinates domain entities and invokes repository interfaces to achieve user goals.
- **Independent of UI & Transport**: Does not know whether it is called from an HTTP REST API, a GraphQL query, or a CLI script.
- **DTOs (Data Transfer Objects)**: Maps request data into domain inputs and returns structured response objects matching API schemas.

## Structure:
- `use-cases/`: Specific business flows (e.g., `create-item.use-case`, `get-item-by-id.use-case`).
- `dtos/`: Input and output data structures.
- `ports/`: Service interfaces (e.g., email notification port, payment port).
