# Architecture

Read this **before implementing** anything in `src/main/java/`.

## Module layout

Single Maven module, Spring Modulith application modules under `ai.unifiedprocess.petclinic`. The direct subpackages
(`core`, `welcome`, `vet`, `owner`, `pet`, `visit`) are the current code modules that implement the Software
Capabilities and Software Activities described in `docs/capabilities/`.

The documentation hierarchy is capability-first:

```text
docs/capabilities/<capability>/entity_model.md
docs/capabilities/<capability>/activities/<activity>/use-cases/<use-case-id>/
```

- **Software Capability** — strategic goal boundary: what this software can fundamentally achieve, e.g. `owner-care` or
  `vet-catalog`. It is a candidate module/service boundary, not one concrete workflow.
- **Software Activity** — behavioral boundary: how related use cases are organized to produce value inside a
  capability, e.g. `pet-management` or `visit-management`.
- **Use Case / Gherkin Feature** — delivery boundary: the smallest independently valuable goal. It is described or
  refined by `uc.md`, which records the JIRA ticket id. The `Feature:` name equals the use-case id. This is the default
  application-service boundary.
- **Gherkin Scenario** — one business-relevant flow or supported transition inside the use case.
- **Cucumber Step Definition** — test adapter from scenario language to application services and test fixture APIs.

Software Activities may own shared policies, domain services, events, or projections when multiple use cases share real
business behavior. Do not create an application service only because an activity exists; application services remain
use-case command/lifecycle boundaries.

The standard implementation traceability chain is:

```text
Software Capability
  -> Software Activity
    -> Use Case / Gherkin Feature
      -> Gherkin Scenario
        -> Cucumber Step Definition
          -> Application Service
            -> Domain Model
```

The Domain Model includes aggregate roots, child entities, value objects, repository ports, domain events, and optional
domain services.

Use Event Storming to test these boundaries before changing the domain model. Put commands and domain events on a
timeline, then ask what changes state, what must be consistent immediately, and where each transaction can end. The
business object whose state changes is an aggregate-root candidate; rules that must hold immediately identify the
consistency boundary; later reactions triggered by events usually belong to another command or use case.

Each feature module has up to four sub-packages:

- **`api`** — REST resources grouped by use case, not CRUD noun. Resource paths follow
  `/api/<activity>/<use-case-id>/<command>`, for example `/api/pet-management/add-pet-to-owner/add`. There is a
  one-to-one mapping from REST resource to application service.
- **`application`** — use-case application services. These own command/result records, transaction boundaries, and the
  use-case lifecycle. REST resources, Vaadin views, and Cucumber steps call these services instead of reaching directly
  into repositories for use-case behavior.
- **`ui`** — Vaadin views, forms, and other UI components. One view per use case / screen.
- **`domain`** — behavior-rich aggregate roots, entities, value objects, domain services, and repository ports.
  Domain objects are not database-shaped persistence objects.
- **`infrastructure`** — jOOQ repository adapters and database persistence objects. Database-shaped types use the
  `PO` suffix, for example `OwnerPO`, `PetPO`, and `VisitPO`.

`core/` holds the app shell (`MainLayout`) and the shared error views.

Keep layers thin. Add a domain service only when it expresses a real business rule; add an application service and REST
resource for each use-case lifecycle.

## Use-case naming rule

The dash-separated use-case id from `docs/capabilities` drives naming:

| Layer | Example |
|-------|---------|
| Software Capability folder | `owner-care` |
| Software Activity folder and REST prefix | `pet-management` |
| Use-case folder and Gherkin `Feature:` | `add-pet-to-owner` |
| Java REST resource | `AddPetToOwnerResource` |
| Java application service | `AddPetToOwnerUseCase` |
| Java primary aggregate root | `Owner` |
| Java persistence object | `OwnerPO`, `PetPO` |
| REST command URL | `/api/pet-management/add-pet-to-owner/add` |
| Database tables | `owners`, `pets` |

The `uc.md` file records the JIRA ticket id. The folder name is the use-case id.

## DDD rules

- Application services are the transaction boundary for a use-case command.
- A use case should declare its primary aggregate root or read model, but it does not require a dedicated table.
- Several use cases may operate on the same aggregate root when that is the natural consistency boundary.
- A use case can involve multiple aggregate roots, but commands should update only the aggregate roots that must be
  consistent immediately inside that transaction.
- Domain objects contain behavior and invariants. They are not Hibernate entities, jOOQ records, or table DTOs.
- Domain services are optional. Add one only when a business rule does not belong naturally inside one aggregate root.
- An aggregate root is itself an entity. It may contain child entities and value objects.
- Persistence objects are database representations and must use the `PO` suffix.
- Repository interfaces live in `domain`; jOOQ implementations live in `infrastructure`.
- Aggregate roots are saved atomically inside the application service transaction.

## Cross-feature rule — and its one exception

Cross-feature reach-in goes through another feature's application/domain API, not its `ui` or `infrastructure`.

**Exception — the app shell and detail aggregators.** `core/ui/MainLayout` and views that aggregate cross-feature
actions (e.g. `OwnerDetailsView` linking to `AddPetView`, `EditPetView`, `AddVisitView`) may import other features'
`ui/` classes **as `.class` route tokens** for `SideNavItem` or `ui.navigate(...)`. No instantiation, no method calls,
no state reads — the token is only a routing key. Any richer interaction must still go through the other feature's
`domain` package.

## Data access (jOOQ)

- jOOQ is confined to `infrastructure`.
- Repository adapters map query results into `*PO` records first, then convert to domain objects.
- Use `org.jooq.Records.mapping(TypePO::new)` for PO construction. Never map generated jOOQ records directly into
  domain aggregate roots.
- Parent-to-children read models may use `multiset(...).convertFrom(...)` to avoid N+1 queries.
- Lazy-loaded grids return `Stream<T>` from a `findPage(int offset, int limit)` plus a paired `int count()`.

## Persistence stereotype

Infrastructure adapters are named `Jooq<Entity>Repository` and annotated `@Repository` — not `*Dao` or `*Store`. The
ban on *Spring Data* does not extend to the stereotype: `@Repository` is what enables
`PersistenceExceptionTranslationPostProcessor` to translate JDBC `SQLException`s into Spring's `DataAccessException`
hierarchy. Plain `@Component` silently opts out.

## Vaadin view conventions

- **Route parameters centralized per feature.** E.g. `owner/ui/OwnerRouteParameters` defines `OWNER_ID` / `PET_ID`
  constants and builder methods (`forOwner(id)`, `forPet(ownerId, petId)`) so route parameter names can't drift between
  views. **Check for an existing `*RouteParameters` class in the feature package before adding a parameterized route —
  create one if it doesn't exist yet.**
- **Styling — never `component.getStyle().set(...)`.** Always use `addClassNames(LumoUtility.…)` constants (e.g.
  `LumoUtility.Padding.Horizontal.MEDIUM`, `LumoUtility.Margin.NONE`, `LumoUtility.FontSize.LARGE`). If a utility class
  doesn't cover what you need, ask rather than falling back to inline styles.
- **Main menu — `SideNav` + `SideNavItem` in the drawer.** Not `Tabs`, not hand-rolled `RouterLink`s in a
  `HorizontalLayout`. Use `new SideNavItem(label, ViewClass.class)` so active-route highlighting and a11y semantics come
  for free. The navbar holds only `DrawerToggle`, logo, and title.
- **One view per use case, per route.** Put the UC id in the class Javadoc so grep from a failing test lands on the
  right file.
- **Validation is layered.** Forms provide field-level feedback. Domain value objects and aggregate behavior enforce
  invariants again so REST, Cucumber, and UI paths share the same business rules.
- **`NotFoundException` in `beforeEnter` for missing entities.** Throwing `com.vaadin.flow.router.NotFoundException`
  from `beforeEnter` — e.g. when `OwnerRepository.findById` returns empty — is the canonical way to
  surface a 404. The router automatically routes to `NotFoundErrorView`, which wins over `ApplicationErrorView` because
  its generic parameter is narrower.
- **Error views render inside `MainLayout`.** The shell stays functional so the user is never stranded.
  `NotFoundErrorView` handles `HasErrorParameter<NotFoundException>` (HTTP 404); `ApplicationErrorView` handles
  `HasErrorParameter<Exception>` (HTTP 500). Both delegate rendering to `ErrorPanel`. Neither shows a stack trace —
  message only.

## Flyway

- Production migrations: `src/main/resources/db/migration/V1__initial_schema.sql`.
- Test seed data: `src/test/resources/db/migration/V2__seed_reference_data.sql` (test classpath only — Flyway picks both
  up automatically when tests run).
- **After changing a migration**, run `./mvnw generate-sources` (or any later phase) to refresh jOOQ classes. The
  migrations effectively *are* the schema DSL.
