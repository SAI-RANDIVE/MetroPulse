# Data sources

All network data was checked on **19 October 2026** against the operators' own websites.

## Sources

| Data | Source | What was taken |
|---|---|---|
| Line 2A and Line 7 station lists and order | MMMOCL (Maha Mumbai Metro Operation Corporation Ltd.), *Project brief*: https://www.mmmocl.co.in/project_brief | Ordered station lists, line lengths (2A: 18.6 km, 17 stations; 7: 16.5 km, 13 stations), interchange stations |
| Current station names, network order | MMMOCL fare calculator: https://www.mmmocl.co.in/fare_table | Current names (e.g. *Anand Nagar*, formerly Upper Dahisar; *Mandapeshwar – I.C. Colony*) |
| Interchanges | MMMOCL *Network under operation*: https://www.mmmocl.co.in/network_under_operation | 2A interchanges with Line 1 at D N Nagar; Line 7 interchanges with Line 1 at Western Express Highway |
| Line colours | MMMOCL route map files: `/assets/images/lines2a-map.svg` (yellow `#FFC600`), `/assets/images/line7-map.svg` (red `#DA291C`) | Line colours |
| Line 1 station list and order | Mumbai Metro One (MMOPL) fare page: https://www.reliancemumbaimetro.com/ticket-fares.aspx | 12 stations, Versova to Ghatkopar |
| Line 1 length | MMMOCL project brief (11.4 km) | Context only |

The MMMOCL pages used are in Marathi. The English spellings for Lines 2A and 7 are transliterations of those names,
cross-checked against English-language reports of MMRDA's station-name announcements (e.g.
https://themetrorailguy.com/2020/06/02/mmrda-publishes-mumbai-metro-yellow-line-2as-station-names/ and
https://themetrorailguy.com/2020/06/01/mmrda-publishes-mumbai-metro-red-line-7s-station-names/). The order always
follows MMMOCL. Line 1 names come directly from the MMOPL English page.

## How the data is stored

The network is seeded by the Flyway migration `src/main/resources/db/migration/V2__seed_network.sql`:
3 lines, 42 stations, 43 line-station nodes, 80 ride edges (40 segments in both directions) and 6 transfer edges.

## Assumptions and limitations

* **Line 7 and Dahisar (East).** MMMOCL states Line 7 has 13 stations from Gundavali to Ovaripada, but its project
  brief lists Dahisar (East) at the end of Line 7, and trains run through to it. MetroPulse models Dahisar (East)
  as one station on both lines (14 Line 7 nodes) with a 4-minute change between them.
* **Interchanges between different stations.** D N Nagar ↔ Andheri (West) and Western Express Highway ↔ Gundavali
  are separate stations joined by walkways; they are modelled as transfer edges between two stations.
* **Travel times are estimates.** No verified inter-station running times were available, so every ride edge is
  2 minutes and interchange walks are 8 minutes (4 minutes inside Dahisar (East)). They are labelled as estimates
  in the interface and documentation. Administrators can change them.
* **Distances and coordinates** are left empty (`NULL`) because they were not verified.
* **Areas** (e.g. "Andheri East") are approximate localities for browsing, not official data.
* **Line 2A ↔ Line 6 (Oshiwara)** and other planned interchanges are not modelled because those lines are not part
  of this prototype.
* **Crowd reports, alerts and facilities** in the demo data are invented samples. They are flagged `is_demo`,
  shown with a "Demo" label, and never presented as operator information.
* Station names can change; the seed reflects the operators' pages on the date above.
