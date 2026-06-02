# Business Capability Specification Index

This directory is the DDD-oriented specification root. Top-level folders represent Business Capabilities. A Business
Capability owns the capability-level domain model and groups regular Business Activities that create business value
inside that capability.

Each Business Capability owns:

- `entity_model.md` - the capability domain model slice.
- `<business-activity>/<use-case-id>/uc.md` - JIRA-backed use-case specification.
- `<business-activity>/<use-case-id>/uc.feature` - executable Gherkin scenarios for that use case.
- `<business-activity>/<use-case-id>/uc.puml` - PlantUML use-case/aggregate-interaction diagram.

Use-case folder names are short dash-separated ids. The `Feature:` name in each `uc.feature` file matches the
folder name exactly. A `Scenario:` is one business-relevant flow or supported transition inside that use case.

## Business Hierarchy

| Level | Meaning |
|-------|---------|
| Business Capability | Business-owned ability that groups regular value-producing activities and is a candidate Spring Modulith application-module boundary. It is an ownership boundary, not one concrete workflow. |
| Business Activity | Regular value-producing business activity inside a capability. It groups related use cases and may own shared policies, domain services, events, or projections. |
| Use Case / Gherkin Feature | Business Activity workflow that delivers a clear business outcome. It is described or refined by `uc.md`, which records the JIRA ticket id. The `Feature:` name equals the use-case id. This is the application-service and REST-resource boundary. |
| Gherkin Scenario | Executable example for a supported transition or flow inside the use case. |
| Cucumber Step Definition | Test adapter that translates scenario language into application-service calls and test-fixture API calls. |
| Application Service | Transaction and orchestration boundary for the use case. |
| Domain Model | Aggregate roots, child entities, value objects, repository ports, domain events, and optional domain services. |

Do not create an application service only because a Business Activity exists. Application services are use-case
or command-lifecycle boundaries. Add activity-level domain services, policies, events, or projections only when several
use cases in the activity share real business behavior.

Domain services are optional. Prefer behavior on the aggregate root when the rule belongs to one consistency boundary.
Use a domain service for real domain policy that coordinates behavior not owned naturally by a single aggregate root.
An aggregate root is itself an entity and may contain child entities and value objects.

Use Event Storming to reveal aggregate boundaries: commands and events show what changes state, which rules must be
consistent immediately, and where each transaction can end.

## Implementation Naming

Every use case has a matching REST resource and application service. Use cases interact with the natural aggregate roots
shown in each capability `entity_model.md`; they do not require one table per use case. REST paths use the Business
Activity id as the prefix.

| Capability | Activity | Use-case id | REST command URL | Application service | Primary aggregate/read model |
|------------|----------|-------------|------------------|---------------------|------------------------------|
| `clinic-portal` | `clinic-experience` | `view-welcome-page` | `/api/clinic-experience/view-welcome-page/view` | `ViewWelcomePageUseCase` | `WELCOME_PAGE` read model |
| `clinic-portal` | `clinic-experience` | `view-application-error` | `/api/clinic-experience/view-application-error/present` | `PresentApplicationErrorUseCase` | `ERROR_PRESENTATION` read model |
| `vet-catalog` | `veterinary-directory` | `view-veterinarians` | `/api/veterinary-directory/view-veterinarians/view` | `ViewVeterinariansUseCase` | `VET` |
| `owner-care` | `owner-management` | `register-new-owner` | `/api/owner-management/register-new-owner/register` | `RegisterNewOwnerUseCase` | `OWNER` |
| `owner-care` | `owner-management` | `find-owners-by-last-name` | `/api/owner-management/find-owners-by-last-name/search` | `FindOwnersByLastNameUseCase` | `OWNER_LISTING` read model |
| `owner-care` | `owner-management` | `view-owner-details` | `/api/owner-management/view-owner-details/view/{ownerId}` | `ViewOwnerDetailsUseCase` | `OWNER_DETAILS` read model |
| `owner-care` | `owner-management` | `update-owner` | `/api/owner-management/update-owner/update/{ownerId}` | `UpdateOwnerUseCase` | `OWNER` |
| `owner-care` | `pet-management` | `add-pet-to-owner` | `/api/pet-management/add-pet-to-owner/add` | `AddPetToOwnerUseCase` | `OWNER` |
| `owner-care` | `pet-management` | `update-pet` | `/api/pet-management/update-pet/update/{ownerId}/{petId}` | `UpdatePetUseCase` | `OWNER` |
| `owner-care` | `visit-management` | `book-visit-for-pet` | `/api/visit-management/book-visit-for-pet/book` | `BookVisitForPetUseCase` | `OWNER` |

Java names are CamelCase. Business Capability, Business Activity, REST path, and Gherkin feature names are dash-case.
Database tables are snake_case and model natural aggregate entities or projections, not activity folders. If an activity
needs a Java concept, make it a real domain service, policy, event, or projection with a CamelCase name; do not create
one merely to mirror the folder hierarchy.

## Capabilities And Activities

| Business Capability | Business Activity | Use cases | Current code package |
|---------------------|-------------------|-----------|----------------------|
| `clinic-portal` | `clinic-experience` | `view-welcome-page`, `view-application-error` | `core`, `welcome` |
| `vet-catalog` | `veterinary-directory` | `view-veterinarians` | `vet` |
| `owner-care` | `owner-management` | `register-new-owner`, `find-owners-by-last-name`, `view-owner-details`, `update-owner` | `owner` |
| `owner-care` | `pet-management` | `add-pet-to-owner`, `update-pet` | `pet` |
| `owner-care` | `visit-management` | `book-visit-for-pet` | `visit` |
