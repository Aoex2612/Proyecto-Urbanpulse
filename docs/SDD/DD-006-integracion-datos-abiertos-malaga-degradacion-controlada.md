# DD-006 — Integración con Datos Abiertos de Málaga y degradación controlada

| Metadato | Valor |
|---|---|
| Estado | En revisión |
| Autoría | Equipo UrbanPulse (refactorización documental) |
| Fecha / última revisión | 2026-11-20 |
| Iteración | Refactorización documental: trazabilidad SDD ↔ ADR |
| Revisores | Equipo técnico UrbanPulse (pendiente de revisión) |
| Enlaces | [ADR-06](../ADR/ADR-06-integracion-datos-abiertos-malaga-degradacion-controlada.md), [feature 004](../../features/004-reporte-y-seguimiento-de-incidencias/spec.md), [plan 004](../../features/004-reporte-y-seguimiento-de-incidencias/plan.md), [QA note](../004-reporte-y-seguimiento-de-incidencias/qa-note.md) |

## 1. Resumen ejecutivo

Para resolver dirección, barrio y distrito a partir de coordenadas (HU04/HU15) sin bloquear el alta de incidencias, se centralizó la consulta al portal de Datos Abiertos de Málaga en el backend (`MalagaOpenDataService`) con un proxy REST, timeout de 3 segundos y degradación a `null` («Sin asignar») ante fallo. Esta decisión evita CORS y latencia incontrolada en la SPA y mantiene el principio de degradación controlada. El riesgo es la dependencia de red externa y la confianza en certificados de administraciones públicas, mitigada con un cliente SSL permisivo y fallback a Nominatim.

## 2. Contexto y problema

- **Situación actual verificable:** `GET /api/incidents/geocode/reverse?latitude&longitude`, servicio `MalagaOpenDataService`, fallback Nominatim, bbox de Málaga lat `[36.5000, 36.9000]`, lon `[-4.7000, -4.2000]`. Ver [plan 004](../../features/004-reporte-y-seguimiento-de-incidencias/plan.md) y [ADR-06](../ADR/ADR-06-integracion-datos-abiertos-malaga-degradacion-controlada.md).
- **Problema:** una consulta directa desde el navegador introduce CORS y expone la SPA a caídas del proveedor; PostGIS añadiría sobrecarga geoespacial no justificada.
- **Motivación:** RF04, RF21, RF26 del [caso de estudio](../DESCRIPCION.md) y la feature 004, implementada y validada. **El ADR-06 sigue en estado `propuesto`**, por lo que su formalización sigue abierta.

## 3. Objetivos, no objetivos y alcance

**Objetivos**
- O1. Resolver dirección/barrio/distrito desde coordenadas dentro del bbox municipal.
- O2. Nunca bloquear el registro de una incidencia por fallo de la fuente externa.
- O3. Centralizar la normalización de valores en el servidor.

**No objetivos**
- N1. Persistir el callejero completo ni instalar PostGIS.
- N2. Llamar al proveedor externo desde el navegador.
- N3. Garantizar disponibilidad de la fuente externa (fuera de nuestro control).

**Dentro del alcance:** `MalagaOpenDataService`, endpoint `/api/incidents/geocode/reverse`, timeouts, fallback, degradación a `null`.
**Fuera del alcance:** catálogo de activos urbanos y contexto meteorológico (backlog), mapa base (ADR-04).

## 4. Requisitos y drivers arquitectónicos

| ID | Tipo | Necesidad / escenario | Criterio de aceptación y evidencia | Prioridad |
|---|---|---|---|---|
| RF-1 | Funcional | Previsualizar dirección al reportar | `ReverseGeocodeResponse{address, district, neighborhood}` en [feature 004](../../features/004-reporte-y-seguimiento-de-incidencias/spec.md) | Alta |
| RF-2 | Funcional | Campos no resueltos se muestran como «Sin asignar» | Feature 004, RF | Alta |
| RC-1 | Restricción | Degradación controlada: fallo externo nunca bloquea el alta | [constitution/mission.md](../../constitution/mission.md), [ADR-06](../ADR/ADR-06-integracion-datos-abiertos-malaga-degradacion-controlada.md) | Alta |
| RQ-1 | Calidad | Timeout estricto de 3 s en la llamada externa | [ADR-06](../ADR/ADR-06-integracion-datos-abiertos-malaga-degradacion-controlada.md), `plan.md` | Alta |
| RQ-2 | Calidad | Listado paginado < 1 s para 10 elementos | [QA note 004](../004-reporte-y-seguimiento-de-incidencias/qa-note.md) | Media |

## 5. Arquitectura actual y dependencias

- **C4 Contexto/Contenedores:** Málaga Open Data como sistema externo en [C4 L1](../architecture.md#1-diagrama-de-contexto-c4-nivel-1); backend como proxy en [C4 L2](../architecture.md#2-diagrama-de-contenedores-c4-nivel-2).
- **Flujo actual:** SPA → `/api/incidents/geocode/reverse` → `MalagaOpenDataService` → API externa (3 s) → fallback Nominatim → DTO → SPA.
- **Dependencias y restricciones:** `RestTemplate`, factoría SSL permisiva, `User-Agent: UrbanPulse-App/1.0`, sin PostGIS.

## 6. Alternativas consideradas

| Alternativa | Descripción | Ventajas | Inconvenientes / riesgos | Evidencia disponible |
|---|---|---|---|---|
| A | Consultas directas desde el navegador | Sin backend extra | CORS, latencia y lógica duplicada en cliente | [ADR-06, opción 1](../ADR/ADR-06-integracion-datos-abiertos-malaga-degradacion-controlada.md) |
| B | PostGIS + capas vectoriales persistidas | Consultas espaciales potentes | Extensión geoespacial y datos duplicados | [ADR-06, opción 2](../ADR/ADR-06-integracion-datos-abiertos-malaga-degradacion-controlada.md) |
| C | Servicio backend con proxy y degradación (elegida) | Centralizado, sin CORS, robusto | Dependencia de red y timeouts | [ADR-06, decisión](../ADR/ADR-06-integracion-datos-abiertos-malaga-degradacion-controlada.md) |

**Criterios de comparación:** complejidad, CORS, latencia, robustez, coste de infraestructura.
**Elección propuesta y justificación:** C, recogida en ADR-06 (aún propuesto); es la única que cumple RC-1 sin ampliar la infraestructura.

## 7. Diseño propuesto

### 7.1. Visión y diagramas C4

- **Contexto (L1):** [Málaga Open Data como fuente externa](../architecture.md#1-diagrama-de-contexto-c4-nivel-1).
- **Contenedores (L2):** el backend media entre SPA y fuente externa; [C4 L2](../architecture.md#2-diagrama-de-contenedores-c4-nivel-2).
- **Componentes (L3):** `MalagaOpenDataService`, `GeoController`, mapper de respuesta.
- **Secuencias:** clic en mapa → petición de geocodificación inversa → timeout/fallback → dirección o `null` → formulario.

### 7.2. Interfaces y contratos

`GET /api/incidents/geocode/reverse?latitude&longitude` → `ReverseGeocodeResponse{address, district, neighborhood}`. Errores y validación de coordenadas en [docs/api.md](../api.md). Restricciones: latitud 36.5000-36.9000, longitud -4.7000/-4.2000, `accuracyMeters` 0.0-500.0.

### 7.3. Modelo y gestión de datos

Sin tablas nuevas: los campos `address`, `district` y `neighborhood` viven en `incidents`. Un fallo externo persiste `null`, mostrado como «Sin asignar». No se guarda copia del catálogo externo.

### 7.4. Fallos, concurrencia y resiliencia

Timeout de 3000 ms; excepción capturada → fallback Nominatim → si también falla, degradación a `null`. No hay reintentos infinitos ni circuit breaker; el riesgo queda registrado en la sección 11.

### 7.5. Seguridad

El proxy evita exponer URLs internas y credenciales de proveedores en el cliente. La factoría SSL permisiva fue necesaria por certificados FNMT/Camerfirma no presentes en el truststore de OpenJDK, según [feature 004, `repare.md`](../004-reporte-y-seguimiento-de-incidencias/repare.md); debe evaluarse su impacto de seguridad antes de un despliegue sensible.

### 7.6. Operación y despliegue

Sin infraestructura nueva; las pruebas simulan respuestas, fallos y timeouts con mocks para no depender de conectividad en CI. No hay SLOs numéricos de disponibilidad de la fuente externa; solo el timeout de 3 s y el objetivo < 1 s del listado paginado.

## 8. Decisiones y trade-offs

| Decisión propuesta | Beneficio | Coste / consecuencia | ¿Requiere ADR? |
|---|---|---|---|
| Proxy backend + degradación | Sin CORS, fallo aislado | Dependencia de red | No — ADR-06 (propuesto) |
| Factoría SSL permisiva + fallback | Resiliencia ante certificados y caídas | Requiere revisión de seguridad | Sí, si se decide endurecer la política SSL |
| Sin PostGIS | Arquitectura simple | Sin consultas espaciales avanzadas | No — ADR-06 |

### 8.1. Decisiones arquitectónicas derivadas

- [ADR-06: Integración con Datos Abiertos de Málaga y Degradación Controlada](../ADR/ADR-06-integracion-datos-abiertos-malaga-degradacion-controlada.md) — **propuesto**. Es la decisión de origen; su aprobación formal queda como cuestión abierta.
- [ADR-03](../ADR/ADR-03-desacoplamiento-dtos-mappers.md) — el DTO de geocodificación se mapea en la capa de servicio.

## 9. Plan de implementación y migración

1. Implementar `MalagaOpenDataService` con timeout de 3 s, User-Agent y fallback.
2. Exponer el endpoint de geocodificación inversa y el DTO de respuesta.
3. Integrar la previsualización en el formulario de alta y aplicar «Sin asignar» en nulos.
4. Simular fallos en tests; reversión: desactivar el endpoint y dejar los campos manuales/vacíos.

**Dependencias:** ADR-03, ADR-01. **Impacto en versiones anteriores:** ninguno; los valores previos se mantienen.

## 10. Estrategia de verificación y evaluación

| Requisito | Prueba/experimento | Entorno y datos | Métrica / umbral | Evidencia esperada |
|---|---|---|---|---|
| RF-1 | `MalagaOpenDataServiceTest` con mocks | Local/CI | 0 fallos | [QA note 004](../004-reporte-y-seguimiento-de-incidencias/qa-note.md): 276 tests backend tras el fix SSL |
| RQ-1 | Inyección de timeout en test | Local | 3 s, sin bloqueo del alta | Logs y aserciones de degradación |
| RQ-2 | Consulta paginada | Local | < 1 s para 10 elementos | QA note 004 |

Los fallos reales de red y certificados solo se observan en un despliegue conectado al portal municipal; en CI se simulan con mocks.

## 11. Riesgos, hipótesis y cuestiones abiertas

| ID | Tipo | Descripción | Impacto | Acción y responsable |
|---|---|---|---|---|
| R-1 | Riesgo | Indisponibilidad del portal externo | Medio | Degradación a `null` ya implementada |
| R-2 | Riesgo | Factoría SSL permisiva | Medio-Alto | Evaluar truststore dedicado — seguridad |
| Q-1 | Pregunta | ADR-06 continúa en estado `propuesto` | Medio | Formalizar aprobación del ADR — arquitectura |
| Q-2 | Hipótesis | El fallback Nominatim mantiene precisión suficiente | Bajo | Medir concordancia con datos oficiales |
| Q-3 | Pregunta | El ADR-06 cita `/api/geo/reverse`, pero el código y las pruebas usan `/api/incidents/geocode/reverse` | Bajo | Aclarar la ruta canónica en el ADR sin cambiar su decisión — arquitectura |

## 12. Revisión y resultado

- **Comentarios de revisión:** pendientes.
- **Resolución:** pendiente de revisión técnica.
- **Decisiones registradas:** [ADR-06](../ADR/ADR-06-integracion-datos-abiertos-malaga-degradacion-controlada.md) (propuesto).
- **Diferencias observadas tras implementar:** se añadió una factoría SSL permisiva y fallback a Nominatim, no detallados en el ADR original; documentados en [`repare.md`](../004-reporte-y-seguimiento-de-incidencias/repare.md).

## Checklist de aprobación

- [x] Problema y alcance delimitados.
- [x] Drivers y restricciones documentados, sin atribuir al enunciado requisitos inexistentes.
- [x] Al menos dos alternativas comparadas o justificación de por qué no hay alternativas viables.
- [x] Diagramas, datos y contratos coherentes.
- [x] Fallos, seguridad, operación y costes considerados en proporción al cambio.
- [x] Criterios de aceptación verificables y plan de pruebas reproducible.
- [x] Riesgos y cuestiones abiertas explícitos.
- [ ] Revisión técnica realizada; ADR enlazados cuando corresponde.
