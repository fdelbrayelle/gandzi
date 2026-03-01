# Gandzi Architecture

## Style
Hexagonal architecture:
- Domain: pure business rules, no framework dependencies.
- Application: use-cases and ports.
- Adapters In: REST/API/UI integration.
- Adapters Out: persistence, notifications, import/export, external APIs.

## Bounded Contexts
- Identity & Settings
- Ledger
- Budgeting
- Wealth Tracking
- Real Estate & Mortgage
- Simulation Engine
- Analytics & Visualization
- Data Portability

## Test Strategy
- Domain unit tests first.
- Use-case tests first.
- Adapter integration tests.
- Frontend component/store tests.
