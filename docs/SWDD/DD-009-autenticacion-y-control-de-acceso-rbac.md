# DD-009 — Autenticación y control de acceso RBAC

| Metadato | Valor |
|---|---|
| Estado | En revisión |
| Autoría | Equipo UrbanPulse (refactorización documental) |
| Fecha / última revisión | 2026-11-20 |
| Iteración | Refactorización documental: trazabilidad SDD ↔ ADR |
| Revisores | Equipo técnico UrbanPulse (pendiente de revisión) |
| Enlaces | [Feature 002](../../features/002-autenticacion-y-control-de-acceso-rbac/spec.md), [plan 002](../../features/002-autenticacion-y-control-de-acceso-rbac/plan.md), [QA note](../002-autenticacion-y-control-de-acceso-rbac/qa-note.md), [ADR-02](../ADR/ADR-02-autenticacion-jwt-bcrypt-rbac.md) |

## 1. Resumen ejecutivo

La feature 002 protege la API y la SPA mediante JWT perimetral y autorización por roles (`CITIZEN`, `OPERATOR`, `TECHNICIAN`, `ADMIN`, `ANALYST`). Se materializa ADR-02 con `JwtAuthenticationFilter` + `SecurityConfig` STATELESS, y ADR-03 en los DTOs de error. La razón principal es evitar accesos no autorizados a operaciones de triaje y auditar el comportamiento de denegación (401 vs 403) sin purgar el token en un 403. El riesgo es la permisividad de CORS en desarrollo y la ausencia de SLOs.

## 2. Contexto y problema

- **Situación actual verificable:** rutas públicas `/api/auth/**` y `/h2-console/**`; `PATCH /api/incidents/{id}/status` permitido a OPERATOR/ADMIN y denegado a CITIZEN; `ProtectedRoute` + `RoleGuard` en la SPA; ver [spec 002](../../features/002-autenticacion-y-control-de-acceso-rbac/spec.md).
- **Problema:** sin filtro perimetral cada controlador tendría que validar identidad, y la SPA podría mostrar acciones no permitidas.
- **Motivación:** RF02 del [caso de estudio](../DESCRIPCION.md); feature implementada y validada sin ADR citado en sus documentos.

## 3. Objetivos, no objetivos y alcance

**Objetivos**
- O1. Rechazar peticiones sin token o con token inválido con 401 estructurado.
- O2. Aplicar `ROLE_<ROL>` en `SecurityContextHolder` y devolver 403 cuando corresponda.
- O3. Ocultar/proteger rutas de la SPA según rol, sin logout automático en 403.

**No objetivos**
- N1. Cambiar el mecanismo JWT definido en ADR-02.
- N2. Implementar permisos granulares por recurso más allá del rol.
- N3. Auditar accesos (DD-005 cubre cambios de estado, no logins).

**Dentro del alcance:** `JwtAuthenticationFilter`, `SecurityConfig`, manejadores 401/403, `ProtectedRoute`, `RoleGuard`, `/operator-panel`.
**Fuera del alcance:** emisión de tokens (DD-008), administración de roles.

## 4. Requisitos y drivers arquitectónicos

| ID | Tipo | Necesidad / escenario | Criterio de aceptación y evidencia | Prioridad |
|---|---|---|---|---|
| RF-1 | Funcional | Token ausente/expirado → 401 con `ErrorResponse` | [spec 002](../../features/002-autenticacion-y-control-de-acceso-rbac/spec.md) | Alta |
| RF-2 | Funcional | Rol del claim JWT → `ROLE_<ROL>` en el contexto | Spec 002 | Alta |
| RF-3 | Funcional | CITIZEN → 403 en triaje; anónimo → `/login`; sin rol → `/` con `accessDenied` | Spec 002, RF-10 | Alta |
| RC-1 | Restricción | Stateless + Spring Security 6 | [ADR-02](../ADR/ADR-02-autenticacion-jwt-bcrypt-rbac.md) | Alta |
| RQ-1 | Calidad | Comportamiento uniforme de errores 401/403 | `ErrorResponse` en [docs/api.md](../api.md) | Alta |

## 5. Arquitectura actual y dependencias

- **C4 Contexto/Contenedores:** [C4 L2](../architecture.md#2-diagrama-de-contenedores-c4-nivel-2).
- **Flujo actual:** petición → filtro JWT → `SecurityFilterChain` → controlador; si falla, `JwtAuthenticationEntryPoint` (401) o `CustomAccessDeniedHandler` (403).
- **Dependencias y restricciones:** Spring Security 6, JWT, React Router, Axios.

## 6. Alternativas consideradas

| Alternativa | Descripción | Ventajas | Inconvenientes / riesgos | Evidencia disponible |
|---|---|---|---|---|
| A | `HandlerInterceptor` por controlador | Implementación local | Se ejecuta tarde y se repite código | [plan 002](../../features/002-autenticacion-y-control-de-acceso-rbac/plan.md) |
| B | `JwtAuthenticationFilter` (elegida) | Perimetral y centralizado | Requiere manejo propio de 401/403 | [plan 002](../../features/002-autenticacion-y-control-de-acceso-rbac/plan.md), [ADR-02](../ADR/ADR-02-autenticacion-jwt-bcrypt-rbac.md) |
| C | Delegar en manejadores por defecto de Spring | Menos código | Respuestas no homogéneas con `ErrorResponse` | [plan 002](../../features/002-autenticacion-y-control-de-acceso-rbac/plan.md) |

**Criterios de comparación:** centralización, uniformidad de errores, coherencia con ADR-02.
**Elección propuesta y justificación:** B, con manejadores propios (alternativa C descartada), tal como quedó implementado.

## 7. Diseño propuesto

### 7.1. Visión y diagramas C4

- **Contexto (L1):** [sin cambios](../architecture.md#1-diagrama-de-contexto-c4-nivel-1).
- **Contenedores (L2):** SPA ↔ API REST con Bearer; [C4 L2](../architecture.md#2-diagrama-de-contenedores-c4-nivel-2).
- **Componentes (L3):** `JwtAuthenticationFilter`, `SecurityConfig`, guardias React.
- **Secuencias:** petición → validación JWT → authorities → decisión de acceso → 200/401/403.

### 7.2. Interfaces y contratos

`PATCH /api/incidents/{id}/status` → 200 (OPERATOR/ADMIN), 403 (CITIZEN), 401 (anónimo). Cuerpo de error `{timestamp, status, error, message, path}`; ver [docs/api.md](../api.md). Ruta SPA `/operator-panel` protegida.

### 7.3. Modelo y gestión de datos

Sin tablas nuevas. Los roles provienen del claim JWT y de `users.role` (ADR-01); los DTOs de respuesta/error siguen ADR-03.

### 7.4. Fallos, concurrencia y resiliencia

401 y 403 son respuestas terminales; no hay reintentos. En frontend, un 401 purga el token y redirige; un 403 **no** hace logout (RF-10), mostrando `location.state.accessDenied`.

### 7.5. Seguridad

`SessionCreationPolicy.STATELESS`; filtro antes de `UsernamePasswordAuthenticationFilter`; CORS permisivo solo en desarrollo según [docs/architecture.md](../architecture.md); los errores 401 son genéricos para evitar enumeración.

### 7.6. Operación y despliegue

Sin infraestructura nueva; misma topología Compose. No hay SLOs numéricos de autorización documentados.

## 8. Decisiones y trade-offs

| Decisión propuesta | Beneficio | Coste / consecuencia | ¿Requiere ADR? |
|---|---|---|---|
| Filtro perimetral frente a interceptor | Centralización | Pruebas específicas del filtro | No — ADR-02 |
| Manejadores 401/403 propios | Contrato de error uniforme | Más clases | No — ADR-03 |
| 403 sin logout | Evita perder sesión válida | Requiere UX de aviso | No — spec 002 |

### 8.1. Decisiones arquitectónicas derivadas

- [ADR-02: JWT + BCrypt + RBAC](../ADR/ADR-02-autenticacion-jwt-bcrypt-rbac.md) — **aceptado**. Decisión de origen.
- [ADR-03: DTOs y mappers](../ADR/ADR-03-desacoplamiento-dtos-mappers.md) — contrato de errores y respuestas.
- [ADR-01](../ADR/ADR-01-persistencia-enums-jpa.md) — persistencia del rol.

## 9. Plan de implementación y migración

1. Registrar `JwtAuthenticationFilter` en la cadena de seguridad.
2. Configurar reglas por rol y rutas públicas en `SecurityConfig`.
3. Implementar manejadores 401/403 con `ErrorResponse`.
4. Añadir `ProtectedRoute`/`RoleGuard` en React; reversión: desactivar reglas y usar acceso público controlado.

**Dependencias:** DD-008 (emisión de token), ADR-01/03. **Impacto en versiones anteriores:** endpoints antes públicos pasan a requerir token.

## 10. Estrategia de verificación y evaluación

| Requisito | Prueba/experimento | Entorno y datos | Métrica / umbral | Evidencia esperada |
|---|---|---|---|---|
| RF-1/RF-2 | `JwtAuthenticationFilterTest`, `JwtAuthenticationEntryPointTest` | Local | 0 fallos | [QA note 002](../002-autenticacion-y-control-de-acceso-rbac/qa-note.md): 116 tests |
| RF-3 | `IncidentControllerSecurityTest`, `SecurityConfigTest` | Local, H2 | 200/401/403 correctos | QA note 002 |
| RQ-1 | Verificación del cuerpo de error | Local | `{timestamp,status,error,message,path}` | Tests de controlador |

Validación local/CI; no requiere cloud real.

## 11. Riesgos, hipótesis y cuestiones abiertas

| ID | Tipo | Descripción | Impacto | Acción y responsable |
|---|---|---|---|---|
| R-1 | Riesgo | CORS permisivo en desarrollo | Medio | Restringir orígenes antes de producción — backend |
| R-2 | Riesgo | Reglas por rol desactualizadas al añadir endpoints | Medio | Revisión de PR y tests de seguridad |
| Q-1 | Pregunta | ¿Auditar intentos de acceso denegado? | Bajo | Decisión pendiente; afectaría a DD-005 |

## 12. Revisión y resultado

- **Comentarios de revisión:** pendientes.
- **Resolución:** pendiente de revisión técnica.
- **Decisiones registradas:** [ADR-02](../ADR/ADR-02-autenticacion-jwt-bcrypt-rbac.md), [ADR-03](../ADR/ADR-03-desacoplamiento-dtos-mappers.md), [ADR-01](../ADR/ADR-01-persistencia-enums-jpa.md).
- **Diferencias observadas tras implementar:** el 403 no purga el token y muestra aviso de acceso denegado; matiz documentado en la feature 002.

## Checklist de aprobación

- [x] Problema y alcance delimitados.
- [x] Drivers y restricciones documentados, sin atribuir al enunciado requisitos inexistentes.
- [x] Al menos dos alternativas comparadas o justificación de por qué no hay alternativas viables.
- [x] Diagramas, datos y contratos coherentes.
- [x] Fallos, seguridad, operación y costes considerados en proporción al cambio.
- [x] Criterios de aceptación verificables y plan de pruebas reproducible.
- [x] Riesgos y cuestiones abiertas explícitos.
- [ ] Revisión técnica realizada; ADR enlazados cuando corresponde.
