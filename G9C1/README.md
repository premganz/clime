# G9C1 - Factory Floor Animation

A simple Spring Boot project that serves a vanilla JS / HTML5 Canvas animation
of a manufacturing plant floor.

## What it shows

- A factory floor plan (top-down) drawn with routine plan notation.
- A **raw material hold** at the global intake (outside the factory boundary, west gate).
- ~20 **machine stations**, each with an **input buffer** (front pallet) and an
  **output pallet** (behind the station).
- Stations are connected as a **directed graph** by conveyor pathways.
- Small **material units** travel along the pathways, queue in input buffers,
  get processed (briefly held) by the station, then drop on the output pallet
  to be picked up by the next conveyor.
- Finished goods are dispatched to the **outbound dock** at the east gate.

## Stack

- Spring Boot 2.7.18 (Java 21) — only `spring-boot-starter-web`.
- Static `index.html` with vanilla JS + HTML5 Canvas.
- jQuery from a CDN for the side-panel UI controls.

No database, no framework on the front-end - just a render loop.

## Run

```powershell
./startapp-local.ps1
```

Then browse to http://localhost:8091/
