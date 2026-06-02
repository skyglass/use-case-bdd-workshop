# AIUP PetClinic

A demo project for **Spec-Driven Development with the AI Unified Process (AIUP)**, implemented as a DDD-oriented modular monolith.

The project revisits the classic Spring PetClinic sample. The implementation is driven by capability-local use-case specifications, Gherkin scenarios, and thin Spring Modulith application services.

## Stack

- **Java 25**
- **Spring Boot 4.0**
- **Spring Modulith** - logical application modules
- **Vaadin 25** - UI
- **jOOQ** - type-safe SQL
- **Flyway** - database migrations
- **Cucumber JVM** - executable use-case scenarios
- **PostgreSQL** via Testcontainers for tests

## Specification Model

The DDD source of truth lives in [`docs/modules`](docs/modules). Top-level folders are Business Capabilities. Their
child folders are Business Activities:

- `docs/modules/<business-capability>/entity_model.md` - capability domain model slice.
- `docs/modules/<business-capability>/<business-activity>/<use-case-id>/uc.md` - JIRA-backed use-case specification.
- `docs/modules/<business-capability>/<business-activity>/<use-case-id>/uc.feature` - Cucumber scenarios.
- `docs/modules/<business-capability>/<business-activity>/<use-case-id>/uc.puml` - PlantUML use-case/aggregate-interaction diagram.

Important naming rule:

- `uc.md` records the JIRA ticket id, for example `UC-007`.
- The use-case id is the dash-separated folder name, for example `add-pet-to-owner`.
- The Gherkin `Feature:` name must exactly match the use-case id. A `Scenario:` is one flow or supported transition
  inside that use case.

## Traceability Chain

Enterprise business value is traced through this chain:

```text
Business Capability
  -> Business Activity
    -> Use Case / Gherkin Feature
      -> Gherkin Scenario
        -> Cucumber Step Definition
          -> Application Service
            -> Domain Model
```

The Domain Model includes aggregate roots, child entities, value objects, repository ports, domain events, and optional
domain services. Add a domain service only when a business rule does not belong naturally inside one aggregate root.

Legacy specs remain under [`docs/use_case_history`](docs/use_case_history), [`docs/entity_model.md`](docs/entity_model.md), and [`docs/use_cases.puml`](docs/use_cases.puml) as compatibility references.

## Business Capabilities

| Business Capability | Business Activity | Use cases |
|---------------------|-------------------|-----------|
| `clinic-portal` | `clinic-experience` | `view-welcome-page`, `view-application-error` |
| `vet-catalog` | `veterinary-directory` | `view-veterinarians` |
| `owner-care` | `owner-management` | `register-new-owner`, `find-owners-by-last-name`, `view-owner-details`, `update-owner` |
| `owner-care` | `pet-management` | `add-pet-to-owner`, `update-pet` |
| `owner-care` | `visit-management` | `book-visit-for-pet` |

## Use-Case REST API

REST resources are grouped by use case, not CRUD table names. The URL shape is
`/api/<business-activity>/<use-case-id>/<command>`, where `<use-case-id>` is the same dash-separated id used by the docs
folder and Gherkin `Feature:`.

Examples:

- `POST /api/owner-management/register-new-owner/register`
- `GET /api/owner-management/find-owners-by-last-name/search?prefix=Davis`
- `GET /api/owner-management/view-owner-details/view/6`
- `POST /api/pet-management/add-pet-to-owner/add`
- `PUT /api/pet-management/update-pet/update/6/8`
- `POST /api/visit-management/book-visit-for-pet/book`

Each REST resource delegates to the matching `*UseCase` application service. Persistence follows the natural aggregate
model, for example `AddPetToOwnerResource` -> `AddPetToOwnerUseCase` -> `OWNER` aggregate with `PET` entities.

## Codex Skill

Use the project skill when changing specs, scenarios, or implementation:

```text
Use $petclinic-use-case-ddd to implement or refine a PetClinic use case from docs/modules.
```

The skill lives at [`custom-skills/petclinic-use-case-ddd`](custom-skills/petclinic-use-case-ddd) and captures the capability/activity/use-case/scenario/application-service workflow.

Codex project guidance is also available in [`AGENTS.md`](AGENTS.md). `CLAUDE.md` is kept for Claude Code compatibility.

## Running

```bash
./mvnw spring-boot:test-run
```

Tests use Testcontainers and need Docker running:

```bash
./mvnw test
```

After changing Flyway migrations, regenerate jOOQ sources:

```bash
./mvnw generate-sources
```

## Structure

```text
docs/modules/       - DDD capability/activity specifications, JIRA tickets, and Gherkin scenarios
custom-skills/      - Codex custom skills for this project
src/main/java/      - Spring Modulith modules, REST resources, application services, domain, infrastructure, Vaadin UI
src/test/java/      - Cucumber steps, Modulith verification, and browserless Vaadin tests
```
