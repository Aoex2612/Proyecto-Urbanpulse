# ==============================================================================
# URBANPULSE - MODELO DE DATOS RELACIONAL (POSTGRESQL)
# Documento Técnico de Arquitectura de Datos y Trazabilidad con Requisitos (RF)
# ==============================================================================

1. PRINCIPIOS Y DECISIONES DE DISEÑO
--------------------------------------------------------------------------------
- Entidad Central:
  'incidents' es el núcleo del dominio. Ningún subsistema externo (clima, tráfico,
  activos) forma una jerarquía independiente; todos orbitan y enriquecen a la incidencia.
- Ubicación:
  Se guardan únicamente las columnas 'latitude' y 'longitude' para representar
  la posición geográfica decimal en el mapa, sin añadir extensiones complejas.
- Almacenamiento de Archivos (RF05):
  Los ficheros físicos (imágenes, vídeos) se guardan en el sistema de archivos del
  servidor; la base de datos solo almacena metadatos y rutas de acceso.
- Trazabilidad y Auditoría (RF12, RF18):
  Toda mutación de estado queda registrada de forma inmutable en 'status_changes'.
- Degradación Controlada (RF26):
  Las observaciones externas y asociaciones a activos admiten valores nulos y 
  cero registros asociados sin bloquear la inserción del reporte principal.

2. MAPA DE CARDINALIDADES (MODELO ALREDEDOR DE INCIDENTS)
--------------------------------------------------------------------------------
       [departments] (1) ──┐
                           │ (1:N)
                           ▼
       [users] (1) ──────► [assignments] (N) ◄────┐
          │                                        │
          │ (1:N)                                  │ (1:N)
          ▼                                        │
       [status_changes] (N) ───────────────────┐   │
          │                                    │   │
          │ (N:1)                              ▼   │
       ┌──┴────────────────────────────────────────┴───┐
       │                  incidents                    │
       │              (Entidad Central)                │
       └───┬───────────────┬───────────────────────┬───┘
           │ 1             │ 1                     │ 1
           │ (1:N)         │ (N:M puente)          │ (N:M puente)
           ▼               ▼                       ▼
     [attachments]   [incident_asset_assoc]   [incident_contexts]
                           │ N                     │ N
                           │ (N:1)                 │ (N:1)
                           ▼ 1                     ▼ 1
                     [urban_assets]          [external_observations]

3. ESTRUCTURA DETALLADA DE ENTIDADES Y CAMPOS
--------------------------------------------------------------------------------

3.1. users (Gestión de Identidad y Control de Acceso)
- Propósito: Almacena credenciales y perfiles de los actores del sistema (RF01, RF02).
- Campos:
  * id                  BIGSERIAL PRIMARY KEY
  * email               VARCHAR(150) NOT NULL UNIQUE
  * password_hash       VARCHAR(255) NOT NULL
  * full_name           VARCHAR(150) NOT NULL
  * role                VARCHAR(30) NOT NULL CHECK (role IN ('CITIZEN', 'OPERATOR', 'TECHNICIAN', 'ADMIN', 'ANALYST'))
  * department_id       BIGINT NULL REFERENCES departments(id) ON DELETE SET NULL
  * created_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP

3.2. departments (Áreas Operativas Municipales)
- Propósito: Catálogo de servicios municipales para enrutamiento de avisos (RF11).
- Campos:
  * id                  BIGSERIAL PRIMARY KEY
  * name                VARCHAR(100) NOT NULL UNIQUE
  * description         TEXT NULL

3.3. incidents (Núcleo Operativo del Dominio)
- Propósito: Registro maestro del reporte emitido por la ciudadanía (RF03, RF04, RF06, RF10).
- Campos:
  * id                  BIGSERIAL PRIMARY KEY
  * title               VARCHAR(200) NOT NULL
  * description         TEXT NOT NULL
  * category            VARCHAR(50) NOT NULL
  * latitude            DOUBLE PRECISION NOT NULL (necesario para la latitud)
  * longitude           DOUBLE PRECISION NOT NULL (necesario para la longitud)
  * accuracy_meters     DOUBLE PRECISION NULL
  * address             VARCHAR(255) NULL
  * district            VARCHAR(100) NULL
  * neighborhood        VARCHAR(100) NULL
  * status              VARCHAR(30) NOT NULL DEFAULT 'REPORTED'
                        CHECK (status IN ('REPORTED', 'VALIDATED', 'REJECTED', 'ASSIGNED', 
                                          'IN_PROGRESS', 'RESOLVED', 'REOPENED', 'CLOSED'))
  * priority            VARCHAR(20) NOT NULL DEFAULT 'MEDIUM' 
                        CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
  * priority_reason     TEXT NULL
  * citizen_id          BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT
  * created_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
  * updated_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP

3.4. attachments (Evidencias y Ficheros Adjuntos)
- Propósito: Metadatos de fotografías o ficheros vinculados a la incidencia (RF05, RF13).
- Campos:
  * id                  BIGSERIAL PRIMARY KEY
  * incident_id         BIGINT NOT NULL REFERENCES incidents(id) ON DELETE CASCADE
  * file_name           VARCHAR(255) NOT NULL
  * file_url            VARCHAR(500) NOT NULL
  * mime_type           VARCHAR(100) NOT NULL
  * file_size_bytes     BIGINT NOT NULL
  * uploaded_at         TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP

3.5. status_changes (Auditoría del Ciclo de Vida)
- Propósito: Trazabilidad inmutable de todas las transiciones de estado (RF12, RF18).
- Campos:
  * id                  BIGSERIAL PRIMARY KEY
  * incident_id         BIGINT NOT NULL REFERENCES incidents(id) ON DELETE CASCADE
  * from_status         VARCHAR(30) NOT NULL
  * to_status           VARCHAR(30) NOT NULL
  * changed_by_user_id  BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT
  * reason              TEXT NULL
  * created_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP

3.6. assignments (Asignaciones y Gestión Operativa)
- Propósito: Registro de tareas encomendadas a departamentos o técnicos (RF11).
- Campos:
  * id                  BIGSERIAL PRIMARY KEY
  * incident_id         BIGINT NOT NULL REFERENCES incidents(id) ON DELETE CASCADE
  * department_id       BIGINT NOT NULL REFERENCES departments(id) ON DELETE RESTRICT
  * technician_id       BIGINT NULL REFERENCES users(id) ON DELETE SET NULL
  * assigned_by_id      BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT
  * is_active           BOOLEAN DEFAULT TRUE
  * assigned_at         TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP

3.7. incident_comments (Colaboración Técnica)
- Propósito: Muro de notas internas y comentarios de seguimiento (RF13).
- Campos:
  * id                  BIGSERIAL PRIMARY KEY
  * incident_id         BIGINT NOT NULL REFERENCES incidents(id) ON DELETE CASCADE
  * author_id           BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT
  * comment             TEXT NOT NULL
  * is_internal         BOOLEAN DEFAULT TRUE
  * created_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP

3.8. urban_assets (Catálogo de Mobiliario y Activos Físicos)
- Propósito: Elementos tangibles de la ciudad (paradas EMT, semáforos) (RF22).
- Campos:
  * id                  BIGSERIAL PRIMARY KEY
  * asset_code          VARCHAR(100) NOT NULL UNIQUE
  * type                VARCHAR(50) NOT NULL
  * latitude            DOUBLE PRECISION NOT NULL (necesario para la latitud)
  * longitude           DOUBLE PRECISION NOT NULL (necesario para la longitud)
  * name                VARCHAR(200) NULL

3.9. incident_asset_associations (Asociación Incidencia-Activo)
- Propósito: Vínculo calculado por distancia y confianza (RF22).
- Campos:
  * incident_id         BIGINT NOT NULL REFERENCES incidents(id) ON DELETE CASCADE
  * asset_id            BIGINT NOT NULL REFERENCES urban_assets(id) ON DELETE CASCADE
  * distance_meters     DOUBLE PRECISION NOT NULL
  * confidence_score    DOUBLE PRECISION NOT NULL
  * is_confirmed        BOOLEAN DEFAULT FALSE
  * PRIMARY KEY (incident_id, asset_id)

3.10. external_observations (Observaciones del Entorno)
- Propósito: Mediciones climáticas o de tráfico en momentos y puntos dados (RF23, RF24, RF25).
- Campos:
  * id                  BIGSERIAL PRIMARY KEY
  * source              VARCHAR(50) NOT NULL
  * metric_type         VARCHAR(50) NOT NULL
  * value_num           DOUBLE PRECISION NULL
  * value_text          VARCHAR(100) NULL
  * observed_at         TIMESTAMP WITH TIME ZONE NOT NULL

3.11. incident_contexts (Enriquecimiento Contextual)
- Propósito: Tabla intermedia entre incidencia y observaciones ambientales (RF23, RF24).
- Campos:
  * incident_id         BIGINT NOT NULL REFERENCES incidents(id) ON DELETE CASCADE
  * observation_id      BIGINT NOT NULL REFERENCES external_observations(id) ON DELETE CASCADE
  * PRIMARY KEY (incident_id, observation_id)

4. SCRIPT DDL COMPLETO (POSTGRESQL)
--------------------------------------------------------------------------------

CREATE TABLE departments (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    role VARCHAR(30) NOT NULL CHECK (role IN ('CITIZEN', 'OPERATOR', 'TECHNICIAN', 'ADMIN', 'ANALYST')),
    department_id BIGINT REFERENCES departments(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE incidents (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(50) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    accuracy_meters DOUBLE PRECISION,
    address VARCHAR(255),
    district VARCHAR(100),
    neighborhood VARCHAR(100),
    status VARCHAR(30) NOT NULL DEFAULT 'REPORTED' 
        CHECK (status IN ('REPORTED', 'VALIDATED', 'REJECTED', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'REOPENED', 'CLOSED')),
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM' 
        CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    priority_reason TEXT,
    citizen_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE attachments (
    id BIGSERIAL PRIMARY KEY,
    incident_id BIGINT NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    uploaded_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE status_changes (
    id BIGSERIAL PRIMARY KEY,
    incident_id BIGINT NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    from_status VARCHAR(30) NOT NULL,
    to_status VARCHAR(30) NOT NULL,
    changed_by_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE assignments (
    id BIGSERIAL PRIMARY KEY,
    incident_id BIGINT NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    department_id BIGINT NOT NULL REFERENCES departments(id) ON DELETE RESTRICT,
    technician_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    assigned_by_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    is_active BOOLEAN DEFAULT TRUE,
    assigned_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE incident_comments (
    id BIGSERIAL PRIMARY KEY,
    incident_id BIGINT NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    author_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    comment TEXT NOT NULL,
    is_internal BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE urban_assets (
    id BIGSERIAL PRIMARY KEY,
    asset_code VARCHAR(100) NOT NULL UNIQUE,
    type VARCHAR(50) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    name VARCHAR(200)
);

CREATE TABLE incident_asset_associations (
    incident_id BIGINT NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    asset_id BIGINT NOT NULL REFERENCES urban_assets(id) ON DELETE CASCADE,
    distance_meters DOUBLE PRECISION NOT NULL,
    confidence_score DOUBLE PRECISION NOT NULL,
    is_confirmed BOOLEAN DEFAULT FALSE,
    PRIMARY KEY (incident_id, asset_id)
);

CREATE TABLE external_observations (
    id BIGSERIAL PRIMARY KEY,
    source VARCHAR(50) NOT NULL,
    metric_type VARCHAR(50) NOT NULL,
    value_num DOUBLE PRECISION,
    value_text VARCHAR(100),
    observed_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE incident_contexts (
    incident_id BIGINT NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    observation_id BIGINT NOT NULL REFERENCES external_observations(id) ON DELETE CASCADE,
    PRIMARY KEY (incident_id, observation_id)
);

5. TRAZABILIDAD CON REQUISITOS FUNCIONALES (RF)
--------------------------------------------------------------------------------
| Requisito Funcional | Entidades Implicadas                     | Implementación / Claves                              |
|---------------------|------------------------------------------|------------------------------------------------------|
| RF01 / RF02         | users, departments                       | roles (RBAC), password_hash, department_id           |
| RF03 / RF04         | incidents                                | title, description, category, coords, address        |
| RF05                | attachments                              | file_url en disco, mime_type, file_size              |
| RF06                | incidents, status_changes                | citizen_id, histórico visible                        |
| RF07                | incidents, assignments                   | Índices en status, category, district, department    |
| RF08                | incidents, urban_assets                  | latitude y longitude para renderizado en mapa        |
| RF09 / RF12 / RF18  | status_changes, incidents                | Matriz de estados, reason obligatorio, auditoría     |
| RF10                | incidents                                | priority, priority_reason                            |
| RF11                | assignments, departments, users          | department_id, technician_id, is_active              |
| RF13                | incident_comments, attachments           | is_internal, evidencias añadidas en resolución       |
| RF15 / RF17         | incidents, status_changes                | Agregaciones por district, category, tiempos de SLA  |
| RF16                | incidents                                | Filtro por category, ventana temporal y coords      |
| RF21                | incidents                                | district, neighborhood                               |
| RF22                | urban_assets, incident_asset_assoc       | distance_meters, confidence_score, asset_code         |
| RF23 / RF24         | external_observations, incident_contexts | metric_type, value_num, observed_at                  |
| RF26                | external_observations, incident_contexts | Tablas desacopladas; fallos de API externa no afectan|
