# Roadmap — UrbanPulse

_Orden y estado de las features. Es la vista de "qué hay hecho, qué toca ahora y qué viene". Cada entrada apunta a su carpeta en `features/`._

## Hecho ✅

1. **000 · Configuración base de infraestructura y persistencia** — Estructura inicial del repositorio, contenedor Docker Compose de PostgreSQL 16 y esquema relacional con tablas y enumerados en `database/init/diagramUrbanPulse.sql`.
2. **001 · Registro e inicio de sesión de ciudadanos** (`HU01`) — Endpoints de registro y login (`/api/auth/register`, `/api/auth/login`), autenticación con BCrypt y JWT, pantallas frontend de `/login` y `/register`, almacenamiento seguro en cliente, enrutamiento protegido y configuración del interceptor de Axios (`features/001-registro-e-inicio-de-sesion-de-ciudadanos/`).

## Siguiente 🔜

- **002 · Reporte de incidencias urbanas geolocalizadas** (`HU02`) — Creación de incidencias con latitud, longitud, dirección y categoría por parte de ciudadanos (`features/002-reporte-de-incidencias-urbanas-geolocalizadas/`).

## Backlog / ideas 💡

- **Adjuntos multimedia en incidencias** — Subida y asociación de fotos/vídeos a las incidencias reportadas.
- **Auditoría y transición de estados** — Registro inmutable de cambios de estado (`status_changes`) según flujo de validación y resolución.
- **Asignación departamental y gestión de técnicos** — Panel para operadores para asignar incidencias a departamentos y técnicos.
- **Gestión e inventario de activos urbanos** — Consulta, alta y vinculación de elementos de mobiliario e infraestructura urbana con incidencias.
- **Enriquecimiento contextual externo** — Integración de observaciones ambientales, climáticas o de tráfico asociadas a la zona de la incidencia.

> Cada feature nueva se crea como `features/NNN-nombre-feature/` con `spec.md`, `plan.md` y `tasks.md` antes de tocar código.
