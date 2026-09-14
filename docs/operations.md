# Operations runbook

## Health

The service exposes Spring Boot Actuator health at `/actuator/health`, with the Kubernetes probe
groups `/actuator/health/liveness` and `/actuator/health/readiness`. The deployment in `k8s/`
wires both probes; the container image declares its own `HEALTHCHECK` for plain Docker hosts.

## Data

H2 runs in file mode. In Kubernetes the database lives on the `stockroom-data` volume mounted at
`/app/data`; the deployment uses the `Recreate` strategy so two pods never open the file at once.

Back up by copying `stockroom.mv.db` while the pod is scaled to zero.

## Schema changes

Flyway applies `stockroom-core/src/main/resources/db/migration` on start-up. Migrations are
append-only: never edit an applied migration, add the next `V<n>__description.sql` instead.

## Time zones

Timestamps are stored as UTC. The `stockroom-config` ConfigMap sets `TZ=UTC` for the pod.

## Common tasks

| Task | How |
|---|---|
| Release stuck reservations | `POST /api/reservations/{ref}/release` |
| Silence alerts during a stock-take | set `STOCKROOM_ALERTS_ENABLED=false` and restart |
| Quarantine a bin | `PUT /api/locations/bins/{id}/pickable` with `{"pickable": false}` |
