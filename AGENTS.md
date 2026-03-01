# Gandzi Project Memory

This file stores durable product and technical decisions for contributors and coding agents.

## Product Identity
- Name: Gandzi (განძი, "treasure")
- Deployment target: local development first, self-hosted, Docker/Compose ready
- Repository visibility: public
- License: Apache-2.0

## Architecture & Engineering Rules
- Hexagonal architecture mandatory.
- TDD mandatory.
- One feature per commit.
- Conventional Commits required.
- Privacy by design and GDPR considerations from the start.

## Backend Stack
- Java 25
- Kotlin 2.x
- Spring Boot 4.0.3
- Gradle (Kotlin DSL)
- Liquibase for schema migrations
- jOOQ for typed SQL access
- OAuth2 enabled from day one
- Monouser start, architecture prepared for future multi-user

## Frontend Stack
- Astro + Vue 3
- Pinia for state management
- Vue-ChartJS for charts
- i18n: English default with extensible major-language dictionaries

## Functional Decisions
- Budget: yearly, month-by-month, by category and account, rollover enabled.
- Alert threshold default: 80%, configurable.
- Notification channel: email (configurable).
- Wealth snapshots: daily/monthly/yearly/on-demand.
- Asset model must include:
  - asset category (e.g. Crypto)
  - holding support/container (e.g. PEA, CTO)
- Real estate:
  - primary residence with co-ownership share
  - fixed-rate mortgage
  - standard monthly amortization
- Simulators:
  - compound interest with scenarios + custom rate
  - wealth projection deterministic + Monte Carlo
  - horizon up to 100 years
- Manual accounting:
  - strict double-entry
  - default categories + custom categories
  - deletion allowed
- Import/export:
  - JSON and CSV
  - merge or replace mode
  - timestamped filename

## Localization & Preferences
- Default language: English
- Target locales include: en, fr, es, de, it, pt, zh, ja, hi, ar, ru
- Default timezone: Europe/Paris (configurable)
- Supported currencies: EUR, USD, JPY, CNY, INR, GBP, CHF
