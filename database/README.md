# Inicialización de Base de Datos — UrbanPulse


Este directorio contiene la configuración y los scripts necesarios para desplegar la base de datos PostgreSQL del proyecto **UrbanPulse** mediante contenedores con Docker Compose.


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
│   │   └── diagramUrbanPulse.sql   # ENUMs, tablas, ForeignKeys, índices y triggers
│   └── README.md                   
└── docker-compose.yml              # Orquestación del servicio PostgreSQL
```

> **Nota:** Todo fichero `.sql` dentro de `database/init/` se ejecuta automáticamente (por orden alfabético) la **primera vez** que se crea el contenedor. Si la base de datos ya existe en el volumen, los scripts no se re-ejecutan.

---

## Uso

### Levantar la base de datos

Desde la **raíz** del proyecto:

```bash
docker compose up -d # levantar el contendor de Postgres
```

Esto arranca el contenedor `postgres_db` y ejecuta `diagramUrbanPulse.sql` si es la primera vez.

### Verificar que está lista

```bash
docker compose ps           # Comprobar estado del contenedor (debe ser "healthy")
docker exec -it postgres_db psql -U admin -d urban_pulse_db -c '\dt'
```

El segundo comando lista todas las tablas creadas.

### Parar la base de datos

```bash
docker compose down          # Para el contenedor, conserva los datos
```

### Resetear (borrar datos y recrear)

```bash
docker compose down -v       # Elimina el volumen postgres_data
docker compose up -d         # Recrea todo desde cero (incluyendo el volumen)
```

---

## Modelo de Datos

El schema define **3 tipos ENUM** y **11 tablas**:

| Tabla | Propósito |
| :--- | :--- |
| `departments` | Catálogo de áreas municipales |
| `users` | Identidad y control de acceso (RBAC con enum `user_role`) |
| `incidents` | Núcleo: reportes ciudadanos geolocalizados |
| `attachments` | Metadatos de archivos adjuntos a incidencias |
| `status_changes` | Auditoría inmutable de transiciones de estado |
| `assignments` | Asignaciones de incidencias a departamentos/técnicos |
| `incident_comments` | Notas y comentarios de seguimiento |
| `urban_assets` | Catálogo de mobiliario urbano (paradas, semáforos…) |
| `incident_asset_associations` | Vínculo incidencia ↔ activo (N:M con distancia y confianza) |
| `external_observations` | Mediciones externas (clima, tráfico) |
| `incident_contexts` | Vínculo incidencia ↔ observación ambiental (N:M) |

Consulta [`BD.md`](../BD.md) en la raíz del proyecto para la documentación detallada del modelo, cardinalidades y trazabilidad con requisitos funcionales.
