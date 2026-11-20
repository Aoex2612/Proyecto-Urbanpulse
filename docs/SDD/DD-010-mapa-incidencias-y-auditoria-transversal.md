# DD-010 — Mapa de incidencias y auditoría transversal

| Metadato | Valor |
|---|---|
| Estado | En revisión |
| Autoría | Equipo UrbanPulse (refactorización documental) |
| Fecha / última revisión | 2026-11-20 |
| Iteración | Refactorización documental: trazabilidad SDD ↔ ADR |
| Revisores | Equipo técnico UrbanPulse (pendiente de revisión) |
| Enlaces | [Feature 003](../../features/003-mapa-incidencias-y-auditoria-transversal/spec.md), [plan 003](../../features/003-mapa-incidencias-y-auditoria-transversal/plan.md), [QA note](../003-mapa-incidencias-y-auditoria-transversal/qa-note.md), [ADR-04](../ADR/ADR-04-cartografia-interactiva-leaflet-openstreetmap.md), [ADR-05](../ADR/ADR-05-trazabilidad-inmutable-auditoria-estados.md), [ADR-07](../ADR/ADR-07-geolocalizacion-nativa-navegador.md) |

## 1. Resumen ejecutivo

La feature 003 integra el mapa interactivo, el alta de incidencias, el historial inmutable de estados y la captura GPS opcional (HU14.4). Reúne tres decisiones ya aceptadas: Leaflet/OSM (ADR-04), tabla `status_changes` (ADR-05) y API W3C Geolocation (ADR-07). El diseño mantiene el mapa centrado en Málaga, excluye estados terminados y restringe el timeline/historial a ADMIN. El principal riesgo documental es la inconsistencia entre la `spec.md` (HU14.4 «en curso») y `tasks.md`/`qa-note.md` (completada).

## 2. Contexto y problema

- **Situación actual verificable:** ruta `/map` centrada en `[36.7213, -4.4214]`, zoom 13; endpoints `GET /api/incidents`, `GET /api/incidents/{id}`, `POST /api/incidents`, `PATCH /api/incidents/{id}/status`, `GET /api/incidents/{id}/history`; ver [spec 003](../../features/003-mapa-incidencias-y-auditoria-transversal/spec.md).
- **Problema:** sin una visión conjunta, mapa, auditoría y geolocalización parecían decisiones aisladas, cuando comparten el mismo flujo de alta y seguimiento.
- **Motivación:** RF06, RF08 y RF12 del [caso de estudio](../DESCRIPCION.md); feature validada con 272 tests backend.

## 3. Objetivos, no objetivos y alcance

**Objetivos**
- O1. Visualizar incidencias activas y filtrarlas por estado/fecha.
- O2. Registrar transiciones de estado de forma inmutable y mostrar el timeline.
- O3. Permitir fijar coordenadas por clic o por GPS con consentimiento.

**No objetivos**
- N1. Resolver dirección/barrio/distrito (DD-006).
- N2. Mostrar histórico completo a cualquier rol (solo ADMIN en `/history`).
- N3. Cambiar las decisiones de cartografía, auditoría o geolocalización ya aceptadas.

**Dentro del alcance:** `MapPage`, `IncidentsMap`, `CreateIncidentModal`, `StatusTimeline`, endpoints de incidencias e historial, `geolocationUtils.js`.
**Fuera del alcance:** adjuntos (DD-012), notificaciones y analítica (backlog).

## 4. Requisitos y drivers arquitectónicos

| ID | Tipo | Necesidad / escenario | Criterio de aceptación y evidencia | Prioridad |
|---|---|---|---|---|
| RF-1 | Funcional | Mapa con marcadores de incidencias activas | [spec 003](../../features/003-mapa-incidencias-y-auditoria-transversal/spec.md) | Alta |
| RF-2 | Funcional | `PATCH` registra una transición en `status_changes` | [ADR-05](../ADR/ADR-05-trazabilidad-inmutable-auditoria-estados.md) | Alta |
| RF-3 | Funcional | `/history` solo ADMIN: 200/403/404 | Spec 003 | Alta |
| RF-4 | Funcional | GPS opcional con 6 decimales y conservación ante error | [ADR-07](../ADR/ADR-07-geolocalizacion-nativa-navegador.md) | Alta |
| RC-1 | Restricción | Leaflet + OSM sin claves de API | [ADR-04](../ADR/ADR-04-cartografia-interactiva-leaflet-openstreetmap.md) | Alta |
| RQ-1 | Calidad | Cobertura ≥ 70% instrucciones / ≥ 60% ramas (umbral citado en documentación de QA) | [QA note 004](../004-reporte-y-seguimiento-de-incidencias/qa-note.md) | Media |

## 5. Arquitectura actual y dependencias

- **C4 Contexto/Contenedores:** [OSM como sistema externo](../architecture.md#1-diagrama-de-contexto-c4-nivel-1), [C4 L2](../architecture.md#2-diagrama-de-contenedores-c4-nivel-2).
- **Flujo actual:** SPA (mapa/modal) → API REST → PostgreSQL; el navegador carga teselas y usa geolocalización nativa.
- **Dependencias y restricciones:** Leaflet 1.9, React-Leaflet, Spring Data JPA, RBAC, PostgreSQL/H2.

## 6. Alternativas consideradas

| Alternativa | Descripción | Ventajas | Inconvenientes / riesgos | Evidencia disponible |
|---|---|---|---|---|
| A | Google Maps / Mapbox | Madurez comercial | Coste y claves | [ADR-04](../ADR/ADR-04-cartografia-interactiva-leaflet-openstreetmap.md) |
| B | Leaflet + OSM (elegida) | Abierto y sin coste | Teselas raster | [ADR-04, decisión](../ADR/ADR-04-cartografia-interactiva-leaflet-openstreetmap.md) |
| C | Envers/JSONB para historial | Menos código | No inmutable/consultable | [ADR-05](../ADR/ADR-05-trazabilidad-inmutable-auditoria-estados.md) |
| D | Geolocalización por IP / plugin Leaflet | Sin permiso nativo | Baja precisión / dependencia | [ADR-07](../ADR/ADR-07-geolocalizacion-nativa-navegador.md) |

**Criterios de comparación:** coste, precisión, inmutabilidad, dependencias, coherencia con ADRs.
**Elección propuesta y justificación:** las alternativas B (mapa), tabla dedicada (auditoría) y API nativa (GPS), ya aceptadas en sus ADR y materializadas en esta feature.

## 7. Diseño propuesto

### 7.1. Visión y diagramas C4

- **Contexto (L1):** [C4 L1](../architecture.md#1-diagrama-de-contexto-c4-nivel-1).
- **Contenedores (L2):** [C4 L2](../architecture.md#2-diagrama-de-contenedores-c4-nivel-2).
- **Componentes (L3):** `IncidentsMap`, `CreateIncidentModal`, `StatusTimeline`, `IncidentService`; ver [C4 L3](../architecture.md#3-diagramas-de-componentes-c4-nivel-3).
- **Secuencias:** alta con coordenadas → `REPORTED`/`MEDIUM` → PATCH de estado → inserción en historial → timeline.

### 7.2. Interfaces y contratos

Endpoints listados en el apartado 2; estados excluidos `CLOSED`/`REJECTED`; categoría cerrada de 5 opciones; `reason` mínimo 10 caracteres si `REJECTED`. Detalle en [docs/api.md](../api.md).

### 7.3. Modelo y gestión de datos

`incidents` con coordenadas, precisión, dirección y estado; `status_changes` con actor, estado anterior/nuevo, motivo y fecha (ADR-05). Los enums se persisten como VARCHAR + CHECK (ADR-01).

### 7.4. Fallos, concurrencia y resiliencia

Ante error de geolocalización se conservan coordenadas previas (ADR-07). El cambio de estado y su auditoría comparten transacción (ADR-05). No hay reintentos automáticos en la SPA.

### 7.5. Seguridad

RBAC: timeline solo ADMIN, `/history` restringido; token Bearer (ADR-02); consentimiento GPS bajo demanda (ADR-07). Sin datos personales en la URL de teselas.

### 7.6. Operación y despliegue

Misma topología Compose. No hay SLOs numéricos; se monitoriza con `/actuator/health` y logs de Docker según [deployment.md](../deployment.md).

## 8. Decisiones y trade-offs

| Decisión propuesta | Beneficio | Coste / consecuencia | ¿Requiere ADR? |
|---|---|---|---|
| Leaflet + OSM | Coste cero | Política de uso de teselas | No — ADR-04 |
| `status_changes` inmutable | Trazabilidad forense | Crecimiento lineal | No — ADR-05 |
| API W3C Geolocation | Precisión y 0 dependencias | HTTPS obligatorio | No — ADR-07 |
| Catálogo cerrado de categorías | Clasificación homogénea | Menos flexibilidad | No — spec 003 |

### 8.1. Decisiones arquitectónicas derivadas

- [ADR-04: Leaflet y OpenStreetMap](../ADR/ADR-04-cartografia-interactiva-leaflet-openstreetmap.md) — **aceptado**.
- [ADR-05: Trazabilidad inmutable](../ADR/ADR-05-trazabilidad-inmutable-auditoria-estados.md) — **aceptado**.
- [ADR-07: Geolocalización nativa](../ADR/ADR-07-geolocalizacion-nativa-navegador.md) — **aceptado** (renombrado desde ADR-06).

## 9. Plan de implementación y migración

1. Renderizar mapa y listado filtrado; conectar popup/modal de detalle.
2. Implementar alta con estado inicial y prioridad por defecto.
3. Añadir cambio de estado con inserción de auditoría y timeline.
4. Integrar captura GPS y verificar con Vitest/JaCoCo; reversión: desactivar `/map` y el botón GPS.

**Dependencias:** DD-008/DD-009 (identidad y roles), ADR-01. **Impacto en versiones anteriores:** ninguno.

## 10. Estrategia de verificación y evaluación

| Requisito | Prueba/experimento | Entorno y datos | Métrica / umbral | Evidencia esperada |
|---|---|---|---|---|
| RF-1/RF-2 | Tests backend de incidencias y cambios de estado | Local, H2 | 0 fallos | [QA note 003](../003-mapa-incidencias-y-auditoria-transversal/qa-note.md): 272 tests backend (1 omitido por PostgreSQL) |
| RF-3 | Tests de controlador por rol | Local | 200/403/404 | QA note 003 |
| RF-4 | Tests de `geolocationUtils.js` con mocks | Vitest | 0 fallos | QA note 003: 7 suites / 68 tests frontend |
| RQ-1 | JaCoCo / Vitest coverage | CI | 70% instr. / 60% ramas (referencia) | QA note 003: 95,12% instr. / 75,85% ramas backend |

La precisión GPS y la carga de teselas reales solo pueden validarse en un entorno con dispositivo/red; en CI se simulan.

## 11. Riesgos, hipótesis y cuestiones abiertas

| ID | Tipo | Descripción | Impacto | Acción y responsable |
|---|---|---|---|---|
| Q-1 | Pregunta | `spec.md` marca HU14.4 «en curso» pero `tasks.md`/`qa-note.md` la dan por terminada | Medio | Aclarar y actualizar estado de la feature |
| R-1 | Riesgo | Política de uso de teselas OSM a gran escala | Medio | Evaluar proveedor alternativo |
| R-2 | Riesgo | Crecimiento de `status_changes` | Bajo a corto plazo | Plan de archivado futuro |

## 12. Revisión y resultado

- **Comentarios de revisión:** pendientes.
- **Resolución:** pendiente de revisión técnica.
- **Decisiones registradas:** [ADR-04](../ADR/ADR-04-cartografia-interactiva-leaflet-openstreetmap.md), [ADR-05](../ADR/ADR-05-trazabilidad-inmutable-auditoria-estados.md), [ADR-07](../ADR/ADR-07-geolocalizacion-nativa-navegador.md).
- **Diferencias observadas tras implementar:** el ADR-05 no detallaba la restricción del timeline a ADMIN; se documenta en la feature 003 y aquí.

## Checklist de aprobación

- [x] Problema y alcance delimitados.
- [x] Drivers y restricciones documentados, sin atribuir al enunciado requisitos inexistentes.
- [x] Al menos dos alternativas consideradas a partir de los ADR.
- [x] Diagramas, datos y contratos coherentes.
- [x] Fallos, seguridad, operación y costes considerados en proporción al cambio.
- [x] Criterios de aceptación verificables y plan de pruebas reproducible.
- [x] Riesgos y cuestiones abiertas explícitos.
- [ ] Revisión técnica realizada; ADR enlazados cuando corresponde.
