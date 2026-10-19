# Test report

## How to run

```bash
mvn test            # all tests
mvn clean package   # tests + runnable jar
mvn test -Dtest=DijkstraRouterTest   # one class
```

**Database for tests:** integration tests start a real **PostgreSQL 17** server from the zonky
`embedded-postgres` binaries (downloaded by Maven on the first run), so PostgreSQL-specific SQL (`regexp_replace`,
`TIMESTAMPTZ`, check constraints, partial-text search) is tested for real. No Docker is required and H2 is not
used. Flyway builds the schema at the start of the run. Tests that write data are `@Transactional` and roll back.
The `.env` file and demo data are switched off in tests so results do not depend on local settings.

## Result

Executed on 19 October 2026, Windows 11, JDK 21.0.8, Maven 3.10:

```
Tests run: 84, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

| Test class | Type | Tests |
|---|---|---|
| `DijkstraRouterTest` | unit (algorithm) | 12 |
| `RouteCostModelTest` | unit (cost model) | 7 |
| `MetroGraphBuilderTest` | unit (graph validation) | 2 |
| `CrowdAggregationPolicyTest` | unit (crowd policy and status) | 5 |
| `CrowdStatusServiceTest` | unit with Mockito | 2 |
| `SmallRulesTest` | unit (alert workflow, accessibility, explanations, search) | 4 |
| `EnvironmentConfigTest` | unit (.env loading, production guard) | 5 |
| `NetworkSeedIntegrationTest` | integration (Flyway, seed data, DB constraints) | 4 |
| `RouteApiIntegrationTest` | integration (routing on the real network) | 11 |
| `CrowdReportIntegrationTest` | integration (reports, validation, moderation) | 8 |
| `ServiceAlertIntegrationTest` | integration (alerts, routing effects, step-free) | 7 |
| `SecurityIntegrationTest` | integration (authentication, roles, CSRF) | 11 |
| `StationApiIntegrationTest` | integration (search, station and line APIs, pages) | 6 |

## Scenarios covered

### Algorithm
* Same-line route in station order (`sameLineRouteFollowsStopsInOrder`, Versova → Ghatkopar on the real network)
* Route with interchanges and correct path rebuilding (`fastestRouteUsesInterchangesAndIsRebuiltInOrder`)
* Fastest ≠ fewest transfers (`fewerTransfersPreferenceChoosesDifferentRouteThanFastest`, and Goregaon (W) → Goregaon (E))
* Crowd penalties change the less-crowded route, and small penalties do not outweigh large time savings
* Blocked and inactive connections are avoided; delays add minutes
* Unreachable destination returns no route; unknown nodes and negative costs are rejected
* Station on two lines can start on either line
* Invalid edges (non-consecutive, cross-line rides, same-line transfers, negative time, unknown node) are excluded and reported

### Services
* Reports are stored as PENDING with a 15-minute expiry; client-supplied status or user fields are ignored
* Expired reports and rejected reports do not affect routing
* Aggregation: newest verified report beats newer unverified ones; missing data stays unknown, never LOW
* Alert transitions: DRAFT → ACTIVE (records the verifier), ACTIVE → RESOLVED, invalid and final-state transitions give 409
* Only active, verified, started alerts affect routes; INFORMATION and future alerts do not
* SUSPENDED segment reroutes; DELAY affects only its connections; closed origin is rejected
* Inconsistent line/station combinations and half-specified segments are rejected
* Step-free mode avoids an interchange whose recorded lift is out of service and warns about missing data
* Accessibility rule: an escalator or a lift of unknown status does not count as step-free

### Web, security and validation
* Station search ignores case, spaces and punctuation (`dn nagar`, `ANDHERI W`)
* Route endpoint: missing parameters, identical stations, unknown station (404), non-numeric id, unknown preference
* Uniform JSON errors with `fieldErrors`; malformed JSON gives 400
* Anonymous → 401 on the admin API; commuter → 403; admin → 200; wrong password → 401
* Admin pages redirect anonymous users to login and return 403 for commuters
* Forms without a CSRF token are rejected
* Registration ignores a posted `role` and stores a BCrypt hash; duplicate emails and mismatched passwords show messages
* Admin bootstrap stores a hashed password
* Rate limit returns 429; double verification returns 409
* Pages render: home, planner, results, directory, station, line, 404

## Manual checks (also carried out)
The application was started against the bundled PostgreSQL with the root `.env`. The log showed `.env` loading,
admin bootstrap and demo seeding. Every public and admin page returned 200, and sign-in, registration, crowd
report submission, rate limiting and alert activation were exercised over HTTP. Screenshots of the home, results,
station and line pages were checked at desktop width and at 600 px.

## Known limitations of the tests
* The browser JavaScript (autocomplete, swap, charts) has no automated tests; it was checked by hand.
* Visual layout is not tested automatically.
* No load or performance testing was done.
