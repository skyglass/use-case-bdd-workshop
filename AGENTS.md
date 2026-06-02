# AGENTS.md

Guidance for Codex when working in this repository.

## Source Of Truth

Use `docs/capabilities/` first:

- `docs/capabilities/<capability>/entity_model.md` - capability domain model slice.
- `docs/capabilities/<capability>/activities/<activity>/use-cases/<use-case-id>/uc.md` - JIRA-backed use-case specification.
- `docs/capabilities/<capability>/activities/<activity>/use-cases/<use-case-id>/uc.feature` - executable scenarios.
- `docs/capabilities/<capability>/activities/<activity>/use-cases/<use-case-id>/uc.puml` - PlantUML use-case/aggregate-interaction diagram.

The use-case id is the dash-separated folder name. The Gherkin `Feature:` name must match it exactly.
A Gherkin `Scenario:` is one business-relevant flow or supported transition inside that use case.

Legacy files in `docs/use_case_history/`, `docs/entity_model.md`, and `docs/use_cases.puml` are compatibility references.

## Implementation Workflow

When implementing or refining a use case:

1. Read the Software Capability `entity_model.md`.
2. Read `uc.md`.
3. Read `uc.feature` and `uc.puml`.
4. Update or add Gherkin scenarios first when behavior changes.
5. Implement the slice through:
   - Cucumber step definition
   - REST resource in the owning code package with URL shape `/api/<activity>/<use-case-id>/<command>`
   - application service in `<module>/application`
   - domain model behavior in `<module>/domain`
   - domain service only when the rule does not belong naturally inside one aggregate root
   - repository port in `<module>/domain`
   - jOOQ repository adapter and `*PO` persistence object in `<module>/infrastructure`
   - Vaadin UI in `<module>/ui`
6. Run relevant tests.

## Stack

- Java 25, Spring Boot 4.0.5, Spring Modulith
- Vaadin 25.1
- jOOQ generated package: `ai.unifiedprocess.demo.petclinic.database`
- Flyway migrations in `src/main/resources/db/migration`
- Test seed data in `src/test/resources/db/migration/V2__seed_reference_data.sql`
- Cucumber scenarios selected from `docs/capabilities`

## Commands

```bash
./mvnw spring-boot:test-run
./mvnw test
./mvnw test -Dtest=UC004FindOwnersByLastNameTest
./mvnw generate-sources
```

Docker must be running for tests, verification, and jOOQ code generation.

Use-case naming rule: dash-case in docs/Gherkin/REST paths, CamelCase in Java classes, snake_case in database tables.
Software Capability is the strategic goal boundary: what this software can fundamentally achieve.
Software Activity is the behavioral boundary: how related use cases are organized to produce value.
Use Case is the delivery boundary: the smallest independently valuable goal.
Scenario is the executable flow: how that goal is achieved in one specific situation.
Activities may own shared policies, domain services, events, or projections. They do not require an application service
by themselves; application services remain use-case boundaries.

Traceability rule: Software Capability -> Software Activity -> Use Case / Gherkin Feature -> Gherkin Scenario ->
Cucumber Step Definition -> Application Service -> Domain Model. The Domain Model includes aggregate roots, child
entities, value objects, repository ports, domain events, and optional domain services. An aggregate root is itself an
entity.

Event Storming rule: commands and events show what changes state, which rules must be consistent immediately, and where
each transaction can end.

## Project Skill

Prefer the custom skill:

```text
Use $petclinic-use-case-ddd to implement or refine a PetClinic use case from docs/capabilities.
```

Skill location: `custom-skills/petclinic-use-case-ddd`.
