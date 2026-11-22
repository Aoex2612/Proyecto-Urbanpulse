# DD-005 — Trazabilidad inmutable de cambios de estado

| Metadato | Valor |
|---|---|
| Estado | En revisión |
| Autoría | Equipo UrbanPulse (refactorización documental) |
| Fecha / última revisión | 2026-11-20 |
| Iteración | Refactorización documental: trazabilidad SDD ↔ ADR |
| Revisores | Equipo técnico UrbanPulse (pendiente de revisión) |
| Enlaces | [ADR-05](../ADR/ADR-05-trazabilidad-inmutable-auditoria-estados.md), [ADR-01](../ADR/ADR-01-persistencia-enums-jpa.md), [feature 003](../../features/003-mapa-incidencias-y-auditoria-transversal/spec.md), [esquema SQL](../../database/init/diagramUrbanPulse.sql) |

## 1. Resumen ejecutivo

El ciclo de vida de una incidencia (`REPORTED` → `VALIDATED` → `ASSIGNED` → `IN_PROGRESS` → `RESOLVED`/`CLOSED`/`REJECTED`) exige reconstruir quién cambió el estado, cuándo, desde qué estado y con qué motivo. Se eligió una tabla relacional dedicada e inmutable (`status_changes`) con inserciones orquestadas por el servicio de negocio dentro de la misma transacción. Frente a Envers/CDC o un array JSON, ofrece semántica de dominio explícita y consultas SQL directas. El riesgo es el crecimiento lineal de la tabla a largo plazo.

## 2. Contexto y problema

- **Situación actual verificable:** tabla `status_changes` con `id`, `incident_id`, `from_status`, `to_status`, `changed_by_user_id`, `reason`, `created_at`; timeline en frontend y endpoint `GET /api/incidents/{id}/history` restringido a ADMIN. Véase [feature 003](../../features/003-mapa-incidencias-y-auditoria-transversal/spec.md) y [ADR-05](../ADR/ADR-05-trazabilidad-inmutable-auditoria-estados.md).
- **Problema:** la auditoría genérica no permitía distinguir eventos de negocio deliberados ni ofrecer un historial legible para ciudadanía y municipalidad.
- **Motivación:** principio de *trazabilidad inmutable* en [constitution/mission.md](../../constitution/mission.md) y RF12/RF18 del [caso de estudio](../DESCRIPCION.md).

## 3. Objetivos, no objetivos y alcance

**Objetivos**
- O1. Registrar cada transición de estado con actor, instante, estado anterior, estado nuevo y motivo.
- O2. Garantizar que las filas históricas nunca se actualizan ni se borran.
- O3. Exponer el historial ordenado cronológicamente al rol autorizado.

**No objetivos**
- N1. Auditar todas las tablas del sistema con un framework genérico.
- N2. Implementar particionado o archivado en esta iteración.
- N3. Cambiar la máquina de estados definida en la feature 003.

**Dentro del alcance:** `status_changes`, servicio de cambios de estado, DTO `StatusChangeDTO`, componente `Timeline`, endpoint `/history`.
**Fuera del alcance:** auditoría de accesos, logs de aplicación, notificaciones (fuera del alcance actual).

## 4. Requisitos y drivers arquitectónicos

| ID | Tipo | Necesidad / escenario | Criterio de aceptación y evidencia | Prioridad |
|---|---|---|---|---|
| RF-1 | Funcional | `PATCH /api/incidents/{id}/status` inserta una transición | [Feature 003](../../features/003-mapa-incidencias-y-auditoria-transversal/spec.md) | Alta |
| RF-2 | Funcional | `GET /api/incidents/{id}/history` devuelve el timeline; 403 para otros roles; 404 si no existe | Feature 003 | Alta |
| RC-1 | Restricción | Estados almacenados como cadena con `CHECK` | [ADR-01](../ADR/ADR-01-persistencia-enums-jpa.md) | Alta |
| RQ-1 | Calidad | Reconstrucción forense y agregaciones de tiempo de ciclo | [ADR-05, razones](../ADR/ADR-05-trazabilidad-inmutable-auditoria-estados.md) | Alta |

## 5. Arquitectura actual y dependencias

- **C4 Contexto/Contenedores:** [C4 L2](../architecture.md#2-diagrama-de-contenedores-c4-nivel-2).
- **Flujo actual:** PATCH → `IncidentService` (`@Transactional`) → actualización de `incidents` + inserción en `status_changes` → mapper → `StatusChangeDTO`.
- **Dependencias y restricciones:** PostgreSQL 16/H2, Spring Data JPA, `@Transactional`, roles RBAC (ADR-02).

## 6. Alternativas consideradas

| Alternativa | Descripción | Ventajas | Inconvenientes / riesgos | Evidencia disponible |
|---|---|---|---|---|
| A | Hibernate Envers / CDC | Auditoría automática de todas las tablas | Metadatos crípticos, esquemas duplicados | [ADR-05](../ADR/ADR-05-trazabilidad-inmutable-auditoria-estados.md) |
| B | Array JSON/JSONB en `incidents` | Sin tabla extra | Consultas y agregaciones difíciles; no inmutable por diseño | [ADR-05](../ADR/ADR-05-trazabilidad-inmutable-auditoria-estados.md) |
| C | Tabla relacional dedicada (elegida) | Eventos de negocio explícitos, SQL directo | Crecimiento lineal | [ADR-05, decisión](../ADR/ADR-05-trazabilidad-inmutable-auditoria-estados.md) |

**Criterios de comparación:** inmutabilidad, semántica de dominio, consultabilidad, simplicidad.
**Elección propuesta y justificación:** C, aceptada en ADR-05; es la que mejor satisface RF-1/RQ-1 con PostgreSQL y H2.

## 7. Diseño propuesto

### 7.1. Visión y diagramas C4

- **Contexto (L1):** [sin cambios](../architecture.md#1-diagrama-de-contexto-c4-nivel-1).
- **Contenedores (L2):** la auditoría reside en la base de datos relacional, dentro del backend.
- **Componentes (L3):** `IncidentService`, `StatusChange` mapper, `Timeline`; ver [C4 L3](../architecture.md#3-diagramas-de-componentes-c4-nivel-3).
- **Secuencias:** operador cambia estado → transacción → `UPDATE incidents` + `INSERT status_changes` → respuesta con el nuevo estado.

### 7.2. Interfaces y contratos

`PATCH /api/incidents/{id}/status` (OPERATOR/ADMIN; CITIZEN → 403); `GET /api/incidents/{id}/history` (ADMIN; 403/404 según caso). Formato de error en [docs/api.md](../api.md).

### 7.3. Modelo y gestión de datos

`status_changes` con clave foránea a `incidents` y `users`; `from_status`/`to_status` como `VARCHAR(30)` con `CHECK` (ADR-01). Regla: **solo INSERT**, nunca UPDATE/DELETE. Definición en [el DDL](../../database/init/diagramUrbanPulse.sql).

### 7.4. Fallos, concurrencia y resiliencia

La inserción del evento histórico y la actualización de la incidencia comparten `@Transactional`: si falla una, se revierte la otra. No hay reintentos automáticos; el cliente debe asumir el error 409/500 correspondiente.

### 7.5. Seguridad

El endpoint de historial está restringido por RBAC (ADR-02); el DTO no expone datos sensibles del actor más allá de lo necesario para la auditoría. La inmutabilidad protege frente a borrados accidentales o intencionados.

### 7.6. Operación y despliegue

Sin nueva infraestructura. ADR-05 advierte que, a gran escala, hará falta particionado o archivado; no hay SLOs numéricos definidos para la consulta de historial.

## 8. Decisiones y trade-offs

| Decisión propuesta | Beneficio | Coste / consecuencia | ¿Requiere ADR? |
|---|---|---|---|
| Tabla `status_changes` inmutable | Trazabilidad forense y SQL directo | Crecimiento lineal | No — ADR-05 |
| Estados como VARCHAR + CHECK | Compatibilidad JPA/H2 | CHECK a sincronizar | No — ADR-01 |
| Historial solo ADMIN en `/history` | Auditoría restringida | El ciudadano ve timeline acotado en el detalle | No — feature 003 |

### 8.1. Decisiones arquitectónicas derivadas

- [ADR-05: Trazabilidad inmutable mediante tabla relacional dedicada e inserción de transiciones de estado](../ADR/ADR-05-trazabilidad-inmutable-auditoria-estados.md) — **aceptado**. Decisión de origen.
- [ADR-01](../ADR/ADR-01-persistencia-enums-jpa.md) y [ADR-02](../ADR/ADR-02-autenticacion-jwt-bcrypt-rbac.md) — soporte de persistencia y autorización.

## 9. Plan de implementación y migración

1. Crear la tabla `status_changes` en el DDL con claves foráneas e índice por fecha.
2. Modificar `IncidentService` para insertar la transición dentro de la transacción de cambio de estado.
3. Exponer `StatusChangeDTO` y el componente `Timeline` en el frontend.
4. Probar transiciones válidas/inválidas y permisos; reversión: conservar la tabla y desactivar el endpoint `/history`.

**Dependencias:** ADR-01, ADR-02, ADR-03. **Impacto en versiones anteriores:** las incidencias existentes conservan su estado actual; el histórico se construye hacia adelante.

## 10. Estrategia de verificación y evaluación

| Requisito | Prueba/experimento | Entorno y datos | Métrica / umbral | Evidencia esperada |
|---|---|---|---|---|
| RF-1 | Tests de servicio de cambio de estado | Local, H2 | 0 fallos | [QA note 003](../003-mapa-incidencias-y-auditoria-transversal/qa-note.md): 272 tests backend (1 omitido) |
| RF-2 | Tests de controlador con roles ADMIN/OPERATOR/CITIZEN | Local | 200/403/404 según rol | Informe JaCoCo del backend |
| RQ-1 | Consulta ordenada del historial | Local, PostgreSQL/H2 | Sin error | Revisión de tests; no hay umbral de latencia definido |

La validación es local/CI; no se requiere cloud real.

## 11. Riesgos, hipótesis y cuestiones abiertas

| ID | Tipo | Descripción | Impacto | Acción y responsable |
|---|---|---|---|---|
| R-1 | Riesgo | Crecimiento lineal de `status_changes` | Medio a largo plazo | Evaluar particionado/archivado — arquitectura |
| R-2 | Riesgo | Cambio de estado sin inserción de auditoría por código nuevo | Alto | Revisión de PR y tests de servicio |
| Q-1 | Pregunta | ¿Exponer histórico completo al ciudadano? | Medio | Decisión de producto pendiente |

## 12. Revisión y resultado

- **Comentarios de revisión:** pendientes.
- **Resolución:** pendiente de revisión técnica.
- **Decisiones registradas:** [ADR-05](../ADR/ADR-05-trazabilidad-inmutable-auditoria-estados.md).
- **Diferencias observadas tras implementar:** el frontend restringe el `Timeline` a ADMIN y muestra un histórico acotado en el detalle, matiz no explícito en el ADR y documentado en la feature 003.

## Checklist de aprobación

- [x] Problema y alcance delimitados.
- [x] Drivers y restricciones documentados, sin atribuir al enunciado requisitos inexistentes.
- [x] Al menos dos alternativas comparadas o justificación de por qué no hay alternativas viables.
- [x] Diagramas, datos y contratos coherentes.
- [x] Fallos, seguridad, operación y costes considerados en proporción al cambio.
- [x] Criterios de aceptación verificables y plan de pruebas reproducible.
- [x] Riesgos y cuestiones abiertas explícitos.
- [ ] Revisión técnica realizada; ADR enlazados cuando corresponde.
