# Prompt: MetroPulse project report

Copy everything inside the box below into ChatGPT, Claude or Gemini. **Attach both sample reports** (the MGM
college report and the Symbiosis report) in the same message. Attach your screenshots and photographs too, or add
them yourself afterwards in the places the report marks.

Before sending, replace every `[[...]]` with your real details. In particular, fill in the volunteering section
truthfully: an AI must not invent your volunteering work.

---

```text
You are helping two students write the final project report for their academic mini-project. Write the complete
report, ready to paste into Microsoft Word.

=== WHO AND WHAT ===
Project title: MetroPulse – A Crowd-Aware Smart Journey Planning and Station Information System for Mumbai Metro
Subject: Full Stack Java Programming
Submitted by:
  - Maithili Pawar – Roll No. D-248
  - Maitri Borkar – Roll No. D-215
College / Department / Class: [[e.g. MGM ..., Department of ..., SY B.Tech ...]]
Guide / Faculty: [[name]]
Academic year: [[2026-27]]
GitHub repository: https://github.com/SAI-RANDIVE/MetroPulse

=== THE TWO ATTACHED SAMPLES ===
1. MGM college sample report: follow its FORMATTING exactly - cover page layout, certificate page,
   acknowledgement, declaration (if present), abstract, index/table of contents, list of figures, list of tables,
   chapter numbering style, heading levels, fonts and sizes, line spacing, margins, page numbering,
   header/footer, figure and table caption style ("Fig. 3.1 ...", "Table 2.1 ..."), and references style.
   Where the sample shows a value (font name, size, spacing), state it at the start of your answer so we can
   set it in Word.
2. Symbiosis sample report: use it ONLY as a guide for how diagrams and the flow of explanation are presented
   (what kinds of diagrams, where they appear, how each is introduced and explained). Do not copy its formatting
   or text.
Write the cover page, certificate and every other page with BOTH student names and roll numbers and the subject
name, exactly as given above.

=== REQUIRED CHAPTERS (keep this order; use the MGM numbering style) ===
1. Introduction - background on Mumbai Metro Lines 1, 2A and 7, motivation, objectives, scope, report organisation.
2. Problem Statement - the commuter problem, target users, proposed solution, research question.
3. Project Planning and Timeline - team roles, development phases, tools, and a TIMELINE CHART (Gantt chart).
4. Data Collection - which official sources were used, what was collected, verification date, assumptions and limitations.
5. Project Design - architecture, database design, algorithm design, UI design.
6. Implementation and Testing - modules, key code, screenshots, testing strategy and results.
7. Final Outcomes - what was achieved against each objective, demo walkthrough, limitations, future scope.
8. Volunteering Services Completed - from the details given below.
9. Conclusion
10. References
Appendices: setup steps, API endpoint list, full test list.

=== PROJECT FACTS (use these; do not invent features, numbers or results) ===
Technology: Java 21, Spring Boot 4.0.8 (Spring MVC, Spring Data JPA with Hibernate 7, Spring Security 7, Bean
Validation), Thymeleaf, Bootstrap 5.3 with custom CSS (light, "organic" design), vanilla JavaScript, Chart.js 4,
PostgreSQL, Flyway migrations, Maven, JUnit 5, Mockito, Spring Boot Test, MockMvc.

Network data: Mumbai Metro Line 1 (Versova–Ghatkopar, 12 stations), Line 2A (Dahisar (East)–Andheri (West),
17 stations), Line 7 (Gundavali–Ovaripada, 13 stations, plus Dahisar (East) as the shared junction).
42 stations in total, 43 line-station nodes, 80 ride edges, 6 transfer edges. The three lines form a loop.
Interchanges: Dahisar (East) (2A and 7, same station), D N Nagar (Line 1) and Andheri (West) (2A) joined by a
walkway, Western Express Highway (Line 1) and Gundavali (Line 7) joined by a walkway.
Sources (checked 19 Oct 2026): MMMOCL project brief, network-under-operation and fare-table pages
(mmmocl.co.in); Mumbai Metro One fare page (reliancemumbaimetro.com). Travel times are prototype ESTIMATES:
2 min per stop, 8 min interchange walks, 4 min change at Dahisar (East). Coordinates were not stored.

Modules:
A. Passenger home page with journey planner (autocomplete station pickers, swap button, preferences, step-free option)
B. Line and station directory (partial search, station pages, line diagrams)
C. Intelligent route planner: weighted directed graph with one node per station-on-a-line; Dijkstra's algorithm;
   three preferences (fastest, less crowded, fewer transfers), each calculated separately; duplicate routes
   flagged; a plain-language explanation for every recommendation.
   Cost model: routeCost = travel time + crowd penalty + incident (alert) penalty + preference penalty.
   Crowd penalties (demo tuning values): LOW 0, MODERATE 2, HIGH 6, VERY_HIGH 12 minutes; transfer penalty 20 minutes.
   Complexity O((V+E) log V).
D. Crowd reports: signed-in users report LOW/MODERATE/HIGH/VERY_HIGH; reports expire after 15 minutes; spam limits
   (1 per station every 5 minutes, 6 per hour); aggregation rule "newest verified report wins, else newest report";
   missing data is shown as unknown, never as low.
E. Service alerts: DRAFT -> ACTIVE -> RESOLVED / CANCELLED workflow; an alert can suspend or delay specific line
   segments, a station, or a whole line; only verified, active alerts affect routes.
F. Station facilities (lift, escalator, wheelchair access, etc.) with status and verification time; step-free routing.
G. Authentication and authorisation: Spring Security, BCrypt passwords, COMMUTER and ADMIN roles, CSRF protection
   on forms, stateless HTTP Basic JSON API, administrator account created from the .env file.
H. Admin dashboard: database-driven statistics and Chart.js charts, moderation queue, alert and facility
   management, station and line editing, network integrity check, audit log.

Architecture: layered - controllers (web pages and JSON API) -> services -> repositories -> PostgreSQL;
a separate routing package (MetroGraphBuilder, DijkstraRouter, RouteCostModel, RouteDescriber, RouteExplainer).
Configuration comes from a root .env file, loaded by a custom Spring EnvironmentPostProcessor.

Database tables: metro_line, station, line_station, network_edge, app_user, crowd_report, service_alert,
alert_affected_edge, station_facility, audit_log (foreign keys, unique and check constraints, indexes).

Testing: 84 automated tests, all passing (19 Oct 2026): 37 unit tests (Dijkstra, cost model, graph validation,
crowd policy, Mockito service test, alert workflow, .env loading, production safety check) and 47 integration
tests against a real PostgreSQL 17 (routing on the real network, crowd reports, alerts, security, station API,
seed data). Worked example to describe: Goregaon (West) -> Goregaon (East): the fastest route is 34 min with
2 changes (south via Line 1); with demo crowd reports at Andheri and WEH, the less-crowded route is 50 min with
1 change (north via Dahisar (East)).
Limitations: estimated travel times, user-reported crowding only, demo facility data, three lines only, no map.

=== DIAGRAMS TO INCLUDE ===
Draw each diagram in Mermaid code (so we can paste it into mermaid.live and export a PNG). Give each one a
figure number, a caption, and a short paragraph introducing and explaining it, in the style of the Symbiosis sample:
  1. System architecture (layers and components)
  2. Use-case diagram (Commuter, Administrator, Guest)
  3. Entity-relationship diagram of the 10 tables
  4. Data flow diagram - level 0 (context) and level 1
  5. Flowchart of the journey-planning process (request -> validation -> graph -> Dijkstra per preference -> results)
  6. Dijkstra algorithm flowchart
  7. Sequence diagram: commuter submits a crowd report and an admin verifies it
  8. State diagram of the alert workflow (DRAFT, ACTIVE, RESOLVED, CANCELLED)
  9. Graph model sketch: nodes and ride/transfer edges around one interchange
  10. Class diagram of the routing package
  11. Gantt chart (Mermaid "gantt") for the timeline below
  12. Deployment / run diagram (browser -> Spring Boot app -> PostgreSQL)

=== TIMELINE (put these dates on the Gantt chart) ===
[[Fill in your real dates. Example phases:
  Week 1: topic selection, requirement analysis, data collection
  Week 2: database design, Flyway schema and seed data
  Week 3: routing engine (graph + Dijkstra) and unit tests
  Week 4: commuter features (planner, stations, crowd reports, alerts)
  Week 5: security and admin dashboard
  Week 6: UI polish, integration testing, documentation and report]]
Team roles: [[e.g. Maithili Pawar - backend, routing, database; Maitri Borkar - UI, testing, documentation]]

=== PHOTOGRAPHS AND SCREENSHOTS ===
Where a screenshot belongs, write a placeholder line like
  [Insert Fig. 6.3: Route results page - Goregaon (West) to Goregaon (East)]
with a caption. Plan placeholders for: home page, journey planner, route results, station directory,
station detail page, line diagram page, crowd report form, login/registration, admin dashboard with charts,
crowd-report moderation, alert management, facilities page, test run output ("Tests run: 84, Failures: 0"),
project folder structure in VS Code, and the volunteering photographs.

=== VOLUNTEERING SERVICES COMPLETED ===
Write this chapter ONLY from the details below. If something is missing, leave a clearly marked
[[to be filled]] instead of inventing it.
[[For each activity: name of activity / organisation, date(s), place, number of hours, what you did,
  who benefited, what you learned, and photos available (yes/no).]]

=== WRITING RULES ===
- Formal academic English, third person, and consistent tense.
- Do not invent survey results, user numbers, performance benchmarks or citations. Use only the references
  listed here, plus well-known ones such as E. W. Dijkstra, "A note on two problems in connexion with graphs",
  Numerische Mathematik 1 (1959) 269-271, and the official Spring Boot and PostgreSQL documentation.
- State clearly that MetroPulse is an academic prototype, not an official Mumbai Metro service.
- Include an abstract (150-250 words), a list of figures and a list of tables.
- Use tables for: objectives vs outcomes, technology stack, database tables, test cases (ID, scenario,
  expected, actual, status), and the API endpoint list.
- Show 3-5 short code excerpts with explanations (Dijkstra loop, cost function, .env loader, security config,
  crowd aggregation rule).
- Output the report chapter by chapter. If it is too long for one reply, stop after a chapter and wait for me
  to type "continue".
```

---

## Tips for building the report in Word

1. **Diagrams:** paste each Mermaid block at https://mermaid.live, export it as PNG, and insert it in Word.
2. **Screenshots:** start the app (`mvn spring-boot:test-run -Dspring-boot.run.main-class=com.metropulse.LocalDevApplication`),
   sign in as admin, click **Admin → Reset demo data** first (demo crowd reports expire after 15 minutes), then press
   `Win + Shift + S`. Good pages to capture: `/`, `/routes?from=26&to=33&preference=LESS_CROWDED`, `/stations`,
   `/stations/4`, `/lines/2A`, `/alerts`, `/reports/new`, `/admin`, `/admin/reports`, `/admin/alerts`.
3. **Test output screenshot:** run `mvn test` and capture the `Tests run: 84, Failures: 0` line.
4. More source material: the project `README.md` (features, architecture, data sources) and the code itself,
   e.g. `src/main/java/com/metropulse/service/routing/DijkstraRouter.java`.
