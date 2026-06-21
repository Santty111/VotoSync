-- init.sql: VotoSync Database Schema and Initial Seed Data

-- Create Replication Role for Standby Replica
CREATE ROLE replicator WITH REPLICATION PASSWORD 'replicator_password' LOGIN;

-- Dynamically append replication permissions to pg_hba.conf and reload config
COPY (SELECT 1) TO PROGRAM 'echo "host replication replicator 0.0.0.0/0 md5" >> "$PGDATA/pg_hba.conf"';
SELECT pg_reload_conf();

-- 1. Identity & Citizens Service
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    national_id VARCHAR(10) UNIQUE NOT NULL, -- Cedula (Ecuador)
    full_name VARCHAR(100) NOT NULL,
    fingerprint_hash VARCHAR(256),
    has_voted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Elections Management Service
CREATE TABLE IF NOT EXISTS elections (
    id SERIAL PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(20) DEFAULT 'DRAFT', -- DRAFT, ACTIVE, COMPLETED
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS candidates (
    id SERIAL PRIMARY KEY,
    election_id INT REFERENCES elections(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    party VARCHAR(100) NOT NULL,
    photo_url VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. Vote Registration & Auditing Service
CREATE TABLE IF NOT EXISTS votes (
    id SERIAL PRIMARY KEY,
    election_id INT REFERENCES elections(id) ON DELETE CASCADE,
    candidate_id INT REFERENCES candidates(id) ON DELETE CASCADE,
    citizen_id_hash VARCHAR(64) NOT NULL, -- SHA-256 hash of national_id for privacy
    transaction_hash VARCHAR(64) UNIQUE NOT NULL, -- SHA-256 of vote details (citizen_id_hash + election_id + candidate_id + signature + logical_timestamp)
    signature TEXT NOT NULL, -- Simulated FirmaEc electronic signature hash
    logical_timestamp BIGINT NOT NULL, -- Sequential logical time of vote registration
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index to enforce "One Citizen, One Vote" constraint per election at the database level
CREATE UNIQUE INDEX IF NOT EXISTS idx_unique_citizen_election_vote 
ON votes (election_id, citizen_id_hash);

-- Idempotency Keys table to block duplicate HTTP requests
CREATE TABLE IF NOT EXISTS idempotency_keys (
    key VARCHAR(64) PRIMARY KEY,
    response_status INT NOT NULL,
    response_body TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Seed Data for Testing
INSERT INTO users (username, password, email, national_id, full_name, fingerprint_hash, has_voted) VALUES
('citizen1', '$2a$10$Y50OB19A71a1795OQ47wSO9B172qK0lqB4aG4qV2d7pC521K16eW2', 'citizen1@votosync.gob.ec', '1723456789', 'Juan Carlos Perez', '0a1b2c3d4e5f6g7h', FALSE),
('citizen2', '$2a$10$Y50OB19A71a1795OQ47wSO9B172qK0lqB4aG4qV2d7pC521K16eW2', 'citizen2@votosync.gob.ec', '0923456781', 'Maria Elena Santos', '1h2g3f4e5d6c7b8a', FALSE),
('citizen3', '$2a$10$Y50OB19A71a1795OQ47wSO9B172qK0lqB4aG4qV2d7pC521K16eW2', 'citizen3@votosync.gob.ec', '0102345678', 'Luis Alberto Gomez', 'f8e7d6c5b4a32100', FALSE);

INSERT INTO elections (title, description, status, start_date, end_date) VALUES
('Presidential Elections 2026', 'National election to choose the next president of Ecuador.', 'ACTIVE', '2026-06-18 00:00:00', '2026-06-25 23:59:59'),
('Constitutional Referendum 2026', 'Referendum regarding judicial reforms and security protocols.', 'DRAFT', '2026-09-01 00:00:00', '2026-09-02 23:59:59');

-- Candidates for Presidential Elections (Assuming election_id = 1)
INSERT INTO candidates (election_id, name, party, photo_url) VALUES
(1, 'Diana Salazar', 'Movimiento Alianza Libertad', 'https://api.dicebear.com/7.x/bottts/svg?seed=Diana'),
(1, 'Christian Zurita', 'Partido Renovación Democrática', 'https://api.dicebear.com/7.x/bottts/svg?seed=Christian'),
(1, 'Blank Vote', 'N/A', 'https://api.dicebear.com/7.x/bottts/svg?seed=Blank');
