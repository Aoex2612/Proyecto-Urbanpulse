# Inicialización de Base de Datos — UrbanPulse

Este directorio contiene la configuración y los scripts necesarios para desplegar y poblar la base de datos PostgreSQL del proyecto **UrbanPulse** mediante contenedores con Docker Compose.

---

## Requisitos Previos

* [Docker Desktop](https://www.docker.com/products/docker-desktop/) instalado y en ejecución.
* Docker Compose v2+ (`docker compose version`).

---

## Parámetros de Conexión

| Parámetro | Valor local |
| :--- | :--- |
| **Motor** | PostgreSQL 16 (Alpine) |
| **Host** | `localhost` |
| **Puerto** | `5432` |
| **Base de Datos** | `urban_pulse_db` |
| **Usuario** | `admin` |
| **Contraseña** | `database` |
| **URL JDBC** | `jdbc:postgresql://localhost:5432/urban_pulse_db` |

---

## Estructura del Directorio

```text
Proyecto-Urbanpulse/
├── database/
│   ├── init/
│   │   └── diagramUrbanPulse.sql   # Tablas, restricciones CHECK, Foreign Keys, índices, triggers y seed
│   └── README.md                   # Esta documentación
└── docker-compose.yml              # Orquestación del servicio PostgreSQL
```

> **Nota:** Todo fichero `.sql` dentro de `database/init/` se ejecuta automáticamente la **primera vez** que se inicializa el contenedor y se crea el volumen persistente. Si la base de datos ya existe en el volumen `postgres_data`, el script no se re-ejecuta a menos que se resetee el volumen.

---

## Uso

### Levantar la base de datos

Desde la **raíz** del proyecto:

```bash
docker compose up -d
```

Esto arranca el contenedor `db_postgres` y ejecuta `diagramUrbanPulse.sql`.

### Verificar que está lista

```bash
docker compose ps                                                    # Comprobar estado (debe ser "healthy")
docker exec -it db_postgres psql -U admin -d urban_pulse_db -c '\dt' # Listar tablas
```

### Parar la base de datos

```bash
docker compose down          # Para el contenedor, conserva los datos persistidos
```

### Resetear (borrar datos y recrear)

```bash
docker compose down -v       # Elimina el volumen postgres_data
docker compose up -d         # Recrea todo desde cero (re-ejecutando diagramUrbanPulse.sql)
```

---

## Modelo de Datos

El esquema define **11 tablas** adaptadas a Hibernate mediante restricciones `CHECK` en columnas `VARCHAR`:

| Tabla | Propósito |
| :--- | :--- |
| `departments` | Catálogo de áreas municipales |
| `users` | Identidad y control de acceso (RBAC: `CITIZEN`, `OPERATOR`, `TECHNICIAN`, `ADMIN`, `ANALYST`) |
| `incidents` | Núcleo: reportes ciudadanos geolocalizados con estados y prioridades |
| `attachments` | Metadatos de archivos adjuntos a incidencias |
| `status_changes` | Auditoría inmutable de transiciones de estado |
| `assignments` | Asignaciones de incidencias a departamentos/técnicos |
| `incident_comments` | Notas y comentarios de seguimiento interno o público |
| `urban_assets` | Catálogo de mobiliario urbano (paradas, semáforos, contenedores...) |
| `incident_asset_associations` | Vínculo incidencia ↔ activo (N:M con distancia y confianza) |
| `external_observations` | Mediciones externas (clima, tráfico) |
| `incident_contexts` | Vínculo incidencia ↔ observación ambiental (N:M) |

---

## Datos Iniciales (Seed Data) para HU01 y HU02

El script inicializa automáticamente datos de prueba con contraseñas hasheadas en **BCrypt**:

| Email | Contraseña plana | Rol | Propósito |
| :--- | :--- | :--- | :--- |
| `admin@urbanpulse.es` | `Admin1234` | `ADMIN` | Acceso a operaciones administrativas y triaje |
| `operator@urbanpulse.es` | `Operator1234` | `OPERATOR` | Acceso a triaje (`PATCH /api/incidents/{id}/status`) y panel operador |
| `citizen@urbanpulse.es` | `Citizen1234` | `CITIZEN` | Usuario estándar con 1 incidencia inicial asociada |

Consulta [`BD.md`](../BD.md) en la raíz del proyecto para la documentación detallada del modelo relacional.
