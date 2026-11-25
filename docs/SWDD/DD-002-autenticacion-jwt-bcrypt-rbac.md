# DD-002 — Autenticación perimetral sin estado con JWT, BCrypt y RBAC

| Metadato | Valor |
|---|---|
| Estado | En revisión |
| Autoría | Equipo UrbanPulse (refactorización documental) |
| Fecha / última revisión | 2026-11-20 |
| Iteración | Refactorización documental: trazabilidad SDD ↔ ADR |
| Revisores | Equipo técnico UrbanPulse (pendiente de revisión) |
| Enlaces | [ADR-02](../ADR/ADR-02-autenticacion-jwt-bcrypt-rbac.md), [ADR-01](../ADR/ADR-01-persistencia-enums-jpa.md), [feature 001](../../features/001-registro-e-inicio-de-sesion-de-ciudadanos/spec.md), [feature 002](../../features/002-autenticacion-y-control-de-acceso-rbac/spec.md), [API](../api.md) |

## 1. Resumen ejecutivo

UrbanPulse necesitaba autenticar a ciudadanía, operadores, técnicos y administración sin mantener sesión en el servidor y con control de acceso por rol en cada endpoint. Se adoptó JWT firmado con HMAC-SHA256 y contraseñas hasheadas con BCrypt, validadas por un filtro perimetral de Spring Security (`JwtAuthenticationFilter`) que puebla el `SecurityContext` con los roles del token. La razón principal es la escalabilidad horizontal y la autonomía operativa frente a un IdP externo. El riesgo conocido es la imposibilidad de revocar tokens antes de su caducidad, mitigada con una expiración acotada de 24 horas.

## 2. Contexto y problema

- **Situación actual verificable:** SPA React + API REST Spring Boot; `SecurityConfig` usa `SessionCreationPolicy.STATELESS`, filtro Bearer y manejadores propios 401/403, según [ADR-02](../ADR/ADR-02-autenticacion-jwt-bcrypt-rbac.md) y [docs/architecture.md](../architecture.md).
- **Problema:** las sesiones HTTP acoplarían estado al servidor y dificultarían el escalado; un IdP externo añadiría infraestructura no prevista para el alcance del proyecto.
- **Motivación:** las features [001](../../features/001-registro-e-inicio-de-sesion-de-ciudadanos/spec.md) y [002](../../features/002-autenticacion-y-control-de-acceso-rbac/spec.md) están implementadas y requieren una decisión trazable, actualmente ausente en sus `spec/plan/tasks`.

## 3. Objetivos, no objectives y alcance

**Objetivos**
- O1. Registro y login con credenciales hasheadas y token firmado.
- O2. Autorización por rol en endpoints y en rutas de la SPA.
- O3. Mantener la API sin estado de sesión.

**No objetivos**
- N1. Integrar OAuth2/OpenID Connect con Keycloak o Auth0.
- N2. Implementar revocación inmediata (blacklist/Redis) en esta iteración.
- N3. Cambiar el modelo de datos de roles (gobernado por ADR-01).

**Dentro del alcance:** `/api/auth/**`, `JwtAuthenticationFilter`, `SecurityConfig`, interceptor Axios, `localStorage`.
**Fuera del alcance:** gestión de sesiones en servidor, MFA, recuperación de contraseña.

## 4. Requisitos y drivers arquitectónicos

| ID | Tipo | Necesidad / escenario | Criterio de aceptación y evidencia | Prioridad |
|---|---|---|---|---|
| RF-1 | Funcional | `POST /api/auth/register` crea usuario `CITIZEN` y devuelve 201 | [Feature 001](../../features/001-registro-e-inicio-de-sesion-de-ciudadanos/spec.md) | Alta |
| RF-2 | Funcional | `POST /api/auth/login` devuelve token Bearer con rol y expiración | `AuthResponse{token, tokenType, expiresIn, role, fullName}` en feature 001 | Alta |
| RF-3 | Funcional | `CITIZEN` recibe 403 en operaciones de triaje; anónimo recibe 401 genérico | [Feature 002](../../features/002-autenticacion-y-control-de-acceso-rbac/spec.md) | Alta |
| RC-1 | Restricción | Spring Security 6 + JJWT 0.12.6 HMAC-SHA256, BCrypt | [tech-stack.md](../../constitution/tech-stack.md) | Alta |
| RC-2 | Restricción | `JWT_SECRET` de al menos 512 bits, nunca versionado | [docs/deployment.md](../deployment.md) | Alta |
| RQ-1 | Calidad | Escalabilidad horizontal sin estado en servidor | [ADR-02, razones](../ADR/ADR-02-autenticacion-jwt-bcrypt-rbac.md) | Alta |

## 5. Arquitectura actual y dependencias

- **C4 Contexto/Contenedores:** [modelo C4 vigente](../architecture.md#2-diagrama-de-contenedores-c4-nivel-2).
- **Flujo actual:** navegador → Axios (Bearer) → `JwtAuthenticationFilter` → `SecurityFilterChain` → controlador → servicio.
- **Dependencias y restricciones:** Spring Security 6, JJWT 0.12.6, BCrypt, variable `JWT_SECRET`, expiración `JWT_EXPIRATION=86400000` ms (24 h).

## 6. Alternativas consideradas

| Alternativa | Descripción | Ventajas | Inconvenientes / riesgos | Evidencia disponible |
|---|---|---|---|---|
| A | Sesiones HTTP stateful (HttpSession/Redis) | Revocación inmediata | Estado en servidor, coordinación de réplicas | [ADR-02](../ADR/ADR-02-autenticacion-jwt-bcrypt-rbac.md) |
| B | JWT + BCrypt + RBAC (elegida) | Stateless, sin infra extra, claims con rol | Sin revocación inmediata | [ADR-02, decisión](../ADR/ADR-02-autenticacion-jwt-bcrypt-rbac.md) |
| C | IdP externo (Keycloak/Auth0) | Gestión de identidad madura | Nueva infraestructura y acoplamiento operativo | [ADR-02](../ADR/ADR-02-autenticacion-jwt-bcrypt-rbac.md) |

**Criterios de comparación:** escalabilidad, complejidad operativa, seguridad de credenciales, autonomía.
**Elección propuesta y justificación:** B, aceptada en ADR-02; es la única que cumple RC-1/RQ-1 sin incorporar infraestructura nueva.

## 7. Diseño propuesto

### 7.1. Visión y diagramas C4

- **Contexto (L1):** sin cambios; actores CITIZEN/OPERATOR/TECHNICIAN/ADMIN/ANALYST en [C4 L1](../architecture.md#1-diagrama-de-contexto-c4-nivel-1).
- **Contenedores (L2):** SPA ↔ API REST con Bearer JWT; [C4 L2](../architecture.md#2-diagrama-de-contenedores-c4-nivel-2).
- **Componentes (L3):** `JwtService`, `JwtAuthenticationFilter`, `SecurityConfig`, `AuthService`; ver [ADR-02](../ADR/ADR-02-autenticacion-jwt-bcrypt-rbac.md).
- **Secuencias:** login → emisión de token → cada petición lleva `Authorization: Bearer <token>` → filtro valida → `SecurityContext` → decisión de acceso.

### 7.2. Interfaces y contratos

- `POST /api/auth/register` → 201 `{message, email}`; 400 validación; 409 email duplicado.
- `POST /api/auth/login` → 200 `AuthResponse`; 401 genérico para credenciales inválidas (anti-enumeración).
- Errores con `ErrorResponse` `{timestamp, status, error, message, path}`; detalle en [docs/api.md](../api.md).
- Rutas públicas: `/api/auth/**`, `/h2-console/**` (desarrollo); resto protegido.

### 7.3. Modelo y gestión de datos

Tabla `users`: `email` único normalizado a minúsculas, `password_hash` con BCrypt, `role` como cadena (`VARCHAR` + `CHECK`, ver [ADR-01](../ADR/ADR-01-persistencia-enums-jpa.md)). Sin tablas de sesión.

### 7.4. Fallos, concurrencia y resiliencia

Token ausente, malformado o expirado → 401; rol insuficiente → 403. No hay reintentos ni estado compartido. Riesgo ADR-02: token válido no revocable antes de 24 h.

### 7.5. Seguridad

HMAC-SHA256 con `JWT_SECRET` ≥ 512 bits fuera del repositorio; BCrypt con salt intrínseco; `Usuario.toString()` omite `passwordHash`; 401 genérico para no revelar si el email existe. Ver [tech-stack.md, límites duros](../../constitution/tech-stack.md).

### 7.6. Operación y despliegue

Compose con `JWT_SECRET` inyectado por entorno, `JWT_EXPIRATION=86400000`; healthchecks y rollback por tag en [docs/deployment.md](../deployment.md). No se definen SLOs numéricos de autenticación en el repositorio.

## 8. Decisiones y trade-offs

| Decisión propuesta | Beneficio | Coste / consecuencia | ¿Requiere ADR? |
|---|---|---|---|
| JWT stateless + BCrypt + RBAC | Escalabilidad y autonomía | Sin revocación inmediata | No — ADR-02 |
| `EnumType.STRING` para roles | Compatibilidad JPA/H2 | CHECK a sincronizar | No — ADR-01 |
| DTOs de auth en controladores | Sin fuga de `passwordHash` | Clases DTO adicionales | No — ADR-03 |

### 8.1. Decisiones arquitectónicas derivadas

- [ADR-02: Autenticación perimetral sin estado mediante JWT y BCrypt con RBAC](../ADR/ADR-02-autenticacion-jwt-bcrypt-rbac.md) — **aceptado**. Decisión de origen.
- [ADR-01](../ADR/ADR-01-persistencia-enums-jpa.md) y [ADR-03](../ADR/ADR-03-desacoplamiento-dtos-mappers.md) — decisiones de soporte ya aceptadas.

## 9. Plan de implementación y migración

1. Configurar `SecurityConfig` STATELESS y el filtro JWT antes de `UsernamePasswordAuthenticationFilter`.
2. Implementar registro/login con BCrypt y emisión de token.
3. Añadir guardias `ProtectedRoute`/`RoleGuard` e interceptor Axios que purga el token ante 401.
4. Verificar con `mvn test` y `npm run lint`; reversión: restaurar la configuración de seguridad anterior y limpiar `localStorage`.

**Dependencias:** ADR-01 y ADR-03. **Impacto en versiones anteriores:** ninguno; las credenciales semilla siguen siendo válidas.

## 10. Estrategia de verificación y evaluación

| Requisito | Prueba/experimento | Entorno y datos | Métrica / umbral | Evidencia esperada |
|---|---|---|---|---|
| RF-1/RF-2 | `AuthServiceTest`, `AuthControllerTest`, `JwtServiceTest` | Local, H2 | 0 fallos | [QA note 001](../001-registro-e-inicio-de-sesion-de-ciudadanos/qa-note.md) registra 59 tests |
| RF-3 | `JwtAuthenticationFilterTest`, `IncidentControllerSecurityTest` | Local, H2 | 0 fallos | [QA note 002](../002-autenticacion-y-control-de-acceso-rbac/qa-note.md) registra 116 tests |
| RC-2 | Revisión de `.env`/secretos en CI | Repositorio | 0 secretos versionados | `npm run lint`, revisión de PR |

No hay SLOs numéricos de latencia/disponibilidad; la validación es local y de CI, no requiere cloud real.

## 11. Riesgos, hipótesis y cuestiones abiertas

| ID | Tipo | Descripción | Impacto | Acción y responsable |
|---|---|---|---|---|
| R-1 | Riesgo | Token válido no revocable antes de 24 h | Medio | ADR-02 mitiga con expiración acotada; reevaluar si aparece requisito de logout global |
| R-2 | Riesgo | `localStorage` expone token a XSS | Medio | Mantener escape de React y revisión de dependencias — frontend |
| Q-1 | Pregunta | ¿Definir SLOs de latencia de login? | Bajo | Decisión pendiente del equipo |

## 12. Revisión y resultado

- **Comentarios de revisión:** pendientes.
- **Resolución:** pendiente de revisión técnica.
- **Decisiones registradas:** [ADR-02](../ADR/ADR-02-autenticacion-jwt-bcrypt-rbac.md).
- **Diferencias observadas tras implementar:** el 403 del feature 002 no purga el token (RF-10), comportamiento coherente con RBAC; no se ha implementado revocación.

## Checklist de aprobación

- [x] Problema y alcance delimitados.
- [x] Drivers y restricciones documentados, sin atribuir al enunciado requisitos inexistentes.
- [x] Al menos dos alternativas comparadas o justificación de por qué no hay alternativas viables.
- [x] Diagramas, datos y contratos coherentes.
- [x] Fallos, seguridad, operación y costes considerados en proporción al cambio.
- [x] Criterios de aceptación verificables y plan de pruebas reproducible.
- [x] Riesgos y cuestiones abiertas explícitos.
- [ ] Revisión técnica realizada; ADR enlazados cuando corresponde.
