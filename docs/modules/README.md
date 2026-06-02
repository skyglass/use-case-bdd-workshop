# Module Specification Index

This directory is the DDD-oriented specification root. Each module represents a bounded use-case cluster and owns:

- `entity_model.md` - the module's domain model slice.
- `<use-case-id>/UC-*.md` - JIRA epic files that define or refine the use case.
- `<use-case-id>/scenarios.feature` - executable Gherkin scenarios for that use case.
- `<use-case-id>/use_cases.puml` - PlantUML use-case/aggregate-interaction diagram.

Use-case folder names are short dash-separated ids. The `Feature:` name in each `scenarios.feature` file matches the folder name exactly.

## Implementation Naming

Every use case has a matching REST resource and application service. Use cases interact with the natural aggregate roots
shown in each module `entity_model.md`; they do not require one table per use case.

| Use-case id | REST command URL | Application service | Primary aggregate/read model |
|-------------|------------------|---------------------|------------------------------|
| `view-welcome-page` | `/api/clinic-experience/view-welcome-page/view` | `ViewWelcomePageUseCase` | `WELCOME_PAGE` read model |
| `view-application-error` | `/api/clinic-experience/view-application-error/present` | `PresentApplicationErrorUseCase` | `ERROR_PRESENTATION` read model |
| `view-veterinarians` | `/api/veterinary-directory/view-veterinarians/view` | `ViewVeterinariansUseCase` | `VET` |
| `register-new-owner` | `/api/owner-management/register-new-owner/register` | `RegisterNewOwnerUseCase` | `OWNER` |
| `find-owners-by-last-name` | `/api/owner-management/find-owners-by-last-name/search` | `FindOwnersByLastNameUseCase` | `OWNER_LISTING` read model |
| `view-owner-details` | `/api/owner-management/view-owner-details/view/{ownerId}` | `ViewOwnerDetailsUseCase` | `OWNER_DETAILS` read model |
| `update-owner` | `/api/owner-management/update-owner/update/{ownerId}` | `UpdateOwnerUseCase` | `OWNER` |
| `add-pet-to-owner` | `/api/pet-management/add-pet-to-owner/add` | `AddPetToOwnerUseCase` | `OWNER` |
| `update-pet` | `/api/pet-management/update-pet/update/{ownerId}/{petId}` | `UpdatePetUseCase` | `OWNER` |
| `book-visit-for-pet` | `/api/visit-management/book-visit-for-pet/book` | `BookVisitForPetUseCase` | `OWNER` |

Java names are CamelCase. REST paths and Gherkin feature names are dash-case. Database tables are snake_case and model
the natural aggregate entities.

## Modules

| Module | Use cases | Code package |
|--------|-----------|--------------|
| `clinic-experience` | `view-welcome-page`, `view-application-error` | `core`, `welcome` |
| `veterinary-directory` | `view-veterinarians` | `vet` |
| `owner-management` | `register-new-owner`, `find-owners-by-last-name`, `view-owner-details`, `update-owner` | `owner` |
| `pet-management` | `add-pet-to-owner`, `update-pet` | `pet` |
| `visit-management` | `book-visit-for-pet` | `visit` |
