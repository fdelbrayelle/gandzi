# Privacy by Design and GDPR Notes

## Principles
- Data minimization: collect only what is required for accounting and projections.
- Purpose limitation: personal finance management only.
- Storage limitation: exports and logs should have explicit retention policies.
- Integrity and confidentiality: OAuth2-protected APIs and strict access boundaries.

## Controls in V1
- OAuth2 resource server enabled by default.
- Full data export support planned as core domain capability.
- Data import supports merge/replace to maintain user control.
- Auditability via immutable accounting entries where feasible.

## Next steps
- Add data subject request workflows (export/delete).
- Add consent record model for optional analytics.
- Add encryption-at-rest deployment notes.
