# Stockroom

Warehouse inventory and order-fulfilment service: SKUs, storage bins, a stock-movement ledger,
purchase orders, reservations, pick lists, low-stock alerts and reporting.

Java 21 · Spring Boot 3.3 · Spring Data JPA · Flyway · H2

## Modules

| Module | Contents |
|---|---|
| `stockroom-core` | Domain model, persistence, Flyway migrations, services, in-process event bus |
| `stockroom-reporting` | Read-side reports built on the core ledger (throughput, valuation, revaluations) |
| `stockroom-api` | Spring Boot application: REST controllers, error handling, scheduled jobs |

`stockroom-api` depends on both other modules; `stockroom-reporting` depends on `stockroom-core`.

## Building and running

Requires a JDK 21 on the `PATH` (or `JAVA_HOME`). Maven is provided by the wrapper.

```bash
./mvnw -B test                                  # full test suite, all modules
./mvnw -B -pl stockroom-core test               # one module
./mvnw -B -DskipTests package                   # build the executable jar
java -jar stockroom-api/target/stockroom-api-1.4.0-SNAPSHOT.jar
```

The application stores its data in an H2 file database under `./data/`. Tests use an in-memory
database per test class.

Docker:

```bash
docker build -t stockroom-api .
docker run -p 8080:8080 -v stockroom-data:/app/data stockroom-api
```

Kubernetes manifests live in `k8s/`.

## Configuration

| Property | Default | Meaning |
|---|---|---|
| `stockroom.reservations.ttl` | `PT30M` | How long a reservation holds stock |
| `stockroom.reservations.sweep-interval` | `PT1M` | How often expired reservations are released |
| `stockroom.reservations.sweep-enabled` | `true` | Turn the expiry sweep off (tests do) |
| `stockroom.alerts.enabled` | `true` | Turn low-stock alerting off |

All timestamps are stored and returned as UTC.

## Business rules

**Stock.** Every change to stock is a movement in the ledger: `RECEIPT`, `PICK`, `ADJUSTMENT`,
or a transfer between bins, which is written as a `TRANSFER_OUT` leg on the source bin and a
`TRANSFER_IN` leg on the destination bin sharing one `transferId`. *Available* stock is on-hand
stock minus reserved stock.

**Reservations.** A reservation holds stock for an order for `stockroom.reservations.ttl`
(30 minutes by default). It is taken from the first pickable bin that can cover the whole
quantity; reservations are never split across bins. Extending a reservation restarts the hold
from the moment of the extension. Expired reservations are released by the sweep.

**Low-stock alerts.** An alert is raised when a SKU's available stock drops *below* its reorder
point. A SKU has at most one open alert: while it is unacknowledged, further drops do not raise
another alert, however far stock falls. Acknowledge it (`POST /api/alerts/{id}/acknowledge`) and
the next drop below the reorder point raises a new one. A reorder point of `0` disables alerts
for that SKU.

**Costing.** Each SKU carries a weighted average unit cost (4 decimal places). Receiving a
purchase order blends the received units into the average, using the supplier's invoiced cost
when one is given. Every change of the average is logged as a revaluation.

**Pick lists.** One open pick list per order, covering all of its active reservations. Lines are
ordered along the picker's walking route: by aisle, then by bay number. Completing a pick list
ships the reserved stock and fulfils the reservations.

## API overview

| Method & path | Purpose |
|---|---|
| `POST /api/skus` · `GET /api/skus` · `GET /api/skus/{id}` | Create / list / read SKUs |
| `PATCH /api/skus/{id}` | Partial update. Omitted fields are left unchanged; `"description": null` clears the description |
| `POST /api/locations` · `POST /api/locations/{id}/bins` | Warehouses and bins |
| `PUT /api/locations/bins/{id}/pickable` | Take a bin in or out of picking |
| `GET /api/skus/{id}/stock` · `.../movements` | Stock by bin, and the ledger |
| `POST /api/skus/{id}/stock/receipts` · `/adjustments` · `/transfers` | Stock movements |
| `POST /api/reservations` · `.../{ref}/release` · `.../{ref}/extend` | Reservations |
| `POST /api/purchase-orders` · `.../{id}/submit` · `.../{id}/receipts` | Purchasing |
| `POST /api/pick-lists` · `.../{id}/complete` · `GET /api/pick-lists/summary` | Picking |
| `GET /api/alerts` · `POST /api/alerts/{id}/acknowledge` | Low-stock alerts |
| `GET /api/reports/throughput?from=&to=` | Units handled per UTC day range (both dates inclusive) |
| `GET /api/reports/valuation` · `GET /api/reports/revaluations/{sku}` | Stock value, cost history |
| `GET /actuator/health` | Health, plus `/liveness` and `/readiness` probe groups |

Errors use one envelope: `{"status": 409, "code": "INSUFFICIENT_STOCK", "message": "...", "fields": []}`.
