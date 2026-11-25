# DD-003 — Desacoplamiento de la API mediante DTOs y mappers dedicados

| Metadato | Valor |
|---|---|
| Estado | En revisión |
| Autoría | Equipo UrbanPulse (refactorización documental) |
| Fecha / última revisión | 2026-11-20 |
| Iteración | Refactorización documental: trazabilidad SDD ↔ ADR |
| Revisores | Equipo técnico UrbanPulse (pendiente de revisión) |
| Enlaces | [ADR-03](../ADR/ADR-03-desacoplamiento-dtos-mappers.md), [arquitectura](../architecture.md), [API](../api.md), [feature 006](../../features/006-adjuntos-multimedia-en-incidencias/spec.md) |

## 1. Resumen ejecutivo

Exponer entidades JPA directamente en los controladores acoplaba el contrato REST al modelo relacional y permitía fugas de campos sensibles y errores de serialización de relaciones perezosas. Se adoptó una capa de DTOs específicos por caso de uso y mappers explícitos bajo `BaseMapper<E, D>`, invocados únicamente desde la capa de servicio. La razón principal es el encapsulamiento y la predictibilidad frente al mapeo por reflexión. El coste es el boilerplate de clases adicionales y la disciplina necesaria para no devolver entidades desde los controladores.

## 2. Contexto y problema

- **Situación actual verificable:** paquete `org.bulbaseros.mappers` con `BaseMapper<E, D>` e implementaciones como `IncidentMapper` o `AttachmentMapper`; los controladores reciben y devuelven DTOs. Véase [ADR-03](../ADR/ADR-03-desacoplamiento-dtos-mappers.md) y [docs/architecture.md](../architecture.md).
- **Problema:** exposición directa de `Usuario`, `Incidencia`, etc., provocaba acoplamiento del contrato, riesgo de serializar `passwordHash`, `LazyInitializationException` y ciclos Jackson.
- **Motivación:** convención obligatoria en [`AGENTS.md`](../../AGENTS.md) y en [tech-stack.md](../../constitution/tech-stack.md): los controladores nunca exponen entidades JPA.

## 3. Objetivos, no objetivos y alcance

**Objetivos**
- O1. Ninguna entidad JPA cruza la frontera del controlador.
- O2. Transformaciones tipadas, testeables e invocadas en `service`.
- O3. Contratos JSON estables aunque cambie el esquema relacional.

**No objetivos**
- N1. Sustituir los mappers por MapStruct/ModelMapper.
- N2. Rediseñar el modelo de dominio ni el DDL.
- N3. Definir una librería genérica de mapeo reutilizable fuera de UrbanPulse.

**Dentro del alcance:** DTOs de petición/respuesta, `BaseMapper`, servicios y controladores REST.
**Fuera del alcance:** serialización de mensajes asíncronos (no existen en el alcance actual), migraciones de BD.

## 4. Requisitos y drivers arquitectónicos

| ID | Tipo | Necesidad / escenario | Criterio de aceptación y evidencia | Prioridad |
|---|---|---|---|---|
| RC-1 | Restricción | Convención de capas del proyecto | [AGENTS.md](../../AGENTS.md) y [ADR-03](../ADR/ADR-03-desacoplamiento-dtos-mappers.md) | Alta |
| RF-1 | Funcional | Respuestas REST con DTOs, nunca entidades | Revisión de controladores; [docs/api.md](../api.md) | Alta |
| RQ-1 | Calidad | Ausencia de `LazyInitializationException` y ciclos de serialización | ADR-03 registra eliminación de estos errores | Alta |
| RC-2 | Restricción | Lombok prevalece sobre boilerplate manual | [AGENTS.md](../../AGENTS.md) | Media |

## 5. Arquitectura actual y dependencias

- **C4 Contexto/Contenedores:** [C4 L2](../architecture.md#2-diagrama-de-contenedores-c4-nivel-2).
- **Flujo actual:** controlador → DTO de petición → servicio → mapper → entidad → repositorio → mapper inverso → DTO de respuesta.
- **Dependencias y restricciones:** Spring Beans, Lombok (`@Data`, `@Builder`, `@AllArgsConstructor`), JUnit para mappers.

## 6. Alternativas consideradas

| Alternativa | Descripción | Ventajas | Inconvenientes / riesgos | Evidencia disponible |
|---|---|---|---|---|
| A | Entidades con `@JsonIgnore`/`@JsonView` | Sin clases extra | Contrato acoplado al ORM, fugas y relaciones perezosas | [ADR-03](../ADR/ADR-03-desacoplamiento-dtos-mappers.md) |
| B | ModelMapper/MapStruct | Menos código manual | Reflexión/procesadores, comportamiento menos predecible | [ADR-03](../ADR/ADR-03-desacoplamiento-dtos-mappers.md) |
| C | DTOs + mappers explícitos (elegida) | Tipado, depurable, testeable | Boilerplate y disciplina de capas | [ADR-03, decisión](../ADR/ADR-03-desacoplamiento-dtos-mappers.md) |

**Criterios de comparación:** encapsulación, depurabilidad, coste de mantenimiento, estabilidad del contrato.
**Elección propuesta y justificación:** C, aceptada en ADR-03; satisface RC-1 y RQ-1 sin dependencias adicionales.

## 7. Diseño propuesto

### 7.1. Visión y diagramas C4

- **Contexto (L1):** sin cambios, [C4 L1](../architecture.md#1-diagrama-de-contexto-c4-nivel-1).
- **Contenedores (L2):** sin cambios; la transformación ocurre dentro del backend.
- **Componentes (L3):** [capa de mappers del backend](../architecture.md#3-diagramas-de-componentes-c4-nivel-3).
- **Secuencias:** petición JSON → DTO → servicio → mapper → entidad → repositorio; respuesta inversa.

### 7.2. Interfaces y contratos

Esquemas JSON documentados en [docs/api.md](../api.md): `AuthResponse`, `IncidentResponse`, `AttachmentResponse`, `PageResponse<T>`, `ErrorResponse`. Los DTOs de petición validan límites (por ejemplo `title` 5-200, `size` 1-50).

### 7.3. Modelo y gestión de datos

Sin cambios en el DDL. Entidades como `Incidencia`, `Usuario` o `ArchivoAdjunto` permanecen confinadas en `entity/` + `repository/` + `service/`. Ejemplo documentado: `attachments` → `AttachmentResponse` vía `AttachmentMapper` en [feature 006](../../features/006-adjuntos-multimedia-en-incidencias/plan.md).

### 7.4. Fallos, concurrencia y resiliencia

Los mappers no gestionan fallos de red; su responsabilidad es pura transformación. La atomicidad se mantiene en los servicios con `@Transactional`; los errores de validación se traducen a `ErrorResponse` en el manejo global de excepciones.

### 7.5. Seguridad

Evitar la serialización de `passwordHash` y de identificadores internos sensibles. Cada DTO expone explícitamente solo los campos públicos del contrato.

### 7.6. Operación y despliegue

Sin impacto en topología ni despliegue. Reversión: reintroducir temporalmente anotaciones de serialización en entidades, con el riesgo documentado en la alternativa A; no se recomienda.

## 8. Decisiones y trade-offs

| Decisión propuesta | Beneficio | Coste / consecuencia | ¿Requiere ADR? |
|---|---|---|---|
| Mappers explícitos `BaseMapper<E, D>` | Predecibles y testeables | Más clases que mantener | No — ADR-03 |
| Lombok en DTOs | Menos boilerplate | Dependencia de anotaciones | No — convención AGENTS.md |

### 8.1. Decisiones arquitectónicas derivadas

- [ADR-03: Desacoplamiento de capa de datos y API mediante DTOs y Mappers dedicados](../ADR/ADR-03-desacoplamiento-dtos-mappers.md) — **aceptado**. Decisión de origen.

## 9. Plan de implementación y migración

1. Definir `BaseMapper<E, D>` y una implementación por agregado expuesto.
2. Sustituir entidades por DTOs en firmas de controladores.
3. Invocar los mappers desde servicios, nunca desde controladores.
4. Añadir pruebas unitarias de mapper y verificar con `mvn test`; reversión temporal descrita en 7.6.

**Dependencias:** ninguna nueva. **Impacto en versiones anteriores:** contratos JSON estables; solo cambia la representación interna.

## 10. Estrategia de verificación y evaluación

| Requisito | Prueba/experimento | Entorno y datos | Métrica / umbral | Evidencia esperada |
|---|---|---|---|---|
| RF-1 | Revisión de firmas de controladores + tests de controlador | Local/CI | 0 entidades JPA en controladores | [QA note 006](../006-adjuntos-multimedia-en-incidencias/qa-note.md), `AttachmentMapperTest` |
| RQ-1 | Pruebas de serialización de respuestas | Local, H2 | Sin `LazyInitializationException` | Informe JaCoCo y logs CI |

No hay SLOs numéricos asociados; validación local/CI.

## 11. Riesgos, hipótesis y cuestiones abiertas

| ID | Tipo | Descripción | Impacto | Acción y responsable |
|---|---|---|---|---|
| R-1 | Riesgo | Un controlador vuelva a devolver una entidad | Medio | Revisión de PR; checklist de AGENTS.md |
| R-2 | Riesgo | DTOs duplicados con semántica divergente | Bajo | Centralizar DTOs compartidos en `dto/` |
| Q-1 | Pregunta | ¿Generar DTOs a partir de OpenAPI en el futuro? | Bajo | Decisión pendiente; requeriría nuevo ADR |

## 12. Revisión y resultado

- **Comentarios de revisión:** pendientes.
- **Resolución:** pendiente de revisión técnica.
- **Decisiones registradas:** [ADR-03](../ADR/ADR-03-desacoplamiento-dtos-mappers.md).
- **Diferencias observadas tras implementar:** los mappers (`IncidentMapper`, `AttachmentMapper`, etc.) se invocan desde servicios, tal como prescribe el ADR; no se detectaron desviaciones.

## Checklist de aprobación

- [x] Problema y alcance delimitados.
- [x] Drivers y restricciones documentados, sin atribuir al enunciado requisitos inexistentes.
- [x] Al menos dos alternativas comparadas o justificación de por qué no hay alternativas viables.
- [x] Diagramas, datos y contratos coherentes.
- [x] Fallos, seguridad, operación y costes considerados en proporción al cambio.
- [x] Criterios de aceptación verificables y plan de pruebas reproducible.
- [x] Riesgos y cuestiones abiertas explícitos.
- [ ] Revisión técnica realizada; ADR enlazados cuando corresponde.
