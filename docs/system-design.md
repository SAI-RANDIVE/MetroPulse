# System design

## Layers

| Layer | Package | Responsibility |
|---|---|---|
| Presentation | `controller.web`, `templates/`, `static/` | Thymeleaf pages, forms, flash messages, error pages |
| API | `controller.api` | JSON endpoints using DTOs, uniform `ApiError` responses |
| Business | `service` | Validation rules, workflows, transactions, audit logging |
| Algorithm | `service.routing` | Graph, Dijkstra, cost model, route description and explanation (no Spring or database code apart from `@Component`) |
| Persistence | `repository`, `entity` | Spring Data JPA repositories and Hibernate entities |
| Database | PostgreSQL + Flyway | Schema, constraints, seed network |
| Cross-cutting | `security`, `config`, `exception`, `validation` | Login and roles, settings, errors, custom constraints |

Controllers never contain business rules or SQL; entities are never returned from the JSON API (DTO records are).

## Main components

* **RoutePlannerService** orchestrates a search: load the graph (`NetworkGraphService`), collect current
  conditions (`RoutingConditionsService`), run `DijkstraRouter` once per preference with weights from
  `RouteCostModel`, then build legs (`RouteDescriber`), explanations (`RouteExplainer`) and warnings.
* **RoutingConditionsService** gathers crowd levels (`CrowdStatusService`), in-effect alerts
  (`ServiceAlertService`) and step-free data (`FacilityService`) into an immutable `RoutingConditions` snapshot.
* **CrowdReportService** handles submission (rate limits, expiry), "my reports" and moderation.
* **ServiceAlertService** handles alert validation (line/station consistency, segment resolution) and the
  DRAFT → ACTIVE → RESOLVED / CANCELLED state machine (`AlertStatus.allowedNext()`).
* **StationService / LineService** handle the directory, search, station and line pages, and admin network edits.
* **DashboardService** computes statistics with aggregate queries.
* **AuditService** records administrative actions without secrets.
* **AdminBootstrap** and **DemoDataSeeder** are `ApplicationRunner`s that run after startup.

## Request flow: planning a journey

```
GET /routes?from=26&to=33&preference=LESS_CROWDED
  → Spring Security (public URL)
  → HomeController.routes()  @Valid RouteSearchForm (+ @DifferentStations)
  → RoutePlannerService.plan()
       NetworkGraphService.currentGraph()        (2 queries: nodes, edges)
       RoutingConditionsService.current()        (crowd reports, alerts, facilities)
       DijkstraRouter × 3 preferences
       RouteDescriber / RouteExplainer
  → RoutePlanResult DTO → routes/results.html
```

The graph is rebuilt per request. The network is small, so this is cheap, and admin changes apply immediately.

## Security design

* **Two filter chains.** `/api/**` is stateless: HTTP Basic on each request, no session, no CSRF (no cookie can
  be abused), JSON 401/403. Everything else uses form login with a session and CSRF tokens on all forms.
* **Roles.** Public: browsing, routes, alerts. COMMUTER: crowd reports. ADMIN: `/admin/**` and `/api/admin/**`.
* **Passwords** are hashed with BCrypt; the login principal erases the hash after authentication.
* **No over-posting.** Forms and request bodies are separate classes without status, user, role or verifier
  fields; the server takes the user from the login.
* **Headers.** A Content Security Policy that allows only same-origin scripts (no inline scripts), plus a
  same-origin referrer policy and frame denial.
* **Production guard.** `ProductionSafetyCheck` stops startup when production uses placeholder passwords.

## Configuration

`DotenvEnvironmentPostProcessor` loads the root `.env` into the Spring environment after OS variables and before
`application.yml` defaults. `MetroPulseProperties` (a `@ConfigurationProperties` record) holds every tunable
value: report lifetime, rate limits, crowd and transfer penalties, time zone.

## Error handling

| Situation | API (`ApiExceptionHandler`) | Web (`WebExceptionHandler`) |
|---|---|---|
| Unknown id | 404 JSON | 404 page |
| Validation error | 400 with `fieldErrors` | form shown again with messages |
| Business rule broken | 400 / 409 / 422 / 429 | message on the form or error page |
| Not signed in / not allowed | 401 / 403 JSON | redirect to login / 403 page |
| Database down | 503 | friendly error page |

Stack traces are never shown to users (`server.error.include-stacktrace: never`).

## Front end

Thymeleaf fragments (`fragments/layout.html`, `components.html`, `planner.html`) supply the navigation, footer,
line chips, crowd pills, alert cards, station picker and pagination. `app.js` upgrades the station `<select>`
boxes into an accessible autocomplete combobox, adds the swap button, live directory filtering and loading
feedback. Every feature still works without JavaScript. `admin.js` draws the Chart.js charts from data attributes.
