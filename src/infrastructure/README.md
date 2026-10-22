# 🔌 Infrastructure Layer

The **Infrastructure Layer** implements the interfaces (ports) defined by the Domain and Application layers using concrete technologies.

## Principles:
- **Adapters**: Implements persistence (PostgreSQL, MongoDB, In-Memory), cache (Redis), message queues (RabbitMQ, Kafka), and third-party APIs.
- **Dependency Inversion**: Outer layers adapt to domain interfaces, keeping domain code free of vendor lock-in.

## Structure:
- `database/`: Database connection configurations, ORM/query adapters, repository implementations.
- `external-services/`: Clients for external REST APIs or SDKs.
- `logging/`: Structured logging, telemetry, and observability adapters.
