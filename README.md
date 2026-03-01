# Gandzi (განძი) - Personal Finance Treasure 🏦💎

`Gandzi` (from Georgian `განძი`, "treasure") is a self-hosted personal finance management application focused on rigorous accounting, privacy by design, and long-term wealth intelligence.

## Vision

Build a trustworthy financial cockpit that lets users:
- Track budgets month-by-month over each civil year.
- Monitor financial wealth, gross wealth, and net wealth precisely.
- Visualize spending and wealth trends with high-quality charts.
- Model real-estate ownership and fixed-rate mortgages.
- Run compound-interest and wealth-projection simulations (including Monte Carlo).
- Import/export all personal data in a transparent way.

## Core Principles

- Hexagonal architecture (domain isolated from frameworks).
- TDD-first delivery.
- Privacy by design + GDPR-by-default.
- Self-hosted first, Docker-ready.
- Public OSS project under Apache-2.0.

## Product Scope (V1)

### 1) Budget tracking 📅
- Annual budgets split by month.
- Budget dimensions: by category and by account.
- Rollover support month to month.
- Civil year as default period.
- Default alert threshold at 80% (configurable).
- Email notification channel configurable for budget alerts.

### 2) Wealth tracking 💼
- Financial wealth, gross wealth, net wealth.
- Manual valuation for now.
- Designed to support future market-data adapters.
- Snapshot frequency configurable: daily, monthly, yearly, on-demand.
- Asset classification support:
  - Asset category (e.g. Crypto, Equity, Bond, Cash, Real Estate).
  - Holding container/support (e.g. PEA, CTO, Pension, Savings).

### 3) Charts and analytics 📊
- Dedicated chart per wealth type.
- Budget chart with reusable monthly template.
- Effective-expenses chart for selected month or year.
- Filters by period, account, category, wealth type.
- Chart export support.

### 4) Primary residence & mortgage 🏠
- Register primary residence with co-ownership share (%).
- Fixed-rate mortgage support.
- Standard monthly amortization schedule.
- Track outstanding principal (capital restant du).

### 5) Compound-interest simulator 🧮
- 3 scenarios (conservative/base/optimistic) + custom fixed rate.
- Duration and expected return inputs.
- Distribution/frequency: monthly, quarterly, yearly.
- Optional recurring contributions.

### 6) Wealth projection simulator 🚀
- Include inflation, income growth, and expense evolution.
- Deterministic mode + Monte Carlo mode.
- Horizon up to 100 years.

### 7) Manual accounting inputs ✍️
- Mandatory double-entry model.
- Manual add/edit/delete operations for expenses, income, wealth adjustments.
- Custom categories allowed.
- Built-in default categories:
  - groceries, transport, health, gifts, vacation,
  - home insurance, borrower insurance, vehicle insurance,
  - taxes, personal care, car maintenance,
  - subscriptions, investments (with asset-type subcategories),
  - bank fees, mortgage, rent, property tax,
  - electricity, gas, water, miscellaneous, leisure.

### 8) Full data import/export 💾
- JSON and CSV export/import.
- Import mode: merge or full replace.
- No encryption in V1.
- Export filenames include precise timestamp.

## Technical Stack

### Backend
- Java 25
- Kotlin 2.x
- Spring Boot 4.0.3
- Gradle (Kotlin DSL)
- jOOQ
- Liquibase
- OAuth2 Resource Server (enabled from day one)
- Hexagonal architecture (`domain`, `application`, `ports`, `adapters`)

### Frontend
- Astro
- Vue 3
- Pinia
- Vue-ChartJS
- i18n strategy: lightweight dictionary-based translation layer, English default, easy extension to major world languages.

### Data & Infra
- PostgreSQL
- Docker + Docker Compose for local/self-hosted deployment
- GitHub Actions CI from start

## Internationalization (i18n) 🌍

- Default UI language: English.
- Frontend ships with a simple extensible locale registry (`en`, `fr`, `es`, `de`, `it`, `pt`, `zh`, `ja`, `hi`, `ar`, `ru`).
- Locale can be user-configured and persisted.

## Currencies & Timezone

Supported currencies in V1:
- EUR, USD, JPY, CNY, INR, GBP, CHF

Default timezone:
- `Europe/Paris` (user-configurable)

## Security, Privacy, GDPR 🔐

- Data minimization by default.
- Explicit purpose limitation in domain services.
- User data export + delete capabilities planned as first-class use-cases.
- Clear separation between operational, analytics, and audit data.
- Strict adapter boundaries to prevent domain leakage.

## Development Approach

- TDD required for all feature increments.
- One feature per commit (Conventional Commits).
- Strong domain tests + adapter integration tests + frontend component tests.

## Repository Structure

- `backend/` Kotlin + Spring Boot + Hexagonal + jOOQ + Liquibase
- `frontend/` Astro + Vue + Pinia + Charting + i18n
- `docs/` architecture, GDPR, ADRs
- `.github/workflows/` CI workflows

## License

Apache License 2.0.
