# API reference

Base URL: `http://localhost:8080`. All bodies are JSON. Times are ISO-8601 UTC (`2026-10-19T16:45:45Z`), except
alert `startsAt`/`endsAt` in requests, which are local Mumbai time without an offset (`2026-10-20T06:00:00`).

## Authentication

* Public endpoints need nothing.
* Other endpoints use **HTTP Basic** with a MetroPulse email and password on every request
  (`curl -u admin@metropulse.local:PASSWORD ...`). The API is stateless: no session cookie and no CSRF token.
* 401 = not signed in or wrong password; 403 = signed in but not allowed (e.g. a commuter calling `/api/admin`).

## Error format (all endpoints)

```json
{
  "timestamp": "2026-10-19T16:45:46Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Some fields are invalid",
  "path": "/api/crowd-reports",
  "fieldErrors": [ { "field": "crowdLevel", "message": "Choose how crowded it is" } ]
}
```

| Status | Meaning |
|---|---|
| 400 | Validation error, bad parameter type, malformed JSON, or a broken business rule (e.g. closed station) |
| 401 / 403 | Authentication / authorisation |
| 404 | Unknown station, line, alert or report |
| 409 | Conflict: invalid alert transition, report already verified, data integrity |
| 422 | Both stations exist but no route is available |
| 429 | Crowd report rate limit |
| 503 | Database unavailable |

## Public endpoints

### `GET /api/lines`
Lines with their stops in order.
```json
[{ "id": 2, "code": "2A", "name": "Line 2A · Dahisar (East) – Andheri (West)", "color": "#FFC600",
   "textColor": "#2B2200", "status": "OPERATIONAL",
   "stops": [{ "lineStationId": 13, "stationId": 13, "stationName": "Dahisar (East)", "sequenceNo": 1,
               "operational": true, "otherLines": [{ "code": "7", "...": "..." }] }] }]
```

### `GET /api/stations` and `GET /api/stations?query=dn nagar`
All stations, or a partial search ignoring case, spaces and punctuation (max 12 results, best match first).
```json
[{ "id": 2, "code": "DN_NAGAR", "name": "D N Nagar", "area": "Andheri West", "status": "OPEN",
   "lines": [{ "id": 1, "code": "1", "color": "#0072BC", "textColor": "#FFFFFF" }], "interchange": false }]
```

### `GET /api/stations/{id}`
One station (same shape). 404 if unknown, 400 if the id is not a number.

### `GET /api/stations/{id}/status`
Crowding per line, alerts in effect, step-free status and facilities.
```json
{ "station": { "id": 5, "name": "Western Express Highway" },
  "crowd": [{ "lineStationId": 5, "line": { "code": "1" }, "state": "USER_REPORTED", "level": "HIGH",
              "reportedAt": "...", "expiresAt": "...", "currentReportCount": 1 }],
  "activeAlerts": [], "accessibility": "STEP_FREE_RECORDED",
  "facilities": [{ "type": "LIFT", "status": "AVAILABLE", "lastVerifiedAt": null, "demo": true }] }
```
`state` is `VERIFIED`, `USER_REPORTED`, `STALE` (last report expired) or `NO_DATA` (`level` is then `null`).

### `GET /api/routes?from={stationId}&to={stationId}&preference={p}&stepFree={bool}`
`preference`: `FASTEST` (default), `LESS_CROWDED`, `FEWER_TRANSFERS`. The requested option comes first.
```json
{ "origin": { "id": 26, "name": "Goregaon (West)" }, "destination": { "id": 33, "name": "Goregaon (East)" },
  "requestedPreference": "FASTEST", "stepFreeRequested": false, "distinctRouteCount": 2,
  "crowdDataAvailable": true, "alertsConsidered": [],
  "options": [{
     "preference": "FASTEST", "label": "Fastest", "recommended": true, "sameRouteAs": null,
     "totalMinutes": 34, "rideMinutes": 18, "transferMinutes": 16, "delayMinutes": 0,
     "interchanges": 2, "stationCount": 12, "routeCost": 34.0, "crowdPenalty": 20,
     "explanation": "Fastest based on configured estimated travel times.",
     "warnings": ["2 stops on this route currently have HIGH or VERY HIGH crowd reports."],
     "steps": [
       { "ride": { "lineCode": "2A", "towards": "Andheri (West)", "minutes": 6,
                   "stops": [{ "stationId": 26, "stationName": "Goregaon (West)", "crowdLevel": null }, "..."] },
         "transfer": null, "rideStep": true },
       { "ride": null, "rideStep": false,
         "transfer": { "fromStation": "Andheri (West)", "fromLineCode": "2A", "toStation": "D N Nagar",
                       "toLineCode": "1", "minutes": 8, "walkBetweenStations": true } }
     ] }],
  "notes": ["Journey times are prototype estimates ..."] }
```
Errors: 400 (missing/invalid parameters, same station, closed station), 404 (unknown station), 422 (no route).

### `GET /api/alerts` and `GET /api/alerts/{id}`
Verified alerts in effect now. A single alert is visible if it was verified and is ACTIVE or RESOLVED; drafts give 404.

## Commuter endpoints (any signed-in user)

### `POST /api/crowd-reports` → 201
```json
{ "lineStationId": 4, "crowdLevel": "HIGH", "comment": "Long queue at the gates" }
```
`crowdLevel` is required (`LOW`, `MODERATE`, `HIGH`, `VERY_HIGH`) and the comment is limited to 280 characters.
Any other fields (status, user, verifier) are ignored. A report expires after 15 minutes. Limits: one report
per station-line every 5 minutes, and 6 per hour (429). Reports by admins are verified automatically.

### `GET /api/crowd-reports/mine?page=0`
`{ "content": [...], "page": 0, "size": 10, "totalElements": 3, "totalPages": 1 }`

## Administrator endpoints (ROLE_ADMIN)

| Method and path | Body / parameters | Result |
|---|---|---|
| `GET /api/admin/reports` | `status`, `lineId`, `level`, `page`, `size` (max 100) | Paged reports with reporter names |
| `PATCH /api/admin/reports/{id}/verification` | `{ "decision": "APPROVED" }` or `REJECTED` | 200; 409 if not PENDING |
| `POST /api/admin/alerts` | see below | 201, DRAFT (or ACTIVE with `publishNow`) |
| `PATCH /api/admin/alerts/{id}` | `{ "status": "ACTIVE" \| "RESOLVED" \| "CANCELLED" }` | 200; 409 for an invalid transition |
| `GET /api/admin/dashboard/stats` | | Counts, 7-day distribution, top stations, graph size, network issues |
| `GET /api/admin/facilities` | `stationId`, `type`, `page`, `size` | Paged facilities |
| `POST /api/admin/facilities` | `{ "stationId": 5, "facilityType": "LIFT", "status": "AVAILABLE", "description": "...", "source": "Site visit" }` | 201 |

Alert body:
```json
{ "title": "Signal fault", "description": "Trains running late", "alertType": "SERVICE_DISRUPTION",
  "severity": "WARNING", "impact": "DELAY", "delayMinutes": 10,
  "lineId": 2, "segmentFromStationId": 23, "segmentToStationId": 26,
  "stationId": null, "startsAt": "2026-10-20T08:00:00", "endsAt": null, "publishNow": true }
```
Rules: give a line and/or a station; a station given with a line must be on it; a segment needs the line and both
ends; a DELAY needs minutes and a segment or station; the end must be after the start. The status, creator and
verifier always come from the server.

Station, line-mapping, connection, alert-editing and facility-update operations are also available on the admin
web pages (`/admin/...`), which use form login and CSRF tokens.
