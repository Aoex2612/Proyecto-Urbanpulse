# DD-001 — Persistencia de enumeraciones con VARCHAR y restricciones CHECK

| Metadato | Valor |
|---|---|
| Estado | En revisión |
| Autoría | Equipo UrbanPulse (refactorización documental) |
| Fecha / última revisión | 2026-11-20 |
| Iteración | Refactorización documental: trazabilidad SDD ↔ ADR |
| Revisores | Equipo técnico UrbanPulse (pendiente de revisión) |
| Enlaces | [ADR-01](../ADR/ADR-01-persistencia-enums-jpa.md), [C4 vigente](../architecture.md), [esquema SQL](../../database/init/diagramUrbanPulse.sql), [stack](../../constitution/tech-stack.md) |

## 1. Resumen ejecutivo

El esquema inicial usaba tipos `ENUM` nativos de PostgreSQL para roles, estados y prioridades, mientras Hibernate 6 persiste las enumeraciones Java con `@Enumerated(EnumType.STRING)` sobre columnas `VARCHAR`. Esta discrepancia provocaba fallos de validación de esquema con `ddl-auto=update/validate`, especialmente en H2. Se propuso sustituir los `ENUM` nativos por `VARCHAR(30)` con restricciones `CHECK`, manteniendo la integridad de dominio en base de datos sin convertidores propietarios. El riesgo principal es la desincronización entre los literales Java y la lista de la restricción `CHECK` al añadir nuevos valores.

## 2. Contexto y problema

- **Situación actual verificable:** las entidades `Usuario` e `Incidencia` mapean `RolUsuario`, `IncidentStatus` e `IncidentPriority` con `@Enumerated(EnumType.STRING)`; el DDL de [`database/init/diagramUrbanPulse.sql`](../../database/init/diagramUrbanPulse.sql) define restricciones `CHECK` sobre columnas de texto. Así lo documenta [ADR-01](../ADR/ADR-01-persistencia-enums-jpa.md).
- **Problema:** los tipos `ENUM` nativos exigían `UserType`/`PGobject` o dependencias de terceros y no eran interpretados de forma homogénea por H2 en modo PostgreSQL, rompiendo arranques y pruebas.
- **Motivación:** el proyecto exige ejecutar las mismas pruebas contra PostgreSQL 16 (Docker) y H2 en memoria, según [`constitution/tech-stack.md`](../../constitution/tech-stack.md) y [`docs/development.md`](../development.md).

## 3. Objetivos, no objetivos y alcance

**Objetivos**
- O1. Arranque de Hibernate sin errores de validación de esquema en PostgreSQL y H2.
- O2. Conservar el dominio de valores válidos validado por la propia base de datos.
- O3. Mantener literales legibles en SQL y en volcados (`'CITIZEN'`, `'REPORTED'`).

**No objetivos**
- N1. Introducir dependencias de mapeo propietarias (Hypersistence, `PGobject`, convertidores `UserType`).
- N2. Cambiar la semántica de los enum Java ni los contratos REST.

**Dentro del alcance:** columnas de rol, estado y prioridad; script DDL; configuración JPA.
**Fuera del alcance:** migración a otro motor, versionado de esquema con Flyway/Liquibase, nuevos valores de enum.

## 4. Requisitos y drivers arquitectónicos

| ID | Tipo | Necesidad / escenario | Criterio de aceptación y evidencia | Prioridad |
|---|---|---|---|---|
| RC-1 | Restricción | Stack fijo: Java 21, Spring Boot 3.4.3, Hibernate 6, PostgreSQL 16 y H2 | [`constitution/tech-stack.md`](../../constitution/tech-stack.md) | Alta |
| RC-2 | Restricción | Sin dependencias propietarias de persistencia | [ADR-01, opciones 1 y 3](../ADR/ADR-01-persistencia-enums-jpa.md) | Alta |
| RQ-1 | Calidad | Arranque y pruebas con `ddl-auto=update/validate` | ADR-01 registra compatibilidad con los 116 tests unitarios existentes | Alta |
| RF-1 | Funcional | Rechazar valores fuera del catálogo permitido | Restricción `CHECK` activa en el DDL | Alta |

## 5. Arquitectura actual y dependencias

- **C4 Contexto/Contenedores:** [modelo C4 vigente](../architecture.md#2-diagrama-de-contenedores-c4-nivel-2).
- **Flujo actual:** API REST → servicios → repositorios Spring Data JPA → PostgreSQL/H2.
- **Dependencias y restricciones:** Hibernate 6, PostgreSQL 16 Alpine en Docker, H2 en pruebas, script DDL en `database/init/`.

## 6. Alternativas consideradas

| Alternativa | Descripción | Ventajas | Inconvenientes / riesgos | Evidencia disponible |
|---|---|---|---|---|
| A | `CREATE TYPE ... AS ENUM` + `UserType`/`@Type` propio | Tipado nativo en PostgreSQL | Conversores propietarios, incompatibilidad con H2, dependencias extra | [ADR-01](../ADR/ADR-01-persistencia-enums-jpa.md) |
| B | `VARCHAR(30)` + `CHECK` | Estándar JPA, portable, dominio validado en BD | Ligero coste de almacenamiento; hay que sincronizar CHECK y enum | [ADR-01, decisión](../ADR/ADR-01-persistencia-enums-jpa.md) |
| C | `EnumType.ORDINAL` | Ahorro de espacio | Desincronización silenciosa al reordenar enum; ilegible en SQL | [ADR-01](../ADR/ADR-01-persistencia-enums-jpa.md) |

**Criterios de comparación:** compatibilidad JPA/H2, integridad de dominio, portabilidad, legibilidad, coste de cambio.
**Elección propuesta y justificación:** la alternativa B, ya aceptada en ADR-01, es la única que satisface RC-1 y RC-2 sin conversores y conserva la validación en base de datos.

## 7. Diseño propuesto

### 7.1. Visión y diagramas C4

- **Contexto (L1):** sin cambios, ver [C4 L1](../architecture.md#1-diagrama-de-contexto-c4-nivel-1).
- **Contenedores (L2):** sin cambios; solo se ajusta el contrato físico de la base de datos.
- **Componentes (L3):** entidades JPA → repositorios → servicios.
- **Secuencias:** el servicio guarda una entidad → Hibernate escribe la cadena → la restricción `CHECK` valida el valor.

### 7.2. Interfaces y contratos

Sin cambios en la API REST. Los contratos JSON transportan los literales como cadenas; ver [docs/api.md](../api.md).

### 7.3. Modelo y gestión de datos

Columnas afectadas: `users.role`, `incidents.status`, `incidents.priority` y sus homólogas en tablas de trazabilidad. Definición: `VARCHAR(30) NOT NULL` + `CHECK (... IN (...))` en [el DDL](../../database/init/diagramUrbanPulse.sql).

### 7.4. Fallos, concurrencia y resiliencia

Un valor no incluido en la lista `CHECK` provoca error de integridad en escrituras. No hay reintentos: se trata de un error de programación que debe detectarse en pruebas. La concurrencia se resuelve en el nivel de transacción JPA habitual.

### 7.5. Seguridad

La restricción actúa como defensa en profundidad frente a valores no autorizados introducidos por cualquier cliente de la base de datos. No se almacenan secretos en estas columnas.

### 7.6. Operación y despliegue

Sin infraestructura nueva: PostgreSQL Docker + H2 en CI. Reversión: restaurar la versión anterior del script DDL y reejecutar las pruebas de esquema. No se definen SLOs específicos para este cambio.

## 8. Decisiones y trade-offs

| Decisión propuesta | Beneficio | Coste / consecuencia | ¿Requiere ADR? |
|---|---|---|---|
| `VARCHAR(30)` + `CHECK` frente a `ENUM` nativo | Compatibilidad JPA/H2 y dominio validado | Sincronizar manualmente la lista al añadir enum values | No — ya registrada en ADR-01 |

### 8.1. Decisiones arquitectónicas derivadas

- [ADR-01: Uso de VARCHAR con restricciones CHECK para persistencia de tipos enumerados con JPA](../ADR/ADR-01-persistencia-enums-jpa.md) — **aceptado**. Es la decisión de origen de este Design Doc.

## 9. Plan de implementación y migración

1. Sustituir los `CREATE TYPE ... AS ENUM` por columnas `VARCHAR(30)` con `CHECK` en el script DDL.
2. Verificar el mapeo `@Enumerated(EnumType.STRING)` en las entidades y ejecutar `mvn test` contra H2.
3. Validar el arranque con PostgreSQL en Docker (`docker compose up -d`).
4. Reversión: recuperar el DDL anterior, recrear el esquema y repetir las pruebas.

**Dependencias:** ninguna nueva. **Impacto en versiones anteriores:** ningún cambio en el contrato REST.

## 10. Estrategia de verificación y evaluación

| Requisito | Prueba/experimento | Entorno y datos | Métrica / umbral | Evidencia esperada |
|---|---|---|---|---|
| RQ-1 | `mvn test` con H2 y arranque con `ddl-auto=update/validate` | Local + CI | Sin errores de validación de esquema | Informe JaCoCo y logs de arranque |
| RF-1 | Inserción de un valor inválido | PostgreSQL/H2 | Rechazo por `CHECK` | Error de integridad en el test |

No existen SLOs de latencia o disponibilidad documentados para este cambio; solo puede validarse localmente y en CI, sin necesidad de cloud real.

## 11. Riesgos, hipótesis y cuestiones abiertas

| ID | Tipo | Descripción | Impacto | Acción y responsable |
|---|---|---|---|---|
| R-1 | Riesgo | Añadir un valor al enum Java sin actualizar el `CHECK` | Medio: fallos de inserción | Revisar el DDL en cada PR que modifique un enum — equipo backend |
| Q-1 | Pregunta | ¿Se documentarán SLOs numéricos de persistencia? | Bajo | Decisión pendiente del equipo de arquitectura |

## 12. Revisión y resultado

- **Comentarios de revisión:** pendientes.
- **Resolución:** pendiente de revisión técnica.
- **Decisiones registradas:** [ADR-01](../ADR/ADR-01-persistencia-enums-jpa.md).
- **Diferencias observadas tras implementar:** el diseño descrito coincide con el DDL y el mapeo actuales; no se han detectado desviaciones.

## Checklist de aprobación

- [x] Problema y alcance delimitados.
- [x] Drivers y restricciones documentados, sin atribuir al enunciado requisitos inexistentes.
- [x] Al menos dos alternativas comparadas o justificación de por qué no hay alternativas viables.
- [x] Diagramas, datos y contratos coherentes.
- [x] Fallos, seguridad, operación y costes considerados en proporción al cambio.
- [x] Criterios de aceptación verificables y plan de pruebas reproducible.
- [x] Riesgos y cuestiones abiertas explícitos.
- [ ] Revisión técnica realizada; ADR enlazados cuando corresponde.
