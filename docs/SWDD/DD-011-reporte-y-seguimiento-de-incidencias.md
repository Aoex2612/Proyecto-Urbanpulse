# DD-011 — Reporte y seguimiento de incidencias con detección territorial

| Metadato | Valor |
|---|---|
| Estado | En revisión |
| Autoría | Equipo UrbanPulse (refactorización documental) |
| Fecha / última revisión | 2026-11-20 |
| Iteración | Refactorización documental: trazabilidad SDD ↔ ADR |
| Revisores | Equipo técnico UrbanPulse (pendiente de revisión) |
| Enlaces | [Feature 004](../../features/004-reporte-y-seguimiento-de-incidencias/spec.md), [plan 004](../../features/004-reporte-y-seguimiento-de-incidencias/plan.md), [QA note](../004-reporte-y-seguimiento-de-incidencias/qa-note.md), [ADR-06](../ADR/ADR-06-integracion-datos-abiertos-malaga-degradacion-controlada.md) |

## 1. Resumen ejecutivo

La feature 004 permite reportar una incidencia con coordenadas y enriquecerla con dirección, barrio y distrito, además de consultar el listado paginado «Mis incidencias» y el detalle. El diseño materializa ADR-06 (proxy backend con degradación controlada), ADR-03 (DTOs/mappers) y ADR-01 (enums como VARCHAR). El riesgo principal es la dependencia de la fuente externa y el uso de una factoría SSL permisiva, documentado como incidencia corregida en `repare.md`.

## 2. Contexto y problema

- **Situación actual verificable:** `GET /api/incidents/geocode/reverse`, `GET /api/incidents/my-incidents?page&size&status`, `GET /api/incidents/{id}`; estado inicial `REPORTED`, prioridad `MEDIUM`, campos no resueltos como «Sin asignar». Ver [spec 004](../../features/004-reporte-y-seguimiento-de-incidencias/spec.md).
- **Problema:** sin resolución territorial automática, el reporte perdía contexto municipal; sin degradación controlada, un fallo externo bloqueaba el alta.
- **Motivación:** RF03/RF04/RF06/RF21/RF26 del [caso de estudio](../DESCRIPCION.md); 18/18 RF validados en QA.

## 3. Objetivos, no objetivos y alcance

**Objetivos**
- O1. Alta de incidencias con enriquecimiento territorial y fallback.
- O2. Listado paginado y detalle con fechas y estado.
- O3. Mantener el alta operativa aunque falle la fuente externa.

**No objetivos**
- N1. Persistir catálogos externos ni usar PostGIS (ADR-06).
- N2. Modificar la cartografía base (ADR-04) ni el historial (ADR-05).
- N3. Implementar notificaciones o duplicados (backlog).

**Dentro del alcance:** geocodificación inversa, `ReverseGeocodeResponse`, `PageResponse<T>`, repositorio `findByCitizenId[AndStatus]`, formulario y listado frontend.
**Fuera del alcance:** adjuntos (DD-012), triaje (DD-010/feature 005 en otras ramas).

## 4. Requisitos y drivers arquitectónicos

| ID | Tipo | Necesidad / escenario | Criterio de aceptación y evidencia | Prioridad |
|---|---|---|---|---|
| RF-1 | Funcional | Captura en mapa y alta con estado/prioridad por defecto | [spec 004](../../features/004-reporte-y-seguimiento-de-incidencias/spec.md) | Alta |
| RF-2 | Funcional | Dirección/barrio/distrito o «Sin asignar» | Spec 004 | Alta |
| RF-3 | Funcional | Listado paginado, orden DESC, filtro por 8 estados; 200 con `content: []` | Spec 004 | Alta |
| RQ-1 | Calidad | Timeout externo 3 s | [ADR-06](../ADR/ADR-06-integracion-datos-abiertos-malaga-degradacion-controlada.md) | Alta |
| RQ-2 | Calidad | Consulta paginada < 1 s para 10 elementos | [QA note 004](../004-reporte-y-seguimiento-de-incidencias/qa-note.md) | Media |
| RC-1 | Restricción | DTOs en controladores y enums VARCHAR | [ADR-03](../ADR/ADR-03-desacoplamiento-dtos-mappers.md), [ADR-01](../ADR/ADR-01-persistencia-enums-jpa.md) | Alta |

## 5. Arquitectura actual y dependencias

- **C4 Contexto/Contenedores:** [Málaga Open Data en C4 L1](../architecture.md#1-diagrama-de-contexto-c4-nivel-1); proxy en [C4 L2](../architecture.md#2-diagrama-de-contenedores-c4-nivel-2).
- **Flujo actual:** formulario → `POST /api/incidents` + previsualización `/geocode/reverse` → servicio → repositorio → PostgreSQL; servicio externo con fallback.
- **Dependencias y restricciones:** `RestTemplate`, Nominatim, H2/PostgreSQL, `PageResponse<T>`.

## 6. Alternativas consideradas

| Alternativa | Descripción | Ventajas | Inconvenientes / riesgos | Evidencia disponible |
|---|---|---|---|---|
| A | Geocodificación directa desde SPA | Sin backend | CORS y latencia en cliente | [ADR-06, opción 1](../ADR/ADR-06-integracion-datos-abiertos-malaga-degradacion-controlada.md) |
| B | PostGIS | Consultas espaciales | Complejidad geoespacial | [ADR-06, opción 2](../ADR/ADR-06-integracion-datos-abiertos-malaga-degradacion-controlada.md) |
| C | Proxy backend + degradación (elegida) | Centralizado y robusto | Timeout de red | [ADR-06, decisión](../ADR/ADR-06-integracion-datos-abiertos-malaga-degradacion-controlada.md) |

**Criterios de comparación:** CORS, robustez, complejidad, coherencia con ADR-06.
**Elección propuesta y justificación:** C, con fallback a Nominatim y degradación a `null`.

## 7. Diseño propuesto

### 7.1. Visión y diagramas C4

- **Contexto (L1):** [C4 L1](../architecture.md#1-diagrama-de-contexto-c4-nivel-1).
- **Contenedores (L2):** [C4 L2](../architecture.md#2-diagrama-de-contenedores-c4-nivel-2).
- **Componentes (L3):** `MalagaOpenDataService`, `IncidentService`, `IncidentMapper`, `CreateIncidentModal`, `MyIncidentsPage`.
- **Secuencias:** coordenadas → geocodificación inversa → alta con valores resueltos o nulos → listado paginado.

### 7.2. Interfaces y contratos

Los tres endpoints del apartado 2; `page` default 0, `size` 1-50 default 10; `title` 5-200, `description` ≥10, latitud/longitud en bbox Málaga, `address` ≤255. Ver [docs/api.md](../api.md). Respuesta 404 estructurada si no existe la incidencia.

### 7.3. Modelo y gestión de datos

Entidad `Incidencia` con campos de localización y estado; `ReverseGeocodeResponse` y `PageResponse<T>` como DTOs (ADR-03). Los fallos externos persisten `null`, nunca bloquean el registro.

### 7.4. Fallos, concurrencia y resiliencia

Timeout 3 s → fallback Nominatim → degradación a `null`. La paginación devuelve 200 con lista vacía fuera de rango. No hay circuit breaker configurado.

### 7.5. Seguridad

La identidad del ciudadano se toma del JWT (ADR-02), no del cuerpo de la petición. No se exponen entidades JPA (ADR-03); la factoría SSL permisiva requiere revisión de seguridad.

### 7.6. Operación y despliegue

Misma topología Compose; pruebas con mocks para no depender de la conectividad municipal. No hay SLOs numéricos salvo el timeout y el objetivo < 1 s del listado.

## 8. Decisiones y trade-offs

| Decisión propuesta | Beneficio | Coste / consecuencia | ¿Requiere ADR? |
|---|---|---|---|
| Proxy + degradación | Alta siempre disponible | Dependencia de red | No — ADR-06 (propuesto) |
| `PageResponse<T>` | Contrato paginado uniforme | DTO adicional | No — ADR-03 |
| Valores `null` → «Sin asignar» | UX clara | Pérdida temporal de contexto | No — spec 004 |
| Factoría SSL permisiva | Resiliencia ante certificados | Riesgo de seguridad | Sí, si se endurece |

### 8.1. Decisiones arquitectónicas derivadas

- [ADR-06: Integración con Datos Abiertos de Málaga y Degradación Controlada](../ADR/ADR-06-integracion-datos-abiertos-malaga-degradacion-controlada.md) — **propuesto**. Decisión de origen.
- [ADR-03](../ADR/ADR-03-desacoplamiento-dtos-mappers.md) y [ADR-01](../ADR/ADR-01-persistencia-enums-jpa.md) — soporte de contrato y persistencia.

## 9. Plan de implementación y migración

1. Implementar repositorio y consultas paginadas por ciudadano/estado.
2. Integrar `MalagaOpenDataService` con timeout y fallback en el alta.
3. Crear formulario, previsualización y vista «Mis incidencias».
4. Verificar con 18 RF y pruebas de fallo; reversión: desactivar previsualización y dejar campos manuales.

**Dependencias:** DD-008/DD-009 (JWT y roles), ADR-01/03/06. **Impacto en versiones anteriores:** ninguno.

## 10. Estrategia de verificación y evaluación

| Requisito | Prueba/experimento | Entorno y datos | Métrica / umbral | Evidencia esperada |
|---|---|---|---|---|
| RF-1/RF-3 | `IncidentServiceTest`, `IncidentControllerTest`, `IncidentRepositoryTest` | Local, H2 | 0 fallos | [QA note 004](../004-reporte-y-seguimiento-de-incidencias/qa-note.md): 272→276 tests backend |
| RF-2 | `MalagaOpenDataServiceTest` con mocks | Local/CI | Sin bloqueo del alta | QA note 004 |
| RQ-2 | Consulta paginada | Local | < 1 s / 10 elementos | QA note 004 |
| RC-1 | JaCoCo + Vitest coverage | CI | 70% instr. / 60% ramas | QA note 004: 95% instr. / 75% ramas backend; 66,6% sentencias frontend |

Los fallos reales de red/certificados solo se observan conectados al portal; en CI se simulan.

## 11. Riesgos, hipótesis y cuestiones abiertas

| ID | Tipo | Descripción | Impacto | Acción y responsable |
|---|---|---|---|---|
| R-1 | Riesgo | Certificados FNMT/Camerfirma no en truststore OpenJDK | Medio | Factoría SSL permisiva; ver `repare.md` |
| R-2 | Riesgo | ADR-06 sigue propuesto | Medio | Formalizar aprobación |
| Q-1 | Pregunta | ¿Sustituir el fallback Nominatim por fuente municipal? | Bajo | Decisión pendiente |
| Q-2 | Pregunta | El ADR-06 cita `/api/geo/reverse`; la implementación y las pruebas usan `/api/incidents/geocode/reverse` | Bajo | Aclarar la ruta canónica en el ADR sin cambiar su decisión — arquitectura |

## 12. Revisión y resultado

- **Comentarios de revisión:** pendientes.
- **Resolución:** pendiente de revisión técnica.
- **Decisiones registradas:** [ADR-06](../ADR/ADR-06-integracion-datos-abiertos-malaga-degradacion-controlada.md), [ADR-03](../ADR/ADR-03-desacoplamiento-dtos-mappers.md), [ADR-01](../ADR/ADR-01-persistencia-enums-jpa.md).
- **Diferencias observadas tras implementar:** SSL y fallback no describían en el ADR original; se documentaron en [`repare.md`](../004-reporte-y-seguimiento-de-incidencias/repare.md).

## Checklist de aprobación

- [x] Problema y alcance delimitados.
- [x] Drivers y restricciones documentados, sin atribuir al enunciado requisitos inexistentes.
- [x] Al menos dos alternativas comparadas o justificación de por qué no hay alternativas viables.
- [x] Diagramas, datos y contratos coherentes.
- [x] Fallos, seguridad, operación y costes considerados en proporción al cambio.
- [x] Criterios de aceptación verificables y plan de pruebas reproducible.
- [x] Riesgos y cuestiones abiertas explícitos.
- [ ] Revisión técnica realizada; ADR enlazados cuando corresponde.
