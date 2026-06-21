# VotoSync: Distributed Voting Platform

VotoSync is a complete, production-ready, distributed voting platform built with **Java (Spring Boot)**, **React.js**, **RabbitMQ**, **PostgreSQL** (Master-Slave replication topology), and **Kubernetes**. It is designed to demonstrate high-performance ledger auditing, transactional write safety, network buffering, and high-availability design principles.

---

## 1. Repository Structure

```
VotoSync/
├── backend-services/              # Java Spring Boot Microservices
│   ├── pom.xml                     # Parent Maven Project
│   ├── identity-service/           # User registry & digital signature validations
│   ├── election-service/           # CRUD for elections & candidates
│   ├── vote-service/               # Transactional write-heavy vote registration
│   └── audit-service/              # Decoupled, read-only ledger & counting audit
├── frontend/                       # React.js application
│   ├── src/                        # Dashboard components & premium styles
│   └── Dockerfile                  # Production Nginx host setup
├── k8s-infrastructure/             # Kubernetes Orchestration files
│   ├── postgres.yaml               # Master StatefulSet & Slave Deployment
│   ├── db-init-configmap.yaml      # Schema migration ConfigMap
│   ├── rabbitmq.yaml               # Message Broker deployment
│   ├── identity-service.yaml       # Deployment & ClusterIP
│   ├── election-service.yaml       # Deployment & ClusterIP
│   ├── vote-service.yaml           # Deployment, ClusterIP & HPA configurations
│   ├── audit-service.yaml          # Deployment & ClusterIP (bound to DB Slave)
│   ├── frontend.yaml               # Deployment & LoadBalancer Ingress
│   └── ntp-daemonset.yaml          # Privileged Chrony sync daemon for nodes
├── docker-compose.yml              # Local container deployment orchestrator
├── init.sql                        # SQL Schema and Seed Data
├── start-local.sh                  # Startup shell script
└── start-local.ps1                 # Startup PowerShell script (Windows)
```

---

## 2. Platform Architecture & Services

The platform utilizes a **decoupled microservice architecture**:

```mermaid
graph TD
    Client[React Frontend] -->|Auth/Validate Signature| Identity[Identity Service]
    Client -->|Manage Elections| Election[Election Service]
    Client -->|Cast Vote (Idempotency Key)| VoteService[Vote Registration Service]
    Client -->|View Audits/Standings| Audit[Audit Service]

    VoteService -->|Publish Payload| Rabbit[RabbitMQ Queue]
    Rabbit -->|Consume Sequentially| VoteConsumer[RabbitMQ Consumer in Vote Service]
    VoteConsumer -->|Write Transaction| PG_Master[(PostgreSQL Master)]
    PG_Master -->|Replicate Data| PG_Slave[(PostgreSQL Slave)]

    Audit -->|Read Decoupled Tallies| PG_Slave
    Identity -->|Read/Write Users| PG_Master
    Election -->|Read/Write metadata| PG_Master
```

### 1. VotoSync Authentication/Identity Service (`identity-service` - Port 8081)
- Handles user registration, credentials hashing via BCrypt, and issues secure JWT tokens.
- Simulates external identity verification hooks:
  - **FirmaEc**: Validates cryptographic signature formats.
  - **ARCOTEL**: Verifies mobile registration linkage.
  - **MINTEL**: Confirms civil registry voting eligibility.

### 2. VotoSync Elections Management Service (`election-service` - Port 8082)
- Performs standard CRUD actions for creating elections and candidates.
- Manages ballot status flags (`DRAFT`, `ACTIVE`, `COMPLETED`).

### 3. VotoSync Vote Registration Service (`vote-service` - Port 8083)
- Highly critical transactional module handling vote submissions.
- **Idempotency Key validation layer**: Rejects duplicates matching same client-submitted `Idempotency-Key` headers (fast-returns cached responses).
- **Vote Buffering**: Validated votes are pushed onto a durable RabbitMQ queue (`vote-ingestion-queue`) to absorb peak election day loads.
- **Ordered Consumption**: A sequential queue consumer processes messages, computes physical clock logical times (`max(msgTimestamp, databaseLogicalTime + 1)`), generates SHA-256 hashes, writes to the PostgreSQL Master DB, and alerts the Identity Service to mark the citizen profile as "voted".

### 4. VotoSync Audit and Counting Service (`audit-service` - Port 8084)
- **Decoupled Read Operations**: Reads exclusively from **PostgreSQL Slave replica nodes** to avoid disrupting Master database write performance.
- Exposes real-time candidate tallies and raw transaction ledger lists displaying cryptographic hashes (`SHA-256(voterHash + electionId + candidateId + signature + logicalTimestamp)`).

---

## 3. Database Topology (PostgreSQL Master-Slave)

To prevent locking read operations on counting updates during peak vote ingestion, VotoSync implements database replica streaming:
- **Master Node (`votosync-db-master`)**: Handles all user registrations, election creation, and write transactions for votes. Configured in `wal_level=replica` mode.
- **Slave Node (`votosync-db-slave`)**: Runs in hot-standby standby mode. It replicates master logs and answers all counting/results and audit ledger requests.

---

## 4. Kubernetes Orchestration & Resilience

Orchestrator files are stored in `/k8s-infrastructure`:
- **HPAs**: The `vote-service` is configured with a Horizontal Pod Autoscaler that scales out replicas (from 2 up to 10) based on:
  - Average CPU exceeding 70%.
  - Queue depth (RabbitMQ ready messages per pod exceeding 50).
- **Physical Clock Consensus**: Distributed system logs require matching time offsets. Since container clocks are dependent on local hosts, `ntp-daemonset.yaml` runs `chrony` in privileged mode on all Kubernetes nodes to enforce clock adjustments to standard NTP pools.

---

## 5. Local Setup & Verification

Ensure you have **Docker** and **Docker Compose** installed.

### Automatic Startup
On Unix/Linux:
```bash
chmod +x start-local.sh
./start-local.sh
```

On Windows (PowerShell):
```powershell
./start-local.ps1
```

### Tear Down
To stop and clean all containers and replication volumes:
```bash
docker compose down -v
```
