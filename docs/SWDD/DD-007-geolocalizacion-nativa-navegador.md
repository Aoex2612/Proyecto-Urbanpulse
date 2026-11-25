# DD-007 — Captura de ubicación con la API nativa W3C Geolocation

| Metadato | Valor |
|---|---|
| Estado | En revisión |
| Autoría | Equipo UrbanPulse (refactorización documental) |
| Fecha / última revisión | 2026-11-20 |
| Iteración | Refactorización documental: trazabilidad SDD ↔ ADR |
| Revisores | Equipo técnico UrbanPulse (pendiente de revisión) |
| Enlaces | [ADR-07](../ADR/ADR-07-geolocalizacion-nativa-navegador.md), [feature 003](../../features/003-mapa-incidencias-y-auditoria-transversal/spec.md), [tarea 10](../003-mapa-incidencias-y-auditoria-transversal/10-tarea.md), [QA note](../003-mapa-incidencias-y-auditoria-transversal/qa-note.md) |

## 1. Resumen ejecutivo

Para fijar con precisión el lugar de una incidencia desde el propio dispositivo, se optó por la API nativa `navigator.geolocation` encapsulada en `geolocationUtils.js`, frente a servicios de geolocalización por IP o plugins de Leaflet. Aporta precisión GPS real, consentimiento gestionado por el navegador y cero dependencias adicionales. El riesgo es la necesidad de contexto seguro (HTTPS/localhost) y la menor precisión en equipos de escritorio sin GPS.

> **Nota documental:** este Design Doc se numeraba originalmente como DD-06, igual que el de Datos Abiertos. Se renombró a DD-007 junto con su ADR para eliminar la colisión de numeración, sin cambiar ninguna decisión técnica.

## 2. Contexto y problema

- **Situación actual verificable:** `frontend/src/utils/geolocationUtils.js` expone `isGeolocationSupported`, captura con `enableHighAccuracy: true`, `timeout: 10000`, `maximumAge: 0`, 6 decimales y conservación de coordenadas previas ante error; ver [tarea 10](../003-mapa-incidencias-y-auditoria-transversal/10-tarea.md) y [ADR-07](../ADR/ADR-07-geolocalizacion-nativa-navegador.md).
- **Problema:** la geolocalización por IP tiene error de kilómetros, inútil para incidencias en vía pública; los plugins añaden dependencias y acoplan la captura al lienzo del mapa.
- **Motivación:** HU14.4 de la feature 003 y el principio de *geolocalización como eje central* en [mission.md](../../constitution/mission.md).

## 3. Objetivos, no objetivos y alcance

**Objetivos**
- O1. Obtener coordenadas precisas bajo demanda del usuario.
- O2. Respetar el consentimiento nativo del navegador y contextos seguros.
- O3. No perder el punto ya marcado si la captura falla o se deniega el permiso.

**No objetivos**
- N1. Añadir librerías de geolocalización ni servicios de pago.
- N2. Acceder a la ubicación sin acción explícita del usuario.
- N3. Sustituir la selección manual de coordenadas en el mapa (ADR-04).

**Dentro del alcance:** `geolocationUtils.js`, modal de alta, integración con `flyTo` del mapa, mocks de pruebas.
**Fuera del alcance:** geocodificación inversa (ADR-06), persistencia de coordenadas (ya cubierta por ADR-01).

## 4. Requisitos y drivers arquitectónicos

| ID | Tipo | Necesidad / escenario | Criterio de aceptación y evidencia | Prioridad |
|---|---|---|---|---|
| RF-1 | Funcional | Botón «Usar mi ubicación» fija lat/lon con 6 decimales | [Feature 003](../../features/003-mapa-incidencias-y-auditoria-transversal/spec.md) | Alta |
| RF-2 | Funcional | Denegación/timeout conserva coordenadas previas | Feature 003, RF | Alta |
| RC-1 | Restricción | Sin dependencias externas de geolocalización | [ADR-07](../ADR/ADR-07-geolocalizacion-nativa-navegador.md) | Alta |
| RC-2 | Restricción | Contexto seguro (HTTPS/localhost) | ADR-07, consecuencias | Alta |
| RQ-1 | Calidad | Coordenadas no obsoletas | `maximumAge: 0`, `enableHighAccuracy: true`, `timeout: 10000` en [tarea 10](../003-mapa-incidencias-y-auditoria-transversal/10-tarea.md) | Alta |

## 5. Arquitectura actual y dependencias

- **C4 Contexto/Contenedores:** la captura ocurre en la SPA; [C4 L2](../architecture.md#2-diagrama-de-contenedores-c4-nivel-2).
- **Flujo actual:** clic → `isGeolocationSupported` → `getCurrentPosition` → actualización del formulario → `flyTo` (zoom 16, 1.5 s) → envío a la API.
- **Dependencias y restricciones:** API W3C del navegador, contexts seguros, mocks de `navigator.geolocation` en Vitest.

## 6. Alternativas consideradas

| Alternativa | Descripción | Ventajas | Inconvenientes / riesgos | Evidencia disponible |
|---|---|---|---|---|
| A | API nativa W3C (elegida) | Precisión real, 0 dependencias, consentimiento nativo | Requiere HTTPS; hardware variable | [ADR-07, decisión](../ADR/ADR-07-geolocalizacion-nativa-navegador.md) |
| B | Geolocalización por IP (ipapi/ipstack) | Sin permisos | Error de kilómetros, servicios de terceros | [ADR-07, opción 2](../ADR/ADR-07-geolocalizacion-nativa-navegador.md) |
| C | Plugin `leaflet.locatecontrol` | Integración visual directa | Dependencia extra y acoplamiento al mapa | [ADR-07, opción 3](../ADR/ADR-07-geolocalizacion-nativa-navegador.md) |

**Criterios de comparación:** precisión, privacidad, dependencias, desacoplamiento de React.
**Elección propuesta y justificación:** A, aceptada en ADR-07 (antes ADR-06 de geolocalización).

## 7. Diseño propuesto

### 7.1. Visión y diagramas C4

- **Contexto (L1):** sin cambios, [C4 L1](../architecture.md#1-diagrama-de-contexto-c4-nivel-1).
- **Contenedores (L2):** navegador del usuario → SPA; sin servicio externo de geolocalización.
- **Componentes (L3):** `geolocationUtils.js`, `CreateIncidentModal`, `IncidentsMap`; ver [C4 L3](../architecture.md#3-diagramas-de-componentes-c4-nivel-3).
- **Secuencias:** usuario pulsa «Usar mi ubicación» → permiso → coordenadas → modal → `flyTo` → alta.

### 7.2. Interfaces y contratos

La utilidad devuelve latitud/longitud al modal; el contrato REST de alta usa `latitude`, `longitude` y `accuracyMeters` con los rangos de [docs/api.md](../api.md). No hay nuevos endpoints.

### 7.3. Modelo y gestión de datos

Las coordenadas se envían en `POST /api/incidents` y se persisten con el mapeo de ADR-01. `accuracyMeters` se muestra como precisión estimada; no se almacena el historial de ubicaciones del usuario.

### 7.4. Fallos, concurrencia y resiliencia

Estados gestionados: carga, permiso denegado, timeout, navegador sin soporte. En cualquier error se conserva el punto previo (`isMountedRef` evita actualizaciones tras desmontaje). No hay reintentos automáticos.

### 7.5. Seguridad y privacidad

El navegador solicita consentimiento explícito; solo se accede bajo demanda. No se envía ubicación a terceros. Se exige HTTPS en producción y `localhost` en desarrollo.

### 7.6. Operación y despliegue

Sin dependencias de red ni infraestructura nueva. No hay SLOs numéricos; la precisión depende del dispositivo y solo puede medirse en pruebas reales con GPS.

## 8. Decisiones y trade-offs

| Decisión propuesta | Beneficio | Coste / consecuencia | ¿Requiere ADR? |
|---|---|---|---|
| API nativa W3C | Precisión y 0 dependencias | Contexto seguro y hardware variable | No — ADR-07 |
| Preservar coordenadas ante error | UX robusta | Lógica adicional de estado | No — feature 003 |
| Renombrado ADR/DD a 07 | Elimina colisión de numeración | Actualizar referencias | No — decisión documental de esta refactorización |

### 8.1. Decisiones arquitectónicas derivadas

- [ADR-07: Captura de ubicación con API nativa W3C Geolocation en el cliente](../ADR/ADR-07-geolocalizacion-nativa-navegador.md) — **aceptado**. Decisión de origen (renombrado desde ADR-06).
- [ADR-04](../ADR/ADR-04-cartografia-interactiva-leaflet-openstreetmap.md) — el mapa donde se representa el punto capturado.

## 9. Plan de implementación y migración

1. Crear `geolocationUtils.js` con detección de soporte y `getCurrentPosition` configurado.
2. Integrar el botón en `CreateIncidentModal` con estados de carga/error.
3. Conectar `flyTo` y conservar coordenadas previas ante fallo.
4. Cubrir con mocks en Vitest y ejecutar `npm run lint`; reversión: ocultar el botón y mantener la selección manual.

**Dependencias:** ADR-04. **Impacto en versiones anteriores:** ninguno; la captura es opcional.

## 10. Estrategia de verificación y evaluación

| Requisito | Prueba/experimento | Entorno y datos | Métrica / umbral | Evidencia esperada |
|---|---|---|---|---|
| RF-1 | Tests de `geolocationUtils.js` con mocks | Vitest | 0 fallos | [QA note 003](../003-mapa-incidencias-y-auditoria-transversal/qa-note.md): cobertura 97,61% sentencias / 92,85% ramas en la utilidad |
| RF-2 | Simular denegación y timeout | Vitest | Coordenadas previas conservadas | Logs y aserciones de la suite |
| RC-2 | Verificar `window.isSecureContext` | Navegador/localhost | Captura bloqueada sin contexto seguro | [tarea 10](../003-mapa-incidencias-y-auditoria-transversal/10-tarea.md) |

La precisión real (GPS) solo puede validarse en un dispositivo físico; en CI se simula con mocks.

## 11. Riesgos, hipótesis y cuestiones abiertas

| ID | Tipo | Descripción | Impacto | Acción y responsable |
|---|---|---|---|---|
| R-1 | Riesgo | Navegadores de escritorio sin GPS | Medio | El usuario puede corregir el punto en el mapa |
| R-2 | Riesgo | Despliegue sin HTTPS bloquea la API | Alto | Verificar certificados en [deployment.md](../deployment.md) |
| Q-1 | Pregunta | ¿Registrar `accuracyMeters` en analítica? | Bajo | Decisión de producto pendiente |

## 12. Revisión y resultado

- **Comentarios de revisión:** pendientes.
- **Resolución:** pendiente de revisión técnica.
- **Decisiones registradas:** [ADR-07](../ADR/ADR-07-geolocalizacion-nativa-navegador.md).
- **Diferencias observadas tras implementar:** se añadió `flyTo` (zoom 16, 1.5 s), `isMountedRef` y redondeo a 6 decimales, no detallados en el ADR; ver [tarea 10](../003-mapa-incidencias-y-auditoria-transversal/10-tarea.md).

## Checklist de aprobación

- [x] Problema y alcance delimitados.
- [x] Drivers y restricciones documentados, sin atribuir al enunciado requisitos inexistentes.
- [x] Al menos dos alternativas comparadas o justificación de por qué no hay alternativas viables.
- [x] Diagramas, datos y contratos coherentes.
- [x] Fallos, seguridad, operación y costes considerados en proporción al cambio.
- [x] Criterios de aceptación verificables y plan de pruebas reproducible.
- [x] Riesgos y cuestiones abiertas explícitos.
- [ ] Revisión técnica realizada; ADR enlazados cuando corresponde.
