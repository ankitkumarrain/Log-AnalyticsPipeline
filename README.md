# Real-Time Log Analytics Pipeline

A production-grade streaming pipeline built with **Scala 3**, **FS2**, **Cats Effect**, and **Apache Kafka**.

## Architecture
```
Kafka Topic → FS2 Stream → Parser → Aggregator → PostgreSQL
                                         ↓
                                   Alert Engine → Webhook
                                         ↑
                                    Http4s API
```

## Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| Scala 3 | 3.3.3 | Language |
| FS2 | 3.10.2 | Functional streaming |
| Cats Effect | 3.5.4 | Pure async IO |
| fs2-kafka | 3.5.1 | Kafka consumer |
| Http4s | 0.23.27 | REST API |
| Doobie | 1.0.0-RC5 | PostgreSQL |
| Circe | 0.14.9 | JSON |
| PureConfig | 0.17.7 | Config management |
| munit-cats-effect | 2.0.0 | Testing |

## Features

- Real-time log ingestion from Kafka
- Pure functional parsing with Cats ValidatedNel
- Sliding window aggregation (30s)
- Alert engine with configurable thresholds
- REST API for querying metrics
- 13 unit tests — all passing

## Prerequisites

- JDK 17+
- sbt
- Docker + Docker Compose

## Quick Start
```bash
# Start infrastructure
docker compose up -d

# Run pipeline
sbt run

# Run tests
sbt test
```

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| GET | /health | Health check |
| GET | /metrics | Log counts by level |
| GET | /errors | Recent error messages |
| GET | /alerts | Window-based alerts |

## Key Design Decisions

- **FS2 Streams** — backpressure handling, composable pipelines
- **Cats ValidatedNel** — accumulate all parse errors, no exceptions
- **Cats Effect Resource** — safe lifecycle management, no resource leaks
- **Ref[IO, A]** — concurrent, lock-free in-memory state
- **Pure functions** — referentially transparent, easily testable
