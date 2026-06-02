---
name: petclinic-use-case-ddd
description: Implement or refine AIUP PetClinic use cases using the repository's DDD, Spring Modulith, BDD, Vaadin, jOOQ, and module-documentation conventions. Use when Codex is asked to add or change a PetClinic use case, update docs/modules, generate or maintain Gherkin scenarios, create Cucumber step definitions, or implement Spring Modulith application services from JIRA-style UC-*.md files.
---

# PetClinic Use Case DDD

## Source Of Truth

Treat `docs/modules/` as the primary source of truth:

- `docs/modules/<module>/entity_model.md` describes the module's domain model slice.
- `docs/modules/<module>/<use-case-id>/UC-*.md` contains JIRA epic tickets that define or refine the use case. `UC-*` is the JIRA ticket id, not the use-case id.
- `docs/modules/<module>/<use-case-id>/scenarios.feature` contains executable scenarios.
- `docs/modules/<module>/<use-case-id>/use_cases.puml` contains the PlantUML use-case and aggregate-interaction diagram.
- The use-case id is the dash-separated folder name, and the Gherkin `Feature:` name must match it exactly.

Legacy files under `docs/use_cases/`, `docs/entity_model.md`, and `docs/use_cases.puml` are compatibility references. Update module-local specs first.

## Workflow

1. Identify the target module and use-case id from `docs/modules/`.
2. Read the module `entity_model.md`, all `UC-*.md` JIRA tickets in the use-case folder, `scenarios.feature`, and `use_cases.puml`.
3. Read `docs/guidelines/architecture.md` before changing production code and `docs/guidelines/testing.md` before changing tests.
4. Implement behavior in this order:
   - Gherkin scenario
   - Cucumber step definition
   - REST resource in `<module>/api` with URL shape `/api/<module>/<use-case-id>/<command>`
   - Application service in `<module>/application`
   - Domain service or aggregate logic in `<module>/domain` when business rules are not pure orchestration
   - Repository port in `<module>/domain`
   - jOOQ repository adapter and `*PO` persistence object in `<module>/infrastructure`
   - Vaadin view in `<module>/ui`
5. Keep use-case application services thin but explicit. They own command/result shapes, transaction boundaries, and the use-case lifecycle.
6. Keep domain rules out of step definitions and Vaadin click handlers. Step definitions exercise application services; views delegate use-case work to application services.
7. Run relevant Maven tests. If migrations changed, regenerate jOOQ sources first.

## Module Rules

- Modules are Spring Modulith application modules under direct subpackages of `ai.unifiedprocess.petclinic`.
- Current modules: `core`, `welcome`, `vet`, `owner`, `pet`, `visit`.
- Prefer dependencies on another module's application/domain API over reaching into UI internals.
- The app shell may use route strings for navigation to avoid unnecessary UI type dependencies.
- Keep repositories named `*Repository` and annotated `@Repository`.
- Use jOOQ `Records.mapping(TypePO::new)` for persistence-object mapping; do not map jOOQ rows directly into domain
  aggregate roots.
- Use-case naming: dash-case in docs/Gherkin/REST paths, CamelCase in Java classes, snake_case in database tables.

## BDD Rules

- Every use-case folder has exactly one `scenarios.feature` file unless the use case grows large enough to justify splitting.
- The `Feature:` name equals the use-case id, for example `Feature: add-pet-to-owner`.
- Tags may reference JIRA tickets such as `@UC-007`.
- Step definitions must call application services or domain services, not duplicate business logic.
- Use test-only Spring beans under `src/test/java` for fixture APIs that prepare or clean data and are not part of the
  production application API.
- Keep Cucumber fixture setup idempotent and roll back or clean scenario data so feature runs do not depend on execution
  order or previous database state.
- Use Cucumber scenarios for business behavior and JUnit/Vaadin browserless tests for UI rendering and navigation details.

## Reporting

When finished, report:

- Modules and use cases changed.
- JIRA tickets/scenarios updated.
- Application services/domain services added or changed.
- Tests run and any remaining gaps.
