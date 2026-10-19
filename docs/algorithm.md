# Route-finding algorithm

## 1. Modelling the metro as a graph

A station can be on more than one line, and changing lines costs time, so a plain "one node per station" graph is
not enough. MetroPulse uses one node per **station on a line**:

| Concept | Table / class | Example |
|---|---|---|
| Station (physical place) | `station` / `Station` | Dahisar (East) |
| Node = station on a line | `line_station` / `GraphNode` | Dahisar (East) on Line 2A, Dahisar (East) on Line 7 |
| Ride edge | `network_edge` with type `RIDE` | Eksar (2A) → Borivali (West) (2A), 2 min |
| Transfer edge | `network_edge` with type `TRANSFER` | D N Nagar (1) → Andheri (West) (2A), 8 min walk |

Edges are **directed**: every connection is stored once per direction, so one direction can be closed or slowed
without touching the other.

```
 Line 1:  Versova ─ D N Nagar ─ Azad Nagar ─ Andheri ─ WEH ─ … ─ Ghatkopar
                       ┆ walk 8                         ┆ walk 8
 Line 2A: Dahisar(E) ─ … ─ Andheri (West)               ┆
             ┆ change 4                                 ┆
 Line 7:  Dahisar(E) ─ Ovaripada ─ … ─────────────── Gundavali
```

`MetroGraphBuilder` turns the database rows into an adjacency list (`MetroGraph`) and **rejects bad data**: a
ride edge must join consecutive stops of one line, a transfer must join two different lines, times must not be
negative, and both ends must exist. Rejected edges are listed on the admin dashboard under *Network integrity*.

## 2. Dijkstra's algorithm

Dijkstra finds the cheapest path from a start node when every edge cost is **non-negative**.

```
dist[s] = 0 for every start node s; all other nodes = ∞
put the start nodes in a priority queue (smallest dist first)
while the queue is not empty:
    take the node u with the smallest dist
    if u was already settled, skip it
    mark u settled
    if u is a destination node: rebuild the path and stop
    for each edge u → v:
        c = cost(edge)            (∞ means "edge not allowed")
        if dist[u] + c < dist[v]:
            dist[v] = dist[u] + c; remember the edge used to reach v; add v to the queue
no destination reached → there is no route
```

Implementation details (`DijkstraRouter`):

* **Several start and end nodes.** The origin station's nodes on every line start at cost 0, and reaching any
  node of the destination station ends the search. Someone at Dahisar (East) can board either line.
* **Path rebuilding.** Each node remembers the edge that reached it; walking back from the destination and
  reversing gives the ordered route.
* **Safety checks.** Unknown start or end nodes raise an error, and a negative or NaN cost is rejected.

**Complexity:** with V nodes and E edges, O((V + E) log V) using a binary heap (`java.util.PriorityQueue`).
Here V = 43 and E = 86, so a search takes well under a millisecond.

## 3. Edge costs and preferences

The cost model (`RouteCostModel`) follows

```
routeCost = estimatedTravelTime + crowdPenalty + incidentPenalty + preferencePenalty
```

| Part | Applies to | Value |
|---|---|---|
| estimatedTravelTime | every edge | `base_minutes` (2 per ride hop, 4–8 per interchange) |
| incidentPenalty | edges named by an active DELAY alert, or arrivals at a delayed station | the alert's delay minutes |
| crowdPenalty | **Less crowded** only: each node entered that has a current report | LOW 0, MODERATE 2, HIGH 6, VERY_HIGH 12 |
| preferencePenalty | **Fewer transfers** only: each transfer edge | +20 |

Some edges are **forbidden** (cost ∞) for every preference:

* connections suspended by an active, verified SUSPENDED alert;
* transfers at a station closed by an alert, or where trains do not stop;
* with *step-free* selected, transfers at a station whose recorded lifts or wheelchair access are all out of service.

The penalty numbers are **demonstration tuning parameters**, not measured delays. They are set in
`application.yml` under `metropulse.routing`.

Dijkstra runs **once per preference**, each time with that preference's costs, so the three results are
genuinely different calculations. Two results with identical edges are shown as *"Same route as …"*, never as
separate alternatives.

## 4. Crowd data rules

* Only reports that are **not rejected** and **not expired** (15 minutes) count.
* Several reports for one station on one line: the newest **verified** report wins; otherwise the newest report
  (`CrowdAggregationPolicy`).
* A node with no current report gets **no penalty** and is shown as *"No recent reports"*. It is never treated
  as low crowding. If no report exists anywhere, the explanation says so.

## 5. Explanations

`RouteExplainer` writes one sentence per option from the computed numbers, for example:

* "Fastest based on configured estimated travel times."
* "Recommended because it has fewer reported crowding issues (crowd penalty 0 vs 20 on the fastest route), for
  about 16 extra minutes."
* "Avoids a connection that is suspended by the verified alert …" (found by re-running Dijkstra without the
  suspensions and checking which alert's connections the ideal route would have used).

## 6. Worked example

Goregaon (West) → Goregaon (East), with demo reports Andheri VERY_HIGH, WEH HIGH and Andheri (West) MODERATE:

| Preference | Route | Minutes | Changes | Cost |
|---|---|---|---|---|
| Fastest | 2A south → walk → Line 1 → walk → Line 7 | 34 | 2 | 34 |
| Less crowded | 2A north → Dahisar (E) → Line 7 south | 50 | 1 | 50 (the southern route would be 34 + 20 = 54) |
| Fewer transfers | same as less crowded | 50 | 1 | 50 + 20 = 70 (southern: 34 + 40 = 74) |

## 7. Limitations

* Uniform estimated times make many alternatives tie or differ only through penalties.
* No timetables, waiting times or train frequency; crowding is per station, not per train.
* Penalties are hand-tuned; with real data they should be calibrated.
* The fewer-transfers option weighs transfers rather than strictly minimising their number.
