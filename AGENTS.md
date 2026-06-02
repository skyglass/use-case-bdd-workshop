# AGENTS.md

Guidance for Codex when working in this repository.

## Source Of Truth

Use `docs/modules/` first:

- `docs/modules/<module>/entity_model.md` - domain model slice.
- `docs/modules/<module>/<use-case-id>/UC-*.md` - JIRA epic files. `UC-*` is the JIRA ticket id.
- `docs/modules/<module>/<use-case-id>/scenarios.feature` - executable scenarios.
- `docs/modules/<module>/<use-case-id>/use_cases.puml` - PlantUML use-case/aggregate-interaction diagram.

The use-case id is the dash-separated folder name. The Gherkin `Feature:` name must match it exactly.

Legacy files in `docs/use_cases/`, `docs/entity_model.md`, and `docs/use_cases.puml` are compatibility references.

## Implementation Workflow

When implementing or refining a use case:

1. Read the module `entity_model.md`.
2. Read all `UC-*.md` JIRA ticket files in the use-case folder.
3. Read `scenarios.feature` and `use_cases.puml`.
4. Update or add Gherkin scenarios first when behavior changes.
5. Implement the slice through:
   - Cucumber step definition
   - REST resource in `<module>/api` with URL shape `/api/<module>/<use-case-id>/<command>`
   - application service in `<module>/application`
   - domain service or aggregate rule in `<module>/domain`
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
- Cucumber scenarios selected from `docs/modules`

## Commands

```bash
./mvnw spring-boot:test-run
./mvnw test
./mvnw test -Dtest=UC004FindOwnersByLastNameTest
./mvnw generate-sources
```

Docker must be running for tests, verification, and jOOQ code generation.

Use-case naming rule: dash-case in docs/Gherkin/REST paths, CamelCase in Java classes, snake_case in database tables.

## Project Skill

Prefer the custom skill:

```text
Use $petclinic-use-case-ddd to implement or refine a PetClinic use case from docs/modules.
```

Skill location: `custom-skills/petclinic-use-case-ddd`.
