-- =============================================================================
-- URBANPULSE — Schema PostgreSQL 16
-- Archivo de inicialización ejecutado por docker-entrypoint-initdb.d
-- =============================================================================

-- 1. TIPOS ENUMERADOS

CREATE TYPE "user_role" AS ENUM (
    'CITIZEN',
    'OPERATOR',
    'TECHNICIAN',
    'ADMIN',
    'ANALYST'
    );

CREATE TYPE "incident_status" AS ENUM (
    'REPORTED',
    'VALIDATED',
    'REJECTED',
    'ASSIGNED',
    'IN_PROGRESS',
    'RESOLVED',
    'REOPENED',
    'CLOSED'
    );

CREATE TYPE "incident_priority" AS ENUM (
    'LOW',
    'MEDIUM',
    'HIGH',
    'CRITICAL'
    );

-- 2. TABLAS ENTIDADES

CREATE TABLE "departments" (
                               "id"          BIGSERIAL PRIMARY KEY,
                               "name"        varchar(100) UNIQUE NOT NULL,
                               "description" text
);

CREATE TABLE "users" (
                         "id"            BIGSERIAL PRIMARY KEY,
                         "email"         varchar(150) UNIQUE NOT NULL,
                         "password_hash" varchar(255) NOT NULL,
                         "full_name"     varchar(150) NOT NULL,
                         "role"          user_role NOT NULL,
                         "department_id" bigint,
                         "created_at"    timestamptz DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE "incidents" (
                             "id"              BIGSERIAL PRIMARY KEY,
                             "title"           varchar(200) NOT NULL,
                             "description"     text NOT NULL,
                             "category"        varchar(50) NOT NULL,
                             "latitude"        double precision NOT NULL,
                             "longitude"       double precision NOT NULL,
                             "accuracy_meters" double precision,
                             "address"         varchar(255),
                             "district"        varchar(100),
                             "neighborhood"    varchar(100),
                             "status"          incident_status NOT NULL DEFAULT 'REPORTED',
                             "priority"        incident_priority NOT NULL DEFAULT 'MEDIUM',
                             "priority_reason" text,
                             "citizen_id"      bigint NOT NULL,
                             "created_at"      timestamptz DEFAULT CURRENT_TIMESTAMP,
                             "updated_at"      timestamptz DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE "attachments" (
                               "id"              BIGSERIAL PRIMARY KEY,
                               "incident_id"     bigint NOT NULL,
                               "file_name"       varchar(255) NOT NULL,
                               "file_url"        varchar(500) NOT NULL,
                               "mime_type"       varchar(100) NOT NULL,
                               "file_size_bytes" bigint NOT NULL,
                               "uploaded_at"     timestamptz DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE "status_changes" (
                                  "id"                 BIGSERIAL PRIMARY KEY,
                                  "incident_id"        bigint NOT NULL,
                                  "from_status"        varchar(30) NOT NULL,
                                  "to_status"          varchar(30) NOT NULL,
                                  "changed_by_user_id" bigint NOT NULL,
                                  "reason"             text,
                                  "created_at"         timestamptz DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE "assignments" (
                               "id"             BIGSERIAL PRIMARY KEY,
                               "incident_id"    bigint NOT NULL,
                               "department_id"  bigint NOT NULL,
                               "technician_id"  bigint,
                               "assigned_by_id" bigint NOT NULL,
                               "is_active"      boolean DEFAULT true,
                               "assigned_at"    timestamptz DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE "incident_comments" (
                                     "id"          BIGSERIAL PRIMARY KEY,
                                     "incident_id" bigint NOT NULL,
                                     "author_id"   bigint NOT NULL,
                                     "comment"     text NOT NULL,
                                     "is_internal" boolean DEFAULT true,
                                     "created_at"  timestamptz DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE "urban_assets" (
                                "id"         BIGSERIAL PRIMARY KEY,
                                "asset_code" varchar(100) UNIQUE NOT NULL,
                                "type"       varchar(50) NOT NULL,
                                "latitude"   double precision NOT NULL,
                                "longitude"  double precision NOT NULL,
                                "name"       varchar(200)
);

CREATE TABLE "incident_asset_associations" (
                                               "incident_id"      bigint NOT NULL,
                                               "asset_id"         bigint NOT NULL,
                                               "distance_meters"  double precision NOT NULL,
                                               "confidence_score" double precision NOT NULL,
                                               "is_confirmed"     boolean DEFAULT false,
                                               PRIMARY KEY ("incident_id", "asset_id")
);

CREATE TABLE "external_observations" (
                                         "id"          BIGSERIAL PRIMARY KEY,
                                         "source"      varchar(50) NOT NULL,
                                         "metric_type" varchar(50) NOT NULL,
                                         "value_num"   double precision,
                                         "value_text"  varchar(100),
                                         "observed_at" timestamptz NOT NULL
);

CREATE TABLE "incident_contexts" (
                                     "incident_id"    bigint NOT NULL,
                                     "observation_id" bigint NOT NULL,
                                     PRIMARY KEY ("incident_id", "observation_id")
);

-- 3. FOREIGN KEYS

ALTER TABLE "users"
    ADD FOREIGN KEY ("department_id")
        REFERENCES "departments" ("id") ON DELETE SET NULL
        DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "incidents"
    ADD FOREIGN KEY ("citizen_id")
        REFERENCES "users" ("id") ON DELETE RESTRICT
        DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "attachments"
    ADD FOREIGN KEY ("incident_id")
        REFERENCES "incidents" ("id") ON DELETE CASCADE
        DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "status_changes"
    ADD FOREIGN KEY ("incident_id")
        REFERENCES "incidents" ("id") ON DELETE CASCADE
        DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "status_changes"
    ADD FOREIGN KEY ("changed_by_user_id")
        REFERENCES "users" ("id") ON DELETE RESTRICT
        DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "assignments"
    ADD FOREIGN KEY ("incident_id")
        REFERENCES "incidents" ("id") ON DELETE CASCADE
        DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "assignments"
    ADD FOREIGN KEY ("department_id")
        REFERENCES "departments" ("id") ON DELETE RESTRICT
        DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "assignments"
    ADD FOREIGN KEY ("technician_id")
        REFERENCES "users" ("id") ON DELETE SET NULL
        DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "assignments"
    ADD FOREIGN KEY ("assigned_by_id")
        REFERENCES "users" ("id") ON DELETE RESTRICT
        DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "incident_comments"
    ADD FOREIGN KEY ("incident_id")
        REFERENCES "incidents" ("id") ON DELETE CASCADE
        DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "incident_comments"
    ADD FOREIGN KEY ("author_id")
        REFERENCES "users" ("id") ON DELETE RESTRICT
        DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "incident_asset_associations"
    ADD FOREIGN KEY ("incident_id")
        REFERENCES "incidents" ("id") ON DELETE CASCADE
        DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "incident_asset_associations"
    ADD FOREIGN KEY ("asset_id")
        REFERENCES "urban_assets" ("id") ON DELETE CASCADE
        DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "incident_contexts"
    ADD FOREIGN KEY ("incident_id")
        REFERENCES "incidents" ("id") ON DELETE CASCADE
        DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "incident_contexts"
    ADD FOREIGN KEY ("observation_id")
        REFERENCES "external_observations" ("id") ON DELETE CASCADE
        DEFERRABLE INITIALLY IMMEDIATE;

-- 4. ÍNDICES DE RENDIMIENTO

CREATE INDEX idx_incidents_status      ON "incidents" ("status");
CREATE INDEX idx_incidents_category    ON "incidents" ("category");
CREATE INDEX idx_incidents_district    ON "incidents" ("district");
CREATE INDEX idx_incidents_citizen_id  ON "incidents" ("citizen_id");
CREATE INDEX idx_assignments_active    ON "assignments" ("is_active");
CREATE INDEX idx_observations_observed ON "external_observations" ("observed_at");

-- 5. TRIGGER — actualizar incidents.updated_at automáticamente

CREATE OR REPLACE FUNCTION update_incident_timestamp()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_incidents_updated_at
    BEFORE UPDATE ON "incidents"
    FOR EACH ROW
EXECUTE FUNCTION update_incident_timestamp();
