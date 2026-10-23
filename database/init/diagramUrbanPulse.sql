-- =============================================================================
-- URBANPULSE — Schema PostgreSQL 16
-- Archivo de inicialización ejecutado por docker-entrypoint-initdb.d
-- =============================================================================
-- Compatibilidad: Hibernate @Enumerated(EnumType.STRING) + ddl-auto=update
-- Se usan VARCHAR + CHECK en lugar de CREATE TYPE ENUM nativo.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. TABLAS
-- ---------------------------------------------------------------------------

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
    "role"          varchar(30) NOT NULL
                    CHECK ("role" IN ('CITIZEN','OPERATOR','TECHNICIAN','ADMIN','ANALYST')),
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
    "status"          varchar(30) NOT NULL DEFAULT 'REPORTED'
                      CHECK ("status" IN ('REPORTED','VALIDATED','REJECTED','ASSIGNED',
                                          'IN_PROGRESS','RESOLVED','REOPENED','CLOSED')),
    "priority"        varchar(20) NOT NULL DEFAULT 'MEDIUM'
                      CHECK ("priority" IN ('LOW','MEDIUM','HIGH','CRITICAL')),
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

-- ---------------------------------------------------------------------------
-- 2. FOREIGN KEYS
-- ---------------------------------------------------------------------------

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

-- ---------------------------------------------------------------------------
-- 3. ÍNDICES DE RENDIMIENTO
-- ---------------------------------------------------------------------------

CREATE INDEX idx_incidents_status      ON "incidents" ("status");
CREATE INDEX idx_incidents_category    ON "incidents" ("category");
CREATE INDEX idx_incidents_district    ON "incidents" ("district");
CREATE INDEX idx_incidents_citizen_id  ON "incidents" ("citizen_id");
CREATE INDEX idx_assignments_active    ON "assignments" ("is_active");
CREATE INDEX idx_observations_observed ON "external_observations" ("observed_at");

-- ---------------------------------------------------------------------------
-- 4. TRIGGER — actualizar incidents.updated_at automáticamente
-- ---------------------------------------------------------------------------

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

-- ---------------------------------------------------------------------------
-- 5. DATOS SEED — Pequeño conjunto de datos para probar Hu01 y Hu02
-- ---------------------------------------------------------------------------
-- Contraseñas (texto plano → cifradas con ByCript):
--   ADMIN:    Admin1234
--   OPERATOR: Operator1234
--   CITIZEN:  Citizen1234
-- ---------------------------------------------------------------------------

-- Departamentos
INSERT INTO "departments" ("name", "description") VALUES
    ('Infraestructuras y Obras Publicas', 'Mantenimiento de vias, aceras, alumbrado y mobiliario urbano'),
    ('Medio Ambiente y Limpieza',         'Gestion de residuos, parques, jardines y calidad ambiental');

-- Usuario ADMIN (sin departamento)
INSERT INTO "users" ("email", "password_hash", "full_name", "role") VALUES
    ('admin@urbanpulse.es',
     '$2a$10$Y7F7.oelmVhYNao16qx5YuoJXpNvs8rTrXtD9NL0xVTaGmSQtkH8C',
     'Administrador UrbanPulse',
     'ADMIN');

-- Usuario OPERATOR (asignado a departamento 1)
INSERT INTO "users" ("email", "password_hash", "full_name", "role", "department_id") VALUES
    ('operator@urbanpulse.es',
     '$2a$10$kEg5179HmUK6Q6QuHFs6BOffSHD6G.Nx/r5GPoV9Xz/4IYt3SeVLu',
     'Operador de Infraestructuras',
     'OPERATOR',
     1);

-- Usuario CITIZEN (para tener una incidencia de prueba)
INSERT INTO "users" ("email", "password_hash", "full_name", "role") VALUES
    ('citizen@urbanpulse.es',
     '$2a$10$M/97.oo2xEPzZ8hk/6BAAOSC.WgHcpyyrNugPXAXiIDMVSDYQJ0Ea',
     'Maria Garcia Lopez',
     'CITIZEN');

-- Incidencia de prueba (citizen_id = 3, el ciudadano insertado arriba)
INSERT INTO "incidents" ("title", "description", "category", "latitude", "longitude",
                         "address", "district", "neighborhood", "status", "priority", "citizen_id")
VALUES
    ('Farola apagada en Calle Mayor',
     'La farola situada frente al numero 42 de la Calle Mayor lleva tres dias sin funcionar, dejando la acera completamente a oscuras por la noche.',
     'Alumbrado',
     39.4699,
     -0.3763,
     'Calle Mayor 42',
     'Ciutat Vella',
     'El Mercat',
     'REPORTED',
     'MEDIUM',
     3);
