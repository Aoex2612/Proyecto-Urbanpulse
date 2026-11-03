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
    ('Farola apagada en Calle Larios',
     'La farola situada frente al numero 42 de la Calle Mayor lleva tres dias sin funcionar, dejando la acera completamente a oscuras por la noche.',
     'Alumbrado',
     36.7196,
     -4.4200,
     'Calle Marques de Larios 10',
     'Distrito Centro',
     'Centro Historico',
     'REPORTED',
     'MEDIUM',
     3);


-- ---------------------------------------------------------------------------
-- 6. DATOS DE PRUEBA MASIVOS (HASTA COMPLETAR 5.000 INCIDENCIAS Y RELACIONADOS)
-- ---------------------------------------------------------------------------

-- 6.1. Departamentos adicionales
INSERT INTO "departments" ("name", "description") VALUES
    ('Movilidad y Trafico',                    'Gestion semaforica, senalizacion vial, carril bici y control de trafico'),
    ('Parques y Jardines',                     'Mantenimiento de zonas verdes, arbolado urbano y areas infantiles'),
    ('Seguridad Ciudadana y Proteccion Civil', 'Vigilancia urbana, proteccion civil y prevencion de riesgos'),
    ('Aguas y Saneamiento',                    'Red de abastecimiento, colectores, imbornales y alcantarillado')
ON CONFLICT ("name") DO NOTHING;

-- 6.2. Usuarios adicionales (Operadores, Tecnicos, Analistas y Ciudadanos)
-- Credenciales de acceso para pruebas:
--   OPERADORES / TECNICOS / ANALISTAS: Operator1234
--   CIUDADANOS:                       Citizen1234
INSERT INTO "users" ("email", "password_hash", "full_name", "role", "department_id") VALUES
    -- Operadores sectoriales
    ('operator.medioambiente@urbanpulse.es', '$2a$10$kEg5179HmUK6Q6QuHFs6BOffSHD6G.Nx/r5GPoV9Xz/4IYt3SeVLu', 'Operador Medio Ambiente', 'OPERATOR', 2),
    ('operator.movilidad@urbanpulse.es',     '$2a$10$kEg5179HmUK6Q6QuHFs6BOffSHD6G.Nx/r5GPoV9Xz/4IYt3SeVLu', 'Operador Movilidad',        'OPERATOR', 3),
    ('operator.parques@urbanpulse.es',       '$2a$10$kEg5179HmUK6Q6QuHFs6BOffSHD6G.Nx/r5GPoV9Xz/4IYt3SeVLu', 'Operador Parques',          'OPERATOR', 4),
    ('operator.aguas@urbanpulse.es',         '$2a$10$kEg5179HmUK6Q6QuHFs6BOffSHD6G.Nx/r5GPoV9Xz/4IYt3SeVLu', 'Operador Aguas',            'OPERATOR', 6),
    -- Tecnicos de campo
    ('tech.obras1@urbanpulse.es',            '$2a$10$kEg5179HmUK6Q6QuHFs6BOffSHD6G.Nx/r5GPoV9Xz/4IYt3SeVLu', 'Alvaro Gomez (Tecnico Vias)',     'TECHNICIAN', 1),
    ('tech.obras2@urbanpulse.es',            '$2a$10$kEg5179HmUK6Q6QuHFs6BOffSHD6G.Nx/r5GPoV9Xz/4IYt3SeVLu', 'Ignacio Mora (Tecnico Alumbrado)', 'TECHNICIAN', 1),
    ('tech.limpieza1@urbanpulse.es',         '$2a$10$kEg5179HmUK6Q6QuHFs6BOffSHD6G.Nx/r5GPoV9Xz/4IYt3SeVLu', 'Beatriz Cano (Tecnica Residuos)',  'TECHNICIAN', 2),
    ('tech.movilidad1@urbanpulse.es',        '$2a$10$kEg5179HmUK6Q6QuHFs6BOffSHD6G.Nx/r5GPoV9Xz/4IYt3SeVLu', 'Daniel Rivas (Tecnico Trafico)',   'TECHNICIAN', 3),
    ('tech.parques1@urbanpulse.es',          '$2a$10$kEg5179HmUK6Q6QuHFs6BOffSHD6G.Nx/r5GPoV9Xz/4IYt3SeVLu', 'Raquel Ortiz (Tecnica Parques)',   'TECHNICIAN', 4),
    ('tech.aguas1@urbanpulse.es',            '$2a$10$kEg5179HmUK6Q6QuHFs6BOffSHD6G.Nx/r5GPoV9Xz/4IYt3SeVLu', 'Victor Leon (Tecnico Aguas)',      'TECHNICIAN', 6),
    -- Analistas
    ('analyst.central@urbanpulse.es',        '$2a$10$kEg5179HmUK6Q6QuHFs6BOffSHD6G.Nx/r5GPoV9Xz/4IYt3SeVLu', 'Laura Soler (Analista Datos)',    'ANALYST', NULL),
    ('analyst.movilidad@urbanpulse.es',      '$2a$10$kEg5179HmUK6Q6QuHFs6BOffSHD6G.Nx/r5GPoV9Xz/4IYt3SeVLu', 'Javier Santos (Analista Trafico)', 'ANALYST', 3),
    -- Ciudadanos adicionales
    ('carlos.sanchez@urbanpulse.es',   '$2a$10$M/97.oo2xEPzZ8hk/6BAAOSC.WgHcpyyrNugPXAXiIDMVSDYQJ0Ea', 'Carlos Sanchez Ruiz',       'CITIZEN', NULL),
    ('lucia.fernandez@urbanpulse.es',  '$2a$10$M/97.oo2xEPzZ8hk/6BAAOSC.WgHcpyyrNugPXAXiIDMVSDYQJ0Ea', 'Lucia Fernandez Moreno',    'CITIZEN', NULL),
    ('javier.martinez@urbanpulse.es',  '$2a$10$M/97.oo2xEPzZ8hk/6BAAOSC.WgHcpyyrNugPXAXiIDMVSDYQJ0Ea', 'Javier Martinez Ortiz',    'CITIZEN', NULL),
    ('ana.gomez@urbanpulse.es',        '$2a$10$M/97.oo2xEPzZ8hk/6BAAOSC.WgHcpyyrNugPXAXiIDMVSDYQJ0Ea', 'Ana Gomez Navarro',          'CITIZEN', NULL),
    ('david.jimenez@urbanpulse.es',    '$2a$10$M/97.oo2xEPzZ8hk/6BAAOSC.WgHcpyyrNugPXAXiIDMVSDYQJ0Ea', 'David Jimenez Romero',      'CITIZEN', NULL),
    ('elena.torres@urbanpulse.es',     '$2a$10$M/97.oo2xEPzZ8hk/6BAAOSC.WgHcpyyrNugPXAXiIDMVSDYQJ0Ea', 'Elena Torres Castro',       'CITIZEN', NULL),
    ('pablo.ruiz@urbanpulse.es',       '$2a$10$M/97.oo2xEPzZ8hk/6BAAOSC.WgHcpyyrNugPXAXiIDMVSDYQJ0Ea', 'Pablo Ruiz Diaz',           'CITIZEN', NULL),
    ('laura.alvarez@urbanpulse.es',    '$2a$10$M/97.oo2xEPzZ8hk/6BAAOSC.WgHcpyyrNugPXAXiIDMVSDYQJ0Ea', 'Laura Alvarez Gil',          'CITIZEN', NULL),
    ('sergio.dominguez@urbanpulse.es', '$2a$10$M/97.oo2xEPzZ8hk/6BAAOSC.WgHcpyyrNugPXAXiIDMVSDYQJ0Ea', 'Sergio Dominguez Morales',  'CITIZEN', NULL),
    ('marta.vazquez@urbanpulse.es',    '$2a$10$M/97.oo2xEPzZ8hk/6BAAOSC.WgHcpyyrNugPXAXiIDMVSDYQJ0Ea', 'Marta Vazquez Blanco',      'CITIZEN', NULL),
    ('antonio.serrano@urbanpulse.es',  '$2a$10$M/97.oo2xEPzZ8hk/6BAAOSC.WgHcpyyrNugPXAXiIDMVSDYQJ0Ea', 'Antonio Serrano Ramos',    'CITIZEN', NULL),
    ('carmen.munoz@urbanpulse.es',     '$2a$10$M/97.oo2xEPzZ8hk/6BAAOSC.WgHcpyyrNugPXAXiIDMVSDYQJ0Ea', 'Carmen Munoz Delgado',       'CITIZEN', NULL),
    ('alberto.marin@urbanpulse.es',    '$2a$10$M/97.oo2xEPzZ8hk/6BAAOSC.WgHcpyyrNugPXAXiIDMVSDYQJ0Ea', 'Alberto Marin Molina',      'CITIZEN', NULL),
    ('patricia.cano@urbanpulse.es',    '$2a$10$M/97.oo2xEPzZ8hk/6BAAOSC.WgHcpyyrNugPXAXiIDMVSDYQJ0Ea', 'Patricia Cano Santos',      'CITIZEN', NULL),
    ('manuel.prieto@urbanpulse.es',    '$2a$10$M/97.oo2xEPzZ8hk/6BAAOSC.WgHcpyyrNugPXAXiIDMVSDYQJ0Ea', 'Manuel Prieto Calvo',      'CITIZEN', NULL),
    ('rocio.gallego@urbanpulse.es',    '$2a$10$M/97.oo2xEPzZ8hk/6BAAOSC.WgHcpyyrNugPXAXiIDMVSDYQJ0Ea', 'Rocio Gallego Cruz',        'CITIZEN', NULL),
    ('adrian.vidal@urbanpulse.es',     '$2a$10$M/97.oo2xEPzZ8hk/6BAAOSC.WgHcpyyrNugPXAXiIDMVSDYQJ0Ea', 'Adrian Vidal Leon',         'CITIZEN', NULL),
    ('beatriz.herrera@urbanpulse.es',  '$2a$10$M/97.oo2xEPzZ8hk/6BAAOSC.WgHcpyyrNugPXAXiIDMVSDYQJ0Ea', 'Beatriz Herrera Marquez',   'CITIZEN', NULL),
    ('gonzalo.pena@urbanpulse.es',     '$2a$10$M/97.oo2xEPzZ8hk/6BAAOSC.WgHcpyyrNugPXAXiIDMVSDYQJ0Ea', 'Gonzalo Pena Cabrera',      'CITIZEN', NULL),
    ('silvia.flores@urbanpulse.es',    '$2a$10$M/97.oo2xEPzZ8hk/6BAAOSC.WgHcpyyrNugPXAXiIDMVSDYQJ0Ea', 'Silvia Flores Vega',        'CITIZEN', NULL)
ON CONFLICT ("email") DO NOTHING;

-- 6.3. Activos urbanos de prueba (60 activos en Malaga)
INSERT INTO "urban_assets" ("asset_code", "type", "latitude", "longitude", "name")
SELECT
    'AST-' || lpad(a.i::text, 4, '0'),
    (ARRAY['LUMINARIA_LED', 'CONTENEDOR_RESTO', 'CONTENEDOR_SELECTIVO', 'BANCO_MADERA',
           'SEMAFORO_PEATONAL', 'FUENTE_POTABLE', 'JUEGO_INFANTIL', 'IMBORNAL_SIFONICO'])[1 + (a.i % 8)],
    round((36.7200 + (sin(a.i::numeric * 1.5) * 0.025))::numeric, 6),
    round((-4.4250 + (cos(a.i::numeric * 1.5) * 0.035))::numeric, 6),
    'Elemento urbano ' || (ARRAY['Luminaria LED', 'Contenedor Residuos', 'Contenedor Selectivo', 'Banco Urbano',
                                  'Semaforo Peatonal', 'Fuente Agua', 'Area Recreo Infantil', 'Imbornal Pluvial'])[1 + (a.i % 8)] || ' #' || a.i
FROM generate_series(1, 60) AS a(i)
ON CONFLICT ("asset_code") DO NOTHING;

-- 6.4. Generacion de 4.999 incidencias adicionales (completando exactamente 5.000 incidencias)
INSERT INTO "incidents" (
    "title",
    "description",
    "category",
    "latitude",
    "longitude",
    "accuracy_meters",
    "address",
    "district",
    "neighborhood",
    "status",
    "priority",
    "priority_reason",
    "citizen_id",
    "created_at",
    "updated_at"
)
WITH raw_data AS (
    SELECT
        s.i,
        -- Categoria
        (ARRAY[
            'Alumbrado',
            'Pavimento y Aceras',
            'Limpieza y Basuras',
            'Mobiliario Urbano',
            'Parques y Jardines',
            'Trafico y Senalizacion',
            'Saneamiento y Alcantarillado',
            'Pintadas y Vandalismo'
        ])[1 + (s.i % 8)] AS cat,
        -- Distrito y barrio (Malaga)
        (ARRAY[
            'Distrito Centro', 'Distrito Este', 'Distrito Ciudad Jardin',
            'Distrito Bailen-Miraflores', 'Distrito Palma-Palmilla', 'Distrito Cruz de Humilladero',
            'Distrito Carretera de Cadiz', 'Distrito Churriana', 'Distrito Campanillas',
            'Distrito Puerto de la Torre', 'Distrito Teatinos-Universidad'
        ])[1 + (s.i % 11)] AS dist,
        (ARRAY[
            'Centro Historico', 'La Malagueta', 'La Merced', 'El Perchel',
            'El Palo', 'Pedregalejo', 'El Limonar', 'Miraflores del Palo',
            'Ciudad Jardin', 'Mangas Verdes', 'Parque del Sur',
            'Gamarra', 'Nueva Malaga', 'Suarez', 'Miraflores de los Angeles',
            'La Palma', 'La Palmilla', 'Virreina',
            'Cruz de Humilladero', 'Los Tilos', 'Poligono Alameda',
            'Huelin', 'La Luz', 'La Paz', 'Puerta Blanca',
            'Churriana Centro', 'Guadalmar',
            'Campanillas', 'Santa Rosalia', 'Parque Tecnologico',
            'Puerto de la Torre', 'Fuente Alegre',
            'Teatinos', 'Colonia Santa Ines', 'El Consul'
        ])[1 + (s.i % 35)] AS neigh,
        -- Calle y numero
        (ARRAY[
            'Calle Marques de Larios', 'Alameda Principal', 'Calle Granada', 'Calle Carreteria',
            'Calle Compania', 'Paseo del Parque', 'Paseo Maritimo Antonio Machado', 'Paseo Maritimo Ciudad de Melilla',
            'Paseo de Reding', 'Avenida de Andalucia', 'Avenida de Velazquez', 'Avenida de Carlos Haya',
            'Avenida Plutarco', 'Calle Hilera', 'Calle Marmoles', 'Calle Victoria',
            'Calle Alamos', 'Calle San Jacinto', 'Avenida de Cervantes', 'Calle Pacifico',
            'Camino del Colmenar', 'Calle Martinez Maldonado', 'Avenida de Europa', 'Calle Salitre',
            'Calle Cuarteles', 'Avenida Jane Bowles', 'Avenida Doctor Galvez Ginachero', 'Calle Cristo de la Epidemia',
            'Calle Armengual de la Mota', 'Calle Muelle de Heredia', 'Calle Trinidad', 'Calle Eugenio Gross',
            'Avenida Jorge Luis Borges', 'Calle Navarro Ledesma', 'Calle Mesonero Romanos', 'Calle Heroe de Sostoa',
            'Avenida de la Aurora', 'Calle La Union', 'Avenida de la Paloma', 'Calle Compositor Lehmberg Ruiz'
        ])[1 + (s.i % 40)] || ' ' || (1 + (s.i * 7) % 180) AS addr,
        -- Estado
        (ARRAY[
            'REPORTED', 'REPORTED', 'REPORTED', 'REPORTED', 'REPORTED',
            'VALIDATED', 'VALIDATED', 'VALIDATED',
            'ASSIGNED', 'ASSIGNED', 'ASSIGNED', 'ASSIGNED',
            'IN_PROGRESS', 'IN_PROGRESS', 'IN_PROGRESS',
            'RESOLVED', 'RESOLVED', 'RESOLVED',
            'CLOSED',
            'REJECTED'
        ])[1 + (s.i % 20)] AS st,
        -- Prioridad
        (ARRAY[
            'LOW', 'LOW',
            'MEDIUM', 'MEDIUM', 'MEDIUM', 'MEDIUM',
            'HIGH', 'HIGH',
            'CRITICAL'
        ])[1 + (s.i % 9)] AS prio,
        -- Ciudadano asociado
        (ARRAY(SELECT id FROM users WHERE role = 'CITIZEN' ORDER BY id))[1 + (s.i % (SELECT count(*) FROM users WHERE role = 'CITIZEN'))] AS cit_id,
        -- Fecha de creacion (escalonada durante los ultimos 180 dias)
        (CURRENT_TIMESTAMP - ((5000 - s.i) * interval '50 minutes' + ((s.i % 59) * interval '1 minute'))) AS c_at
    FROM generate_series(2, 5000) AS s(i)
)
SELECT
    -- Titulo segun categoria
    CASE (r.i % 8)
        WHEN 0 THEN (ARRAY[
            'Farola apagada o sin suministro electrico',
            'Punto de luz con parpadeo constante y zumbido molesto',
            'Farola con tulipa rota y lampara colgando con riesgo de caida',
            'Farola encendida de forma continuada a pleno dia',
            'Caja de conexion de alumbrado abierta con cables al descubierto',
            'Baculon de iluminacion inclinado por posible impacto de vehiculo'
        ])[1 + (r.i % 6)]
        WHEN 1 THEN (ARRAY[
            'Baldosas rotas y sueltas con grave riesgo de caida para peatones',
            'Socavon considerable en calzada que golpea bajos de automoviles',
            'Hundimiento del pavimento en el paso de peatones principal',
            'Bordillo de acera partido y desprendido en parada de autobus',
            'Tapa de arqueta desnivelada que vibra con estruendo al paso de coches',
            'Grietas profundas y deformacion en el pavimento de la calzada'
        ])[1 + (r.i % 6)]
        WHEN 2 THEN (ARRAY[
            'Contenedores de residuos organicos completamente desbordados',
            'Acumulacion descontrolada de muebles y enseres en la acera',
            'Papelera publica arrancada o quemada intencionadamente',
            'Mancha persistente de liquidos y grasa con fuerte hedor en el suelo',
            'Restos de poda y ramaje seco sin retirar obstaculizando el paso',
            'Suciedad y restos de botellon acumulados en plaza publica'
        ])[1 + (r.i % 6)]
        WHEN 3 THEN (ARRAY[
            'Banco publico con tablones partidos y tornillos expuestos',
            'Marquesina de autobus con cristal de seguridad fracturado',
            'Valla de proteccion peatonal doblada tras accidente',
            'Bolardo metalico arrancado que invade la calzada',
            'Pilona automatica de acceso peatonal bloqueada y averiada',
            'Fuente publica de agua potable goteando y con charco permanente'
        ])[1 + (r.i % 6)]
        WHEN 4 THEN (ARRAY[
            'Rama de gran calibre desgajada con riesgo inminente de caida',
            'Arbol inclinado con ramas que invaden el carril de circulacion',
            'Fuga abundante de agua en la tuberia del sistema de riego',
            'Juegos infantiles y columpios rotos en area de recreo infantil',
            'Alcorque de arbolado vacio y sin cubrir con peligro de tropiezo',
            'Vegetacion y matorral descontrolado que tapan visibilidad de cruce'
        ])[1 + (r.i % 6)]
        WHEN 5 THEN (ARRAY[
            'Semaforo apagado en cruce de alta densidad de trafico',
            'Semaforo con optica roja fundida para vehiculos',
            'Senal vertical de STOP o Ceda el Paso doblada o arrancada',
            'Paso de peatones con pintura reflectante totalmente desgastada',
            'Espejo parabolico de visibilidad roto o desviado en esquina',
            'Senal de direccion prohibida oculta por vegetacion'
        ])[1 + (r.i % 6)]
        WHEN 6 THEN (ARRAY[
            'Imbornal atascado por hojas y lodo provocando balsa de agua',
            'Olores nauseabundos intensos saliendo de rejilla de alcantarillado',
            'Fuga de agua limpia brotando del pavimento en acera',
            'Rejilla de drenaje pluvial suelta o partida al paso de vehiculos',
            'Hundimiento del asfalto alrededor de pozo de saneamiento',
            'Desbordamiento puntual de arqueta pluvial durante lluvias'
        ])[1 + (r.i % 6)]
        ELSE (ARRAY[
            'Pintadas y grafitis en fachada de edificio publico municipal',
            'Pintadas ofensivas sobre panel informativo turistico',
            'Contenedor de recogida selectiva quemado por vandalismo',
            'Mobiliario de parque infantil manchado con sprays',
            'Vandalismo en placa de senalizacion con el nombre de la calle',
            'Pintadas en monumento y elemento de patrimonio protegido'
        ])[1 + (r.i % 6)]
    END AS title,
    -- Descripcion detallada
    'Incidencia reportada en ' || r.addr || ' (' || r.neigh || ', ' || r.dist || '). ' ||
    CASE (r.i % 8)
        WHEN 0 THEN 'Se constata problema con el punto de alumbrado publico. Afecta a la visibilidad nocturna de los vecinos.'
        WHEN 1 THEN 'Deterioro notable en el pavimento con riesgo directo para viandantes o vehiculos que circulan por la via.'
        WHEN 2 THEN 'Problema higienico y de salubridad debido a la concentracion excesiva de residuos urbanos sin recoger.'
        WHEN 3 THEN 'Elemento de mobiliario urbano danado que precisa reparacion o sustitucion inmediata por los servicios tecnicos.'
        WHEN 4 THEN 'Afeccion en el arbolado o instalacion del parque. Se requiere actuacion de la brigada de mantenimiento verde.'
        WHEN 5 THEN 'Fallo en la senalizacion o regulacion vial que compromete la seguridad y fluidez del transito rodado.'
        WHEN 6 THEN 'Incidencia en la red de saneamiento y drenaje de aguas que produce molestias o estancamiento de caudal.'
        ELSE 'Acto de vandalismo con danos esteticos o estructurales sobre bienes de uso comun en la via publica.'
    END AS description,
    r.cat AS category,
    round((36.7200 + (sin(r.i::numeric * 1.618) * 0.028) + (cos(r.i::numeric * 0.314) * 0.012))::numeric, 6) AS latitude,
    round((-4.4250 + (cos(r.i::numeric * 1.618) * 0.038) + (sin(r.i::numeric * 0.457) * 0.015))::numeric, 6) AS longitude,
    round((5.0 + abs(sin(r.i::numeric)) * 15.0)::numeric, 1) AS accuracy_meters,
    r.addr AS address,
    r.dist AS district,
    r.neigh AS neighborhood,
    r.st AS status,
    r.prio AS priority,
    CASE r.prio
        WHEN 'CRITICAL' THEN (ARRAY[
            'Peligro inminente para la integridad fisica de los viandantes en zona concurrida',
            'Afecta gravemente a la circulacion y seguridad en arteria principal de la ciudad',
            'Riesgo electrico o estructural critico con posibilidad de accidente inmediato',
            'Ubicacion sensible en acceso directo a centro educativo o centro de salud'
        ])[1 + (r.i % 4)]
        WHEN 'HIGH' THEN (ARRAY[
            'Riesgo notable de tropiezos o accidentes menores en acera transitada',
            'Obstaculo persistente que bloquea parcialmente el paso o carril',
            'Molestia vecinal continuada que genera quejas reiteradas en el barrio',
            'Deterioro progresivo que podria derivar en averia mayor si no se repara pronto'
        ])[1 + (r.i % 4)]
        ELSE NULL
    END AS priority_reason,
    r.cit_id AS citizen_id,
    r.c_at AS created_at,
    CASE
        WHEN r.st = 'REPORTED' THEN r.c_at
        ELSE LEAST(CURRENT_TIMESTAMP, r.c_at + (((r.i % 48) + 1) * interval '1 hour'))
    END AS updated_at
FROM raw_data r;

-- 6.5. Asignaciones tecnicas para incidencias en tramite o cerradas
INSERT INTO "assignments" ("incident_id", "department_id", "technician_id", "assigned_by_id", "is_active", "assigned_at")
SELECT
    i.id,
    dept.id,
    tech.id,
    op.id,
    (i.status IN ('ASSIGNED', 'IN_PROGRESS')),
    i.created_at + interval '2 hours'
FROM incidents i
JOIN departments dept ON dept.id = (
    CASE i.category
        WHEN 'Alumbrado'                   THEN 1
        WHEN 'Pavimento y Aceras'          THEN 1
        WHEN 'Mobiliario Urbano'           THEN 1
        WHEN 'Limpieza y Basuras'          THEN 2
        WHEN 'Pintadas y Vandalismo'       THEN 2
        WHEN 'Trafico y Senalizacion'      THEN 3
        WHEN 'Parques y Jardines'          THEN 4
        WHEN 'Saneamiento y Alcantarillado' THEN 6
        ELSE 1
    END
)
LEFT JOIN LATERAL (
    SELECT id FROM users u WHERE u.role = 'TECHNICIAN' AND u.department_id = dept.id ORDER BY u.id LIMIT 1
) tech ON true
LEFT JOIN LATERAL (
    SELECT id FROM users u WHERE u.role = 'OPERATOR' ORDER BY u.id LIMIT 1
) op ON true
WHERE i.status IN ('ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED')
  AND i.id > 1;

-- 6.6. Historial de transiciones de estado
INSERT INTO "status_changes" ("incident_id", "from_status", "to_status", "changed_by_user_id", "reason", "created_at")
SELECT
    i.id,
    'REPORTED',
    i.status,
    2, -- Operador
    'Cambio de estado automatico por tramitacion y flujo de trabajo',
    i.created_at + interval '1 hour'
FROM incidents i
WHERE i.status <> 'REPORTED' AND i.id > 1;

-- 6.7. Comentarios tecnicos y vecinales
INSERT INTO "incident_comments" ("incident_id", "author_id", "comment", "is_internal", "created_at")
SELECT
    i.id,
    CASE WHEN (i.id % 2 = 0) THEN 2 ELSE i.citizen_id END,
    CASE WHEN (i.id % 2 = 0)
        THEN 'Operador/Tecnico: Se ha registrado la incidencia y se ha notificado a la brigada de guardia.'
        ELSE 'Ciudadano: Rogamos maxima celeridad en la solucion de este problema en nuestro barrio.'
    END,
    (i.id % 2 = 0),
    i.created_at + interval '3 hours'
FROM incidents i
WHERE (i.id % 3 = 0) AND i.id > 1;

-- 6.8. Adjuntos de prueba simulados
INSERT INTO "attachments" ("incident_id", "file_name", "file_url", "mime_type", "file_size_bytes", "uploaded_at")
SELECT
    i.id,
    'evidencia_' || i.id || '.jpg',
    'https://storage.urbanpulse.es/incidents/img_' || i.id || '.jpg',
    'image/jpeg',
    (102400 + ((i.id * 7919) % 1500000)),
    i.created_at + interval '5 minutes'
FROM incidents i
WHERE (i.id % 5 = 0) AND i.id > 1;

-- 6.9. Asociaciones entre incidencias y activos urbanos
INSERT INTO "incident_asset_associations" ("incident_id", "asset_id", "distance_meters", "confidence_score", "is_confirmed")
SELECT
    i.id,
    ((i.id % 60) + 1),
    round((3.0 + ((i.id % 45)::numeric * 0.7))::numeric, 2),
    round((0.75 + ((i.id % 25)::numeric * 0.01))::numeric, 2),
    (i.id % 2 = 0)
FROM incidents i
WHERE (i.id % 4 = 0) AND i.id > 1;

-- ---------------------------------------------------------------------------
-- 7. SINCRONIZACION DE SECUENCIAS
-- ---------------------------------------------------------------------------
SELECT setval('departments_id_seq', (SELECT COALESCE(MAX(id), 1) FROM "departments"));
SELECT setval('users_id_seq', (SELECT COALESCE(MAX(id), 1) FROM "users"));
SELECT setval('incidents_id_seq', (SELECT COALESCE(MAX(id), 1) FROM "incidents"));
SELECT setval('attachments_id_seq', (SELECT COALESCE(MAX(id), 1) FROM "attachments"));
SELECT setval('status_changes_id_seq', (SELECT COALESCE(MAX(id), 1) FROM "status_changes"));
SELECT setval('assignments_id_seq', (SELECT COALESCE(MAX(id), 1) FROM "assignments"));
SELECT setval('incident_comments_id_seq', (SELECT COALESCE(MAX(id), 1) FROM "incident_comments"));
SELECT setval('urban_assets_id_seq', (SELECT COALESCE(MAX(id), 1) FROM "urban_assets"));
