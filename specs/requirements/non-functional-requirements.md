# 🛡️ Non-Functional Requirements Specification (NFRS)

Documenting Quality Attributes based on the **ISO/IEC 25010** software quality standard.

---

## 1. Performance Efficiency
- **RNF-01 (Response Time)**: 95% of read requests (`GET /items`) must respond in less than 200 ms under normal load (50 concurrent users).
- **RNF-02 (Throughput)**: The service must sustain at least 200 requests per second (RPS) without error spikes.

---

## 2. Security
- **RNF-03 (Authentication & Authorization)**: Protected endpoints must enforce token validation (e.g., JWT).
- **RNF-04 (Data Sanitization & Injection Prevention)**: All inputs must be strictly validated against domain schemas before processing.
- **RNF-05 (Encryption in Transit)**: All communication must be encrypted via TLS 1.3 / HTTPS.

---

## 3. Reliability & Availability
- **RNF-06 (Uptime)**: 99.9% availability during business hours.
- **RNF-07 (Graceful Degradation)**: If external services fail, the system must return meaningful error codes rather than crashing.

---

## 4. Maintainability & Architecture
- **RNF-08 (Clean Architecture)**: Domain business logic must not depend on database or presentation frameworks (Dependency Inversion Principle).
- **RNF-09 (Spec Traceability)**: 100% of exposed endpoints must have an active schema in `specs/api/openapi.yaml`.
- **RNF-10 (Test Coverage)**: Core domain logic and use cases must maintain minimum 80% test coverage.
