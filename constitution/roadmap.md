# Roadmap — UrbanPulse

_Orden y estado de las features. Es la vista de "qué hay hecho, qué toca ahora y qué viene". Cada entrada apunta a su carpeta en `features/`._

## Hecho ✅

1. **000 · Configuración base de infraestructura y persistencia** — Estructura inicial del repositorio, contenedor Docker Compose de PostgreSQL 16 y esquema relacional con tablas y enumerados en `database/init/diagramUrbanPulse.sql`.
2. **001 · Registro e inicio de sesión de ciudadanos** (`HU01`) — Endpoints de registro y login (`/api/auth/register`, `/api/auth/login`), autenticación con BCrypt y JWT, pantallas frontend de `/login` y `/register`, almacenamiento seguro en cliente, enrutamiento protegido y configuración del interceptor de Axios (`features/001-registro-e-inicio-de-sesion-de-ciudadanos/`).
3. **002 · Autenticación y control de acceso por roles (RBAC)** (`HU02`) — Filtro de autenticación JWT backend (`JwtAuthenticationFilter`), manejadores estructurados de errores 401/403 (`ErrorResponse`), restricción de operaciones de triaje por rol (`OPERATOR` / `ADMIN`), endpoint `PATCH /api/incidents/{id}/status`, componentes frontend `ProtectedRoute` y `RoleGuard`, panel de operador y notificación de acceso denegado (`features/002-autenticacion-y-control-de-acceso-rbac/`).
4. **003 · Mapa interactivo de incidencias con Leaflet, reporte geolocalizado y auditoría transversal** (`HU03`) — Visualización cartográfica con Leaflet de incidencias urbanas, creación ágil de incidencias marcando coordenadas en el mapa y registro inmutable de auditoría de cambios de estado (`status_changes`) con visualización de línea temporal (`features/003-mapa-incidencias-y-auditoria-transversal/`).
5. **006 · Adjuntos multimedia en incidencias** (`HU05`) — Subida y asociación de fotos/evidencias a las incidencias reportadas con previsualización reactiva, almacenamiento en disco y galería en detalle (`features/006-adjuntos-multimedia-en-incidencias/`).
6. **004 · Reporte y seguimiento de incidencias con detección territorial** (`HU04`, `HU06`, `HU15`) — Implementado y validado exitosamente.
7. **005 · Gestión y triaje operativo de incidencias** (`HU07`–`HU12`) — Validación y rechazo con motivo auditable, asignación multidepartamental y técnica (`assignments`), máquina de estados del ciclo de vida, muro de comentarios (`incident_comments`) y priorización asistida (`features/005-gestion-y-triaje-operativo-de-incidencias/`).

## En mejora 🛠️



## En curso 🔄

- **009 · Analítica urbana y reportes** (`HU22`–`HU25`) — Métricas operativas (MTTT, MTTR, evaluación de SLAs con umbrales 3d/7d/15d/30d), análisis territorial y temporal con gráficos visuales, exportación de informes en PDF y recomendación asistida con grado de confianza (`features/009-analitica-urbana-y-reportes/`).

## Siguiente 🔜

- **Validación asistida e inferencia de duplicados** — Sugerencia automática de incidencias similares y detección de proximidad espaciotemporal.

## Backlog / ideas 💡

- **Validación asistida e inferencia de duplicados** — Sugerencia automática de incidencias similares y detección de proximidad espaciotemporal.
- **Asignación departamental y gestión de técnicos** — Panel para operadores para asignar incidencias a departamentos y técnicos.
- **Gestión e inventario de activos urbanos** — Consulta, alta y vinculación de elementos de mobiliario e infraestructura urbana con incidencias.
- **Enriquecimiento contextual externo** — Integración de observaciones ambientales, climáticas o de tráfico asociadas a la zona de la incidencia.

> Cada feature nueva se crea como `features/NNN-nombre-feature/` con `spec.md`, `plan.md` y `tasks.md` antes de tocar código.
