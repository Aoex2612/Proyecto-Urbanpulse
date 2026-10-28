# Roadmap — UrbanPulse

_Orden y estado de las features. Es la vista de "qué hay hecho, qué toca ahora y qué viene". Cada entrada apunta a su carpeta en `features/`._

## Hecho ✅

1. **000 · Configuración base de infraestructura y persistencia** — Estructura inicial del repositorio, contenedor Docker Compose de PostgreSQL 16 y esquema relacional con tablas y enumerados en `database/init/diagramUrbanPulse.sql`.
2. **001 · Registro e inicio de sesión de ciudadanos** (`HU01`) — Endpoints de registro y login (`/api/auth/register`, `/api/auth/login`), autenticación con BCrypt y JWT, pantallas frontend de `/login` y `/register`, almacenamiento seguro en cliente, enrutamiento protegido y configuración del interceptor de Axios (`features/001-registro-e-inicio-de-sesion-de-ciudadanos/`).
3. **002 · Autenticación y control de acceso por roles (RBAC)** (`HU02`) — Filtro de autenticación JWT backend (`JwtAuthenticationFilter`), manejadores estructurados de errores 401/403 (`ErrorResponse`), restricción de operaciones de triaje por rol (`OPERATOR` / `ADMIN`), endpoint `PATCH /api/incidents/{id}/status`, componentes frontend `ProtectedRoute` y `RoleGuard`, panel de operador y notificación de acceso denegado (`features/002-autenticacion-y-control-de-acceso-rbac/`).
4. **003 · Mapa interactivo de incidencias con Leaflet, reporte geolocalizado y auditoría transversal** (`HU03`) — Visualización cartográfica con Leaflet de incidencias urbanas, creación ágil de incidencias marcando coordenadas en el mapa y registro inmutable de auditoría de cambios de estado (`status_changes`) con visualización de línea temporal (`features/003-mapa-incidencias-y-auditoria-transversal/`).
5. **004 · Reporte y seguimiento de incidencias con detección territorial** (`HU04`, `HU06`, `HU15`) — Reporte con captura de coordenadas y dirección postal oficial del callejero del Ayuntamiento de Málaga en modo solo lectura, resolución automática de barrio y distrito con datos abiertos y degradación controlada (`null` / `"Sin asignar"`), y panel personal "Mis Incidencias" con listado paginado y filtros (`features/004-reporte-y-seguimiento-de-incidencias/`).

## En curso 🔄

_Ninguna feature en curso actualmente._

## Siguiente 🔜

- **Adjuntos multimedia en incidencias** (`HU05`) — Subida y asociación de fotos/evidencias a las incidencias reportadas.

## Backlog / ideas 💡

- **Adjuntos multimedia en incidencias** — Subida y asociación de fotos/vídeos a las incidencias reportadas.
- **Validación asistida e inferencia de duplicados** — Sugerencia automática de incidencias similares y detección de proximidad espaciotemporal.
- **Asignación departamental y gestión de técnicos** — Panel para operadores para asignar incidencias a departamentos y técnicos.
- **Gestión e inventario de activos urbanos** — Consulta, alta y vinculación de elementos de mobiliario e infraestructura urbana con incidencias.
- **Enriquecimiento contextual externo** — Integración de observaciones ambientales, climáticas o de tráfico asociadas a la zona de la incidencia.

> Cada feature nueva se crea como `features/NNN-nombre-feature/` con `spec.md`, `plan.md` y `tasks.md` antes de tocar código.
