# Problem statement

## Context

Mumbai's metro network is growing quickly. Lines 1, 2A and 7 already form a loop across the western suburbs:
Line 1 runs east–west from Versova to Ghatkopar, Line 2A runs north–south along the Link Road corridor, and
Line 7 runs north–south along the Western Express Highway. The lines meet at Dahisar (East), and walkways join
D N Nagar (Line 1) with Andheri (West) (Line 2A), and Western Express Highway (Line 1) with Gundavali (Line 7).

Because of this loop, many journeys have more than one sensible route. A route that is a few minutes faster may
pass through some of the busiest interchanges in the city during peak hours.

## The commuter problem

* Network maps show **where** trains go, not **how busy** a station is right now.
* Disruption information is scattered, and it is often unclear whether it affects a particular journey.
* Passengers who need lifts cannot easily tell which interchanges are step-free today.
* Route tools tend to show one answer without explaining the trade-off.

## Target users

* **Commuters**: plan journeys, check stations, and share what they see on the platform.
* **Moderators (administrators)**: verify reports, publish alerts, keep station and facility data correct.

## Proposed solution

MetroPulse is a web application that:

1. models the network as a weighted directed graph and finds routes with Dijkstra's algorithm;
2. compares the **fastest**, **less-crowded** and **fewer-transfer** routes, says when they are the same, and
   explains each recommendation;
3. collects short-lived crowd reports (valid for 15 minutes) and lets moderators verify them;
4. applies verified service alerts to exactly the connections they affect;
5. records station facilities and uses them for step-free interchange planning;
6. keeps estimates, user reports and verified information clearly distinct in the interface.

## Scope

**In scope:** Lines 1, 2A and 7 (42 stations); journey planning; crowd reports; alerts; facilities; admin
dashboard; authentication and roles; automated tests.

**Out of scope:** live train positions, fares and ticketing, official operator data feeds, maps, mobile apps,
other metro lines, suburban rail.

## Research question

> Can a shortest-path planner whose edge weights include recent, crowd-sourced crowding and verified incident
> information give commuters useful alternative routes, while staying honest about how reliable and how recent
> that information is?

The prototype answers this as a design question: it shows how crowd and alert data can be combined into
non-negative edge costs, and how the result can be explained. It does not measure real passenger outcomes;
no survey or field study was carried out.
