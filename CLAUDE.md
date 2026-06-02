# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Project purpose

Demo for a talk on **Spec-Driven Development with the AI Unified Process
(AIUP)**. Re-implements the classic Spring PetClinic by writing the specs
first (`docs/`) and generating code against them.

**`docs/capabilities/` is the source of truth, not the code.** When asked to
implement something, read the relevant spec first:

- `docs/capabilities/<capability>/entity_model.md` — capability-local domain model slice.
- `docs/capabilities/<capability>/activities/<activity>/use-cases/<use-case-id>/uc.md` — JIRA-backed
  use-case specification with preconditions, scenarios, postconditions, business
  rules, field labels, and navigation.
- `docs/capabilities/<capability>/activities/<activity>/use-cases/<use-case-id>/uc.feature`
  — executable Cucumber scenarios. The `Feature:` name matches the use-case
  id exactly.
- `docs/capabilities/<capability>/activities/<activity>/use-cases/<use-case-id>/uc.puml` — PlantUML use-case and
  aggregate-interaction diagram.

`uc.md` records the JIRA ticket id. The use-case id is the dash-separated folder
name, e.g. `add-pet-to-owner`. The Gherkin `Feature:` name equals the use-case
id; each `Scenario:` is one business-relevant flow or supported transition
inside that use case.

If a use case and the code disagree, the use case wins unless the user says
otherwise.

## Traceability policy

Use this chain for future implementation work:

```text
Software Capability
  -> Software Activity
    -> Use Case / Gherkin Feature
      -> Gherkin Scenario
        -> Cucumber Step Definition
          -> Application Service
            -> Domain Model
```

The Domain Model includes aggregate roots, child entities, value objects,
repository ports, domain events, and optional domain services. Add a domain
service only when a business rule does not belong naturally inside one aggregate
root. An aggregate root is itself an entity.

Capabilities define strategic boundaries, activities define behavioral
boundaries, use cases define delivery boundaries, and scenarios define executable
flow examples.

## Stack

- **Java 25**, **Spring Boot 4.0.5**, **Spring Modulith**, **Vaadin 25.1**
- **jOOQ** for type-safe SQL — generated sources live in
  `target/generated-sources/jooq` under package
  `ai.unifiedprocess.demo.petclinic.database`
- **Flyway** migrations in `src/main/resources/db/migration`
  (`V1__initial_schema.sql` covers the full entity model; test-only seed
  data lives in `src/test/resources/db/migration/V2__seed_reference_data.sql`)
- **PostgreSQL** in prod; **Testcontainers** (`postgres:17-alpine`) for
  tests *and* for jOOQ code generation at build time

## Commands

```bash
# Run the app locally (Testcontainers-backed Postgres via TestAiupPetclinicApplication)
./mvnw spring-boot:test-run

# Full build — also runs jOOQ codegen against a throwaway Testcontainers Postgres
./mvnw verify

# Run all tests (Docker must be running)
./mvnw test

# Run a single test class / method
./mvnw test -Dtest=UC004FindOwnersByLastNameTest
./mvnw test -Dtest=UC004FindOwnersByLastNameTest#singleMatchNavigatesDirectlyToDetails

# Regenerate jOOQ sources after changing a Flyway migration
./mvnw generate-sources
```

Docker must be running for `test`, `verify`, and `generate-sources`. **If
you add or change a migration, jOOQ classes won't update until you re-run
`generate-sources` (or any later phase).**

## When to read the detailed guides

- **Before implementing a use case** → read the corresponding
  `docs/capabilities/<capability>/activities/<activity>/use-cases/<use-case-id>/` folder first. It defines
  the JIRA ticket(s), Gherkin scenarios, preconditions, main success scenario,
  alternative flows, postconditions, business rules, field labels, and
  navigation. The spec is the source of truth.

- **Before implementing a use case, writing a view, adding a repository, or
  touching anything in `src/main/java/`** → read
  [`docs/guidelines/architecture.md`](docs/guidelines/architecture.md)
  first. It covers package layout, use-case REST resources, DDD aggregate
  rules, jOOQ persistence-object mapping patterns, Vaadin view conventions,
  the shell exception, form validation, error handling, and the repository
  stereotype rule.

- **Before writing or modifying any test under `src/test/java/`** → read
  [`docs/guidelines/testing.md`](docs/guidelines/testing.md) first. It
  covers `SpringBrowserlessTest`, the `UC<NNN><Name>Test` naming rule, the
  `@UseCase` annotation, `PetClinicTestBase`, seed-data conventions,
  locator patterns, and what you must **not** do (no public test getters,
  no `assertNotNull` on fields, no nested field reach-in, no Karibu).

Do not skip these. Both files are short and kept current — drift between
them and the code is a bug to fix, not a style preference to ignore.

## Skills available for this project

Prefer these over ad-hoc generation:

- `aiup-core:entity-model`, `aiup-core:use-case-spec`,
  `aiup-core:use-case-diagram`, `aiup-core:requirements` — authoring/updating
  specs in `docs/`.
- `aiup-vaadin-jooq:flyway-migration` — generate `V*.sql` from the entity
  model.
- `aiup-vaadin-jooq:implement` — implement a use case end-to-end (view +
  jOOQ queries). Already honours `docs/guidelines/architecture.md`.
- `aiup-vaadin-jooq:browserless-test` — server-side Vaadin view tests
  (`SpringBrowserlessTest`, `$()` locators). Default for UC tests; see
  `docs/guidelines/testing.md`.
- `aiup-vaadin-jooq:playwright-test` — browser-based end-to-end tests. For
  server-side tests, see `docs/guidelines/testing.md` (the `karibu-test`
  skill is obsolete).
