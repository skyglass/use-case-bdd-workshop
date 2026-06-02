---
name: petclinic-use-case-ddd
description: Implement or refine AIUP PetClinic use cases using the repository's DDD, Spring Modulith, BDD, Vaadin, jOOQ, and capability/activity documentation conventions. Use when Codex is asked to add or change a PetClinic use case, update docs/modules, generate or maintain Gherkin scenarios, create Cucumber step definitions, or implement Spring Modulith application services from uc.md use-case specs.
---

# PetClinic Use Case DDD

## Source Of Truth

Treat `docs/modules/` as the primary source of truth:

- `docs/modules/<business-capability>/entity_model.md` describes the capability's domain model slice.
- `docs/modules/<business-capability>/<business-activity>/<use-case-id>/uc.md` contains the JIRA-backed use-case specification. The JIRA ticket id is recorded inside this file.
- `docs/modules/<business-capability>/<business-activity>/<use-case-id>/uc.feature` contains executable scenarios.
- `docs/modules/<business-capability>/<business-activity>/<use-case-id>/uc.puml` contains the PlantUML use-case and aggregate-interaction diagram.
- The use-case id is the dash-separated folder name, and the Gherkin `Feature:` name must match it exactly.
- A Gherkin `Scenario:` is one business-relevant flow or supported transition inside that use case.

Legacy files under `docs/use_case_history/`, `docs/entity_model.md`, and `docs/use_cases.puml` are compatibility references. Update capability-local specs first.

## Workflow

1. Identify the target Business Capability, Business Activity, and use-case id from `docs/modules/`.
2. Read the capability `entity_model.md`, `uc.md`, `uc.feature`, and `uc.puml`.
3. Read `docs/guidelines/architecture.md` before changing production code and `docs/guidelines/testing.md` before changing tests.
4. Implement behavior in this order:
   - Gherkin scenario
   - Cucumber step definition
   - REST resource in the owning code package with URL shape `/api/<business-activity>/<use-case-id>/<command>`
   - Application service in `<module>/application`
   - Domain model behavior in `<module>/domain`
   - Domain service only when a business rule does not belong naturally inside one aggregate root
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
- A Business Capability is an ownership boundary, not one concrete workflow.
- A good use case is a Business Activity workflow that delivers a clear business outcome.
- Business Activities group related use cases and may own shared policies, domain services, events, or projections.
  They do not require an application service by themselves; application services remain use-case boundaries.
- Traceability chain: Business Capability -> Business Activity -> Use Case / Gherkin Feature -> Gherkin Scenario ->
  Cucumber Step Definition -> Application Service -> Domain Model.
- The Domain Model includes aggregate roots, child entities, value objects, repository ports, domain events, and optional
  domain services. An aggregate root is itself an entity.
- Use Event Storming to reveal aggregate boundaries: commands and events show what changes state, which rules must be
  consistent immediately, and where each transaction can end.

## BDD Rules

- Every use-case folder has exactly one `uc.feature` file unless the use case grows large enough to justify splitting.
- The `Feature:` name equals the use-case id, for example `Feature: add-pet-to-owner`.
- A `Scenario:` is not the use case; it is one flow or supported transition inside the use case.
- Tags may reference JIRA tickets such as `@UC-007`.
- Step definitions must call application services for use-case behavior. They may use test-fixture APIs for setup, but
  must not duplicate business logic or bypass the application-service boundary.
- Use test-only Spring beans under `src/test/java` for fixture APIs that prepare or clean data and are not part of the
  production application API.
- Keep Cucumber fixture setup idempotent and roll back or clean scenario data so feature runs do not depend on execution
  order or previous database state.
- Use Cucumber scenarios for business behavior and JUnit/Vaadin browserless tests for UI rendering and navigation details.

## Reporting

When finished, report:

- Business Capabilities, Business Activities, and use cases changed.
- JIRA tickets/scenarios updated.
- Application services/domain services added or changed.
- Tests run and any remaining gaps.
