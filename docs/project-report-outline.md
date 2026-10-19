# Project report outline

A suggested structure for the written report. Fill each section with your own analysis; do not cite sources you
have not read, and do not report survey results or performance numbers you have not measured.

## Title page
MetroPulse: A Crowd-Aware Smart Journey Planning and Station Information System for Mumbai Metro. Name, roll
number, course, institution, guide, date.

## Abstract (150–250 words)
Problem, approach (Spring Boot web application, graph model, Dijkstra with crowd and alert weights), main
features, how it was tested, key limitation (estimated times, user-reported crowding).

## 1. Introduction
1.1 Background: urban rail in Mumbai, Lines 1, 2A and 7 and the loop they form
1.2 Motivation: crowding, disruptions and accessibility as everyday commuter concerns
1.3 Report organisation

## 2. Literature and context review
2.1 Journey planners and shortest-path routing (Dijkstra; mention A* and why it was not needed)
2.2 Crowd-sourced transport information and its reliability problems
2.3 Verification and moderation of user-generated data
2.4 Web application frameworks: MVC, ORM, server-side rendering
(Use real, checked references only - see the reference list below.)

## 3. Problem statement
From [problem-statement.md](problem-statement.md): problem, target users, research question.

## 4. Objectives
List the seven objectives from the project brief and how each is met.

## 5. Methodology
5.1 Development approach (phased: data → routing engine → commuter features → security/admin → UI → testing)
5.2 Data collection: operator websites, verification date, assumptions ([data-sources.md](data-sources.md))
5.3 Tools: Java 21, Spring Boot, PostgreSQL, Flyway, Maven, JUnit, Git

## 6. Requirements
6.1 Functional requirements (modules A–H)
6.2 Non-functional requirements (correctness, security, accessibility, maintainability)
6.3 Use-case diagram (commuter, administrator)

## 7. System design
7.1 Layered architecture ([system-design.md](system-design.md))
7.2 Database design and ER diagram ([database-design.md](database-design.md))
7.3 Graph model and algorithm ([algorithm.md](algorithm.md))
7.4 Security design
7.5 UI design: organic light theme, accessibility choices, page flow

## 8. Implementation
8.1 Project structure and key classes (with short code excerpts)
8.2 Environment configuration and the `.env` loader
8.3 Routing engine: MetroGraphBuilder, DijkstraRouter, RouteCostModel
8.4 Crowd reports, aggregation policy and expiry
8.5 Alert workflow and how alerts affect edges
8.6 Admin dashboard and audit log
8.7 Screenshots of the main pages

## 9. Testing
Strategy, tools, scenario table and results from [test-report.md](test-report.md).

## 10. Results and discussion
Worked route examples (e.g. Goregaon (West) → Goregaon (East)); how the preferences differ; what the
explanations tell the user; observations from your own trials.

## 11. Limitations and future work
Estimated times, no live data, uniform penalties, demo facility data, three lines only. Future work: official
open data or GTFS feeds, timetable-aware routing, calibrated penalties, more lines, maps, notifications.

## 12. Conclusion

## References
Verified sources used in this project:
* MMMOCL, Project brief: https://www.mmmocl.co.in/project_brief (accessed 19 Oct 2026)
* MMMOCL, Network under operation: https://www.mmmocl.co.in/network_under_operation (accessed 19 Oct 2026)
* MMMOCL, Fare table: https://www.mmmocl.co.in/fare_table (accessed 19 Oct 2026)
* Mumbai Metro One, Ticket fares: https://www.reliancemumbaimetro.com/ticket-fares.aspx (accessed 19 Oct 2026)
* E. W. Dijkstra, "A note on two problems in connexion with graphs", *Numerische Mathematik* 1, 269–271 (1959)
* Spring Boot reference documentation: https://docs.spring.io/spring-boot/
* PostgreSQL documentation: https://www.postgresql.org/docs/

Add textbook or paper references only after reading them.

## Appendices
A. Setup guide (README) · B. API reference · C. Full test list
