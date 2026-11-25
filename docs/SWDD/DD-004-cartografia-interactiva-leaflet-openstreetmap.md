# DD-004 — Cartografía interactiva con Leaflet y OpenStreetMap

| Metadato | Valor |
|---|---|
| Estado | En revisión |
| Autoría | Equipo UrbanPulse (refactorización documental) |
| Fecha / última revisión | 2026-11-20 |
| Iteración | Refactorización documental: trazabilidad SDD ↔ ADR |
| Revisores | Equipo técnico UrbanPulse (pendiente de revisión) |
| Enlaces | [ADR-04](../ADR/ADR-04-cartografia-interactiva-leaflet-openstreetmap.md), [feature 003](../../features/003-mapa-incidencias-y-auditoria-transversal/spec.md), [QA note](../003-mapa-incidencias-y-auditoria-transversal/qa-note.md), [C4](../architecture.md) |

## 1. Resumen ejecutivo

El mapa es el eje de la experiencia de UrbanPulse: muestra incidencias geolocalizadas, permite fijar coordenadas al reportar y soporta popups con contexto. Se eligió Leaflet 1.9 + React-Leaflet con teselas de OpenStreetMap frente a Google Maps y Mapbox, para evitar costes por consumo de API y claves de facturación. La razón principal es la independencia de proveedor y la integración declarativa con React. El riesgo es la política de uso de `tile.openstreetmap.org` ante tráfico masivo.

## 2. Contexto y problema

- **Situación actual verificable:** `/map` centrado en `[36.7213, -4.4214]` con zoom 13; componentes `IncidentsMap`, `MapPage`, `CreateIncidentModal`; ver [feature 003](../../features/003-mapa-incidencias-y-auditoria-transversal/spec.md) y [ADR-04](../ADR/ADR-04-cartografia-interactiva-leaflet-openstreetmap.md).
- **Problema:** las alternativas comerciales imponían cuotas, claves y dependencia de facturación, innecesarias para una plataforma municipal de código abierto.
- **Motivación:** RF08 del [documento de caso de estudio](../DESCRIPCION.md) y la feature 003, implementada y validada.

## 3. Objetivos, no objetivos y alcance

**Objetivos**
- O1. Visualizar incidencias activas (excluyendo `CLOSED`/`REJECTED`) sobre mapa base abierto.
- O2. Permitir seleccionar coordenadas mediante clic y ver detalle en popup/modal.
- O3. Funcionar en desarrollo y pruebas sin credenciales de proveedor.

**No objetivos**
- N1. Sustituir la fuente de teselas por un proveedor comercial en esta iteración.
- N2. Implementar mapas vectoriales WebGL ni 3D.
- N3. Resolver geocodificación inversa (gobernada por ADR-06).

**Dentro del alcance:** `MapPage`, `IncidentsMap`, integración React-Leaflet, capa de teselas.
**Fuera del alcance:** API de datos abiertos, captura GPS (ADR-07), analítica espacial avanzada.

## 4. Requisitos y drivers arquitectónicos

| ID | Tipo | Necesidad / escenario | Criterio de aceptación y evidencia | Prioridad |
|---|---|---|---|---|
| RF-1 | Funcional | Mapa centrado en Málaga con zoom 13 | [Feature 003, RF](../../features/003-mapa-incidencias-y-auditoria-transversal/spec.md) | Alta |
| RF-2 | Funcional | Marcadores con código, título, estado y prioridad | Popup documentado en feature 003 | Alta |
| RC-1 | Restricción | React 19 + Vite 6, sin API keys propietarias | [tech-stack.md](../../constitution/tech-stack.md), [ADR-04](../ADR/ADR-04-cartografia-interactiva-leaflet-openstreetmap.md) | Alta |
| RQ-1 | Calidad | Carga rápida en conexiones móviles | ADR-04 cita < 40 KB de JS base de Leaflet | Media |

## 5. Arquitectura actual y dependencias

- **C4 Contexto/Contenedores:** [OSM como sistema externo](../architecture.md#1-diagrama-de-contexto-c4-nivel-1), [SPA en C4 L2](../architecture.md#2-diagrama-de-contenedores-c4-nivel-2).
- **Flujo actual:** SPA → API REST para incidencias → SPA → teselas OSM → navegador.
- **Dependencias y restricciones:** Leaflet 1.9.x, React-Leaflet, servidor de teselas `tile.openstreetmap.org`.

## 6. Alternativas consideradas

| Alternativa | Descripción | Ventajas | Inconvenientes / riesgos | Evidencia disponible |
|---|---|---|---|---|
| A | Google Maps JavaScript API | Cobertura comercial madura | Costes, claves y facturación | [ADR-04](../ADR/ADR-04-cartografia-interactiva-leaflet-openstreetmap.md) |
| B | Mapbox GL JS | Renderizado vectorial WebGL | Cuotas y dependencia de proveedor | [ADR-04](../ADR/ADR-04-cartografia-interactiva-leaflet-openstreetmap.md) |
| C | Leaflet + React-Leaflet + OSM (elegida) | Ligero, abierto, sin claves | Teselas raster; política de uso de OSM | [ADR-04, decisión](../ADR/ADR-04-cartografia-interactiva-leaflet-openstreetmap.md) |

**Criterios de comparación:** coste, independencia, integración React, rendimiento, licencias.
**Elección propuesta y justificación:** C, aceptada en ADR-04; cumple RC-1 sin introducir secretos ni costes recurrentes.

## 7. Diseño propuesto

### 7.1. Visión y diagramas C4

- **Contexto (L1):** [sin cambios](../architecture.md#1-diagrama-de-contexto-c4-nivel-1).
- **Contenedores (L2):** el navegador descarga teselas directamente de OSM; el backend solo entrega datos de incidencias.
- **Componentes (L3):** `MapPage`, `IncidentsMap`, `MapDateFilter`; ver [C4 L3](../architecture.md#3-diagramas-de-componentes-c4-nivel-3).
- **Secuencias:** carga de `/map` → `GET /api/incidents` → render de marcadores → clic → popup/modal de detalle.

### 7.2. Interfaces y contratos

`GET /api/incidents`, `GET /api/incidents/{id}` y `POST /api/incidents` con latitud/longitud decimales WGS84. Límites de coordenadas y validación en [docs/api.md](../api.md).

### 7.3. Modelo y gestión de datos

`incidents.latitude`, `incidents.longitude` y `accuracy_meters` se almacenan en PostgreSQL; la capa cartográfica no persiste teselas. Formato decimal con 6 decimales en la captura GPS (feature 003).

### 7.4. Fallos, concurrencia y resiliencia

Si el servidor de teselas falla, el mapa puede quedar sin capa base, pero las incidencias siguen consultables vía API. No hay reintentos configurados en la SPA; se registra como riesgo operativo.

### 7.5. Seguridad

No se envían credenciales al proveedor de teselas. La URL de teselas es pública; no hay datos personales en ella. Las coordenadas de incidencias se tratan como datos de localización, con consentimiento explícito solo en la captura GPS (ADR-07).

### 7.6. Operación y despliegue

Sin infraestructura nueva. Para despliegues intensivos, ADR-04 indica la necesidad de evaluar proveedor de teselas propio o comercial; no hay SLOs numéricos definidos en el repositorio.

## 8. Decisiones y trade-offs

| Decisión propuesta | Beneficio | Coste / consecuencia | ¿Requiere ADR? |
|---|---|---|---|
| Leaflet + OSM | Coste cero, sin claves | Teselas raster y política de uso de OSM | No — ADR-04 |
| Catálogo cerrado de categorías | Clasificación homogénea | Menos flexibilidad de usuario | No — decisión de feature 003 |

### 8.1. Decisiones arquitectónicas derivadas

- [ADR-04: Cartografía interactiva con Leaflet y OpenStreetMap frente a servicios privativos](../ADR/ADR-04-cartografia-interactiva-leaflet-openstreetmap.md) — **aceptado**. Decisión de origen.
- Relacionadas: [ADR-05](../ADR/ADR-05-trazabilidad-inmutable-auditoria-estados.md) (timeline) y [ADR-07](../ADR/ADR-07-geolocalizacion-nativa-navegador.md) (captura GPS).

## 9. Plan de implementación y migración

1. Integrar `MapContainer`, `TileLayer` y `Marker` en `IncidentsMap`.
2. Conectar el listado filtrado de incidencias activas desde la API.
3. Añadir popup/modal de detalle y control de selección de coordenadas.
4. Verificar con Vitest y `npm run lint`; reversión: desactivar la ruta `/map` y usar el listado.

**Dependencias:** ADR-05 para el timeline y ADR-07 para la captura de ubicación. **Impacto en versiones anteriores:** ninguno.

## 10. Estrategia de verificación y evaluación

| Requisito | Prueba/experimento | Entorno y datos | Métrica / umbral | Evidencia esperada |
|---|---|---|---|---|
| RF-1/RF-2 | Tests de `IncidentsMap` y `MapPage` (Vitest) | Local, datos de incidencias mock | 0 fallos | [QA note 003](../003-mapa-incidencias-y-auditoria-transversal/qa-note.md): 7 suites / 68 tests frontend |
| RQ-1 | Carga de teselas en red local | Navegador | Sin error de carga | Revisión manual; no hay umbral numérico documentado |

No hay SLOs numéricos; el comportamiento con proveedor de teselas a gran escala solo podría validarse en un despliegue real.

## 11. Riesgos, hipótesis y cuestiones abiertas

| ID | Tipo | Descripción | Impacto | Acción y responsable |
|---|---|---|---|---|
| R-1 | Riesgo | Política de uso de OSM desaconseja tráfico masivo | Medio | Evaluar proveedor propio/comercial si escala — operaciones |
| Q-1 | Pregunta | La `spec.md` de feature 003 marca HU14.4 «en curso» mientras `tasks.md`/`qa-note.md` la dan por terminada | Bajo | Aclarar estado en el seguimiento de la feature |
| Q-2 | Pregunta | ¿Definir SLO de carga de mapa? | Bajo | Decisión pendiente |

## 12. Revisión y resultado

- **Comentarios de revisión:** pendientes.
- **Resolución:** pendiente de revisión técnica.
- **Decisiones registradas:** [ADR-04](../ADR/ADR-04-cartografia-interactiva-leaflet-openstreetmap.md).
- **Diferencias observadas tras implementar:** el mapa incluye un catálogo cerrado de categorías y un toggle crear/cancelar no detallados en el ADR; se documentan en la feature 003.

## Checklist de aprobación

- [x] Problema y alcance delimitados.
- [x] Drivers y restricciones documentados, sin atribuir al enunciado requisitos inexistentes.
- [x] Al menos dos alternativas comparadas o justificación de por qué no hay alternativas viables.
- [x] Diagramas, datos y contratos coherentes.
- [x] Fallos, seguridad, operación y costes considerados en proporción al cambio.
- [x] Criterios de aceptación verificables y plan de pruebas reproducible.
- [x] Riesgos y cuestiones abiertas explícitos.
- [ ] Revisión técnica realizada; ADR enlazados cuando corresponde.
