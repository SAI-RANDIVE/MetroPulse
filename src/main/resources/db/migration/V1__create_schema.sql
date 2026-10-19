-- MetroPulse schema (PostgreSQL). All timestamps are timezone-aware (timestamptz).

CREATE TABLE metro_line (
    id            BIGSERIAL PRIMARY KEY,
    line_code     VARCHAR(10)  NOT NULL UNIQUE,
    name          VARCHAR(120) NOT NULL,
    color         VARCHAR(7)   NOT NULL CHECK (color ~ '^#[0-9A-Fa-f]{6}$'),
    text_color    VARCHAR(7)   NOT NULL DEFAULT '#FFFFFF' CHECK (text_color ~ '^#[0-9A-Fa-f]{6}$'),
    operator_name VARCHAR(120),
    status        VARCHAR(20)  NOT NULL CHECK (status IN ('OPERATIONAL', 'PARTIAL', 'SUSPENDED')),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE station (
    id         BIGSERIAL PRIMARY KEY,
    code       VARCHAR(40)  NOT NULL UNIQUE CHECK (code ~ '^[A-Z0-9_]+$'),
    name       VARCHAR(120) NOT NULL UNIQUE,
    area       VARCHAR(120),
    latitude   NUMERIC(9, 6) CHECK (latitude BETWEEN -90 AND 90),
    longitude  NUMERIC(9, 6) CHECK (longitude BETWEEN -180 AND 180),
    status     VARCHAR(20)  NOT NULL CHECK (status IN ('OPEN', 'CLOSED')),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- A station's membership of one line. These rows are the nodes of the routing graph.
CREATE TABLE line_station (
    id             BIGSERIAL PRIMARY KEY,
    line_id        BIGINT  NOT NULL REFERENCES metro_line (id),
    station_id     BIGINT  NOT NULL REFERENCES station (id),
    sequence_no    INTEGER NOT NULL CHECK (sequence_no > 0),
    is_operational BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_line_station UNIQUE (line_id, station_id),
    CONSTRAINT uq_line_sequence UNIQUE (line_id, sequence_no)
);
CREATE INDEX idx_line_station_station ON line_station (station_id);

-- Directed connection between two line-specific nodes. Both directions are stored explicitly.
CREATE TABLE network_edge (
    id           BIGSERIAL PRIMARY KEY,
    from_node_id BIGINT      NOT NULL REFERENCES line_station (id),
    to_node_id   BIGINT      NOT NULL REFERENCES line_station (id),
    edge_type    VARCHAR(10) NOT NULL CHECK (edge_type IN ('RIDE', 'TRANSFER')),
    base_minutes INTEGER     NOT NULL CHECK (base_minutes >= 0),
    distance_km  NUMERIC(6, 2) CHECK (distance_km >= 0),
    is_active    BOOLEAN     NOT NULL DEFAULT TRUE,
    note         VARCHAR(200),
    CONSTRAINT chk_edge_not_loop CHECK (from_node_id <> to_node_id),
    CONSTRAINT uq_edge UNIQUE (from_node_id, to_node_id, edge_type)
);
CREATE INDEX idx_edge_to_node ON network_edge (to_node_id);

CREATE TABLE app_user (
    id            BIGSERIAL PRIMARY KEY,
    display_name  VARCHAR(80)  NOT NULL,
    email         VARCHAR(160) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL CHECK (role IN ('COMMUTER', 'ADMIN')),
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uq_app_user_email ON app_user (lower(email));

CREATE TABLE crowd_report (
    id                  BIGSERIAL PRIMARY KEY,
    line_station_id     BIGINT      NOT NULL REFERENCES line_station (id),
    user_id             BIGINT REFERENCES app_user (id),
    crowd_level         VARCHAR(20) NOT NULL CHECK (crowd_level IN ('LOW', 'MODERATE', 'HIGH', 'VERY_HIGH')),
    comment             VARCHAR(280),
    verification_status VARCHAR(20) NOT NULL CHECK (verification_status IN ('PENDING', 'APPROVED', 'REJECTED')),
    created_at          TIMESTAMPTZ NOT NULL,
    expires_at          TIMESTAMPTZ NOT NULL,
    verified_by         BIGINT REFERENCES app_user (id),
    verified_at         TIMESTAMPTZ,
    is_demo             BOOLEAN     NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_report_expiry CHECK (expires_at > created_at),
    -- Only clearly flagged demonstration records may exist without a submitting user
    CONSTRAINT chk_report_user CHECK (user_id IS NOT NULL OR is_demo)
);
CREATE INDEX idx_crowd_report_node_expiry ON crowd_report (line_station_id, expires_at);
CREATE INDEX idx_crowd_report_status_created ON crowd_report (verification_status, created_at DESC);
CREATE INDEX idx_crowd_report_user_created ON crowd_report (user_id, created_at DESC);

CREATE TABLE service_alert (
    id            BIGSERIAL PRIMARY KEY,
    line_id       BIGINT REFERENCES metro_line (id),
    station_id    BIGINT REFERENCES station (id),
    title         VARCHAR(140)  NOT NULL,
    description   VARCHAR(2000) NOT NULL,
    alert_type    VARCHAR(30)   NOT NULL CHECK (alert_type IN ('SERVICE_DISRUPTION', 'FACILITY_ISSUE', 'ACCESS_RESTRICTION', 'OTHER')),
    severity      VARCHAR(20)   NOT NULL CHECK (severity IN ('INFO', 'WARNING', 'CRITICAL')),
    impact        VARCHAR(20)   NOT NULL CHECK (impact IN ('INFORMATION', 'DELAY', 'SUSPENDED')),
    delay_minutes INTEGER       NOT NULL DEFAULT 0 CHECK (delay_minutes BETWEEN 0 AND 180),
    status        VARCHAR(20)   NOT NULL CHECK (status IN ('DRAFT', 'ACTIVE', 'RESOLVED', 'CANCELLED')),
    starts_at     TIMESTAMPTZ   NOT NULL,
    ends_at       TIMESTAMPTZ,
    created_by    BIGINT REFERENCES app_user (id),
    verified_by   BIGINT REFERENCES app_user (id),
    verified_at   TIMESTAMPTZ,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    is_demo       BOOLEAN       NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_alert_target CHECK (line_id IS NOT NULL OR station_id IS NOT NULL),
    CONSTRAINT chk_alert_window CHECK (ends_at IS NULL OR ends_at > starts_at),
    CONSTRAINT chk_alert_creator CHECK (created_by IS NOT NULL OR is_demo)
);
CREATE INDEX idx_service_alert_status ON service_alert (status, starts_at);

-- Connections affected by an alert (for example a suspended line segment).
CREATE TABLE alert_affected_edge (
    alert_id BIGINT NOT NULL REFERENCES service_alert (id) ON DELETE CASCADE,
    edge_id  BIGINT NOT NULL REFERENCES network_edge (id),
    PRIMARY KEY (alert_id, edge_id)
);

CREATE TABLE station_facility (
    id               BIGSERIAL PRIMARY KEY,
    station_id       BIGINT      NOT NULL REFERENCES station (id),
    facility_type    VARCHAR(30) NOT NULL CHECK (facility_type IN ('LIFT', 'ESCALATOR', 'WHEELCHAIR_ACCESS', 'ACCESSIBLE_ENTRANCE', 'RESTROOM', 'PARKING', 'OTHER')),
    status           VARCHAR(20) NOT NULL CHECK (status IN ('AVAILABLE', 'OUT_OF_SERVICE', 'UNKNOWN')),
    description      VARCHAR(300),
    source           VARCHAR(200),
    last_verified_at TIMESTAMPTZ,
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_demo          BOOLEAN     NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_station_facility_station ON station_facility (station_id);

-- Administrative actions. Never stores passwords, hashes, tokens or secrets.
CREATE TABLE audit_log (
    id          BIGSERIAL PRIMARY KEY,
    actor_id    BIGINT REFERENCES app_user (id),
    actor_email VARCHAR(160),
    action      VARCHAR(60)  NOT NULL,
    entity_type VARCHAR(60)  NOT NULL,
    entity_id   BIGINT,
    details     VARCHAR(500),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_log_created ON audit_log (created_at DESC);
