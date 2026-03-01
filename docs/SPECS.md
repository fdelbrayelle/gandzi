# Gandzi Product Specs (Domain-by-Domain)

Gandzi (from Georgian 🇬🇪 "განძი", treasure) is a privacy-by-default, local-first, self-hosted personal finance product designed as a manual-control-first "Excel on steroids". The product intentionally starts with explicit manual entry to preserve user control and data transparency, while keeping external API integrations as roadmap extensions.

## 1. Product Identity & Core Intent

- Product name: `Gandzi`
- License: Apache-2.0
- Visibility: public repository
- Primary intent: personal financial command center for budgeting, wealth tracking, simulations, and data portability
- Design principles:
  - Privacy-by-design and privacy-by-default
  - Local-first + self-hosted first
  - Domain isolation with hexagonal architecture
  - TDD-first delivery and Conventional Commits

Current repo status:
- Implemented and documented in `README.md`, `AGENTS.md`, `CLAUDE.md`
- Frontend landing and dashboard UX implemented

## 2. Security & Identity (OAuth2)

Spec:
- OAuth2 from day one
- Secure sign-in flow via Keycloak in local/self-hosted environments

Current repo status:
- Backend configured as OAuth2 Resource Server (`SecurityConfig`)
- Frontend login CTA redirects to Keycloak auth endpoint
- Local auth state is persisted client-side for dashboard access and refresh continuity
- Logout currently clears local session state and returns to landing (no full OIDC token/session lifecycle yet)
- Docker Compose includes Keycloak service (`admin/admin`)

## 3. Localization, Currency & Regional Settings

Spec:
- Default language: English
- Broad locale coverage in frontend
- Default timezone: Europe/Paris, configurable
- Supported currencies: EUR, USD, JPY, CNY, INR, GBP, CHF

Current repo status:
- Full locale dictionaries implemented for: `en, fr, es, de, it, pt, zh, ja, hi, ar, ru, ka`
- Preferences store supports locale/timezone/currency and persists to local storage
- Account Settings UI provides language, timezone, currency, date format controls

## 4. Budgeting Domain

Spec:
- Annual budget split month-by-month
- Budget tracking by category and by account
- Rollover support
- Default alert threshold: 80% (configurable)
- Email notifications configurable
- UX goal: editable matrix similar to advanced spreadsheet experience

Current repo status:
- Backend domain and services implemented for budget model, rollover logic, and threshold alert evaluation
- Notification port + logging notifier adapter implemented
- Frontend Annual Budget dashboard section implemented as editable matrix with:
  - monthly editable cells
  - annual/monthly/% rollups
  - free cashflow row
  - sticky headers/labels and modern dark UI
- Email sending integration remains adapter-level placeholder (no full outbound mail workflow yet)

## 5. Wealth Tracking Domain

Spec:
- Track financial wealth, gross wealth, net wealth
- Classify assets by category and holding support/container (e.g., Crypto, PEA, CTO)
- Snapshot frequency configurable (daily/monthly/yearly/on-demand)
- Manual valuation first, with market/API connectors in roadmap

Current repo status:
- Backend wealth domain models implemented:
  - `AssetCategory`, `HoldingSupport`, `SnapshotFrequency`
  - `WealthSnapshot` with gross/net/financial computation methods
- Frontend menu entry and placeholders present
- Full wealth workflows/UI and persistence adapters remain to be expanded

## 6. Real Estate & Mortgage Domain

Spec:
- Primary residence with co-ownership share
- Fixed-rate mortgage support
- Standard monthly amortization
- Outstanding principal tracking

Current repo status:
- Backend domain models implemented for:
  - residence ownership share valuation
  - fixed-rate mortgage monthly payment
  - outstanding principal over time
- End-to-end UI/API flows not yet implemented

## 7. Simulation Domain

Spec:
- Compound interest simulator:
  - preset scenarios + custom rate
  - monthly/quarterly/yearly distribution
  - recurring contributions
- Wealth projection simulator:
  - inflation, income growth, expense growth
  - deterministic + Monte Carlo
  - horizon up to 100 years

Current repo status:
- Backend simulation domain implemented:
  - compound interest engine
  - deterministic wealth projection
  - Monte Carlo projection with percentile outputs
- Frontend "Simulations" menu exists; dedicated interactive simulator screens still pending

## 8. Ledger / Manual Entry Domain

Spec:
- Double-entry bookkeeping mandatory
- Manual add/edit/delete operations
- Default categories + custom categories
- Deletion allowed

Current repo status:
- Backend ledger domain implemented with:
  - balanced double-entry invariant
  - default category enum + custom category support
- Manual entry service and repository port present
- Full persistence adapter and full CRUD API/UI still to be expanded

## 9. Analytics & Charts Domain

Spec:
- Priority charts:
  - wealth type charts
  - budget charts
  - effective expense charts (month/year)
- Filtering by period/account/category
- Export capability

Current repo status:
- Frontend chart components present (`WealthChart`, `BudgetTemplateChart`, `ExpenseChart`)
- Filter utility and export-to-image utility scaffolded
- Full data wiring and complete analytics pages still pending

## 10. Data Portability Domain

Spec:
- Import/export all user data
- Formats: JSON and CSV
- Import mode: merge or replace
- Timestamped export filename
- No export encryption in V1

Current repo status:
- Backend portability domain implemented:
  - format and mode enums
  - merge/replace import behavior
  - timestamped export naming
- UI/API workflow integration still pending

## 11. Account Settings Domain

Spec:
- User-configurable preferences for language, timezone, currencies, alerts, formatting, simulation defaults
- Save action and clear UX feedback

Current repo status:
- Frontend Account Settings screen implemented with persisted preferences:
  - profile display name
  - language, timezone, date format
  - primary/secondary currencies
  - budget alert threshold
  - email notification toggles and email field
  - wealth snapshot frequency
  - default simulation horizon
  - default export format
- Save behavior:
  - button-driven save
  - keyboard shortcut (`Ctrl+S` / `Cmd+S`)
  - in-UI saved notification

## 12. Frontend UX & Design System

Spec:
- Distinct, intentional dark-mode visual identity
- Synthwave-inspired branding
- Avoid generic default UI

Current repo status:
- Design system CSS with dark gradients, glass surfaces, neon accents
- Custom synthwave/pyramid logo with financial symbolism
- Landing page tags aligned with product posture and key features
- Authenticated dashboard with persistent selected section across refresh
- Loading-state handling prevents landing flash during auth resolution

## 13. Architecture & Code Organization

Spec:
- Hexagonal architecture to isolate domain from frameworks
- Clear separation between domain/application/adapters

Current repo status:
- Backend structure follows domain/application/ports/adapters layout
- Domain-heavy implementation exists for major functional areas
- Adapter and API layers are partially implemented and need expansion for full production flows

## 14. Infrastructure, Build & Delivery

Spec:
- PostgreSQL
- Docker and Docker Compose support
- CI in GitHub Actions
- Backend stack: Kotlin + Spring + Gradle + jOOQ + Liquibase
- Frontend stack: Astro + Vue 3 + Pinia + Vue-ChartJS

Current repo status:
- Docker Compose includes `postgres`, `keycloak`, `backend`, `frontend`
- Backend Gradle wrapper added and configured
- GitHub Actions workflow present for backend/frontend checks
- Liquibase changelog + initial schema included
- jOOQ generation configuration included

## 15. Testing Status

Spec:
- TDD-first delivery strategy
- Domain and use-case tests as baseline

Current repo status:
- Backend unit tests exist for domain logic across budget, wealth, real estate, simulation, ledger, portability, money value object
- Frontend tests exist for i18n baseline and chart filtering utility
- Broader integration/e2e coverage remains a planned expansion area

