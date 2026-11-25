# DD-008 — Registro e inicio de sesión de ciudadanos

| Metadato | Valor |
|---|---|
| Estado | En revisión |
| Autoría | Equipo UrbanPulse (refactorización documental) |
| Fecha / última revisión | 2026-11-20 |
| Iteración | Refactorización documental: trazabilidad SDD ↔ ADR |
| Revisores | Equipo técnico UrbanPulse (pendiente de revisión) |
| Enlaces | [Feature 001](../../features/001-registro-e-inicio-de-sesion-de-ciudadanos/spec.md), [plan 001](../../features/001-registro-e-inicio-de-sesion-de-ciudadanos/plan.md), [QA note](../001-registro-e-inicio-de-sesion-de-ciudadanos/qa-note.md), [ADR-02](../ADR/ADR-02-autenticacion-jwt-bcrypt-rbac.md) |

## 1. Resumen ejecutivo

La feature 001 incorpora el alta de ciudadanos y el login que devuelve un JWT de 24 horas. Se conserva la decisión de ADR-02 (JWT stateless + BCrypt + RBAC), el mapeo de roles de ADR-01 y los DTOs de ADR-03. El diseño cubre validación de campos, email único, respuesta 401 genérica anti-enumeración y un interceptor Axios que adjunta el Bearer token y lo purga ante 401. El riesgo principal es el almacenamiento del token en `localStorage` y la ausencia de revocación inmediata.

## 2. Contexto y problema

- **Situación actual verificable:** `POST /api/auth/register` (201) y `POST /api/auth/login` (200 con `AuthResponse`), registro con rol `CITIZEN`, contraseña BCrypt y JWT HMAC-SHA256; ver [spec 001](../../features/001-registro-e-inicio-de-sesion-de-ciudadanos/spec.md).
- **Problema:** antes de la feature no existía identidad para ciudadanos ni token para consumir la API protegida.
- **Motivación:** RF01 del [caso de estudio](../DESCRIPCION.md). La feature está implementada y validada, pero sus documentos no citaban ningún ADR; este DD establece esa trazabilidad.

## 3. Objetivos, no objetivos y alcance

**Objetivos**
- O1. Registrar ciudadanos con validación y email normalizado/único.
- O2. Emitir un token firmado con rol e identidad del usuario.
- O3. Integrar el token en las peticiones de la SPA.

**No objetivos**
- N1. Registrar otros roles desde la interfaz pública (solo `CITIZEN`; los demás son semilla/administración).
- N2. Implementar recuperación de contraseña, MFA o verificación de email.
- N3. Redefinir la estrategia de autenticación (ADR-02).

**Dentro del alcance:** `/api/auth/**`, `AuthService`, `JwtService`, `AuthResponse`, interceptor Axios, guardias de ruta.
**Fuera del alcance:** administración de usuarios, RBAC de endpoints (DD-009), auditoría (DD-005).

## 4. Requisitos y drivers arquitectónicos

| ID | Tipo | Necesidad / escenario | Criterio de aceptación y evidencia | Prioridad |
|---|---|---|---|---|
| RF-1 | Funcional | Alta con `fullName`, `email`, contraseña | `fullName`/`email` ≤150; contraseña ≥8 con letra y dígito (spec 001) | Alta |
| RF-2 | Funcional | Email duplicado → 409; datos inválidos → 400 | [spec 001](../../features/001-registro-e-inicio-de-sesion-de-ciudadanos/spec.md) | Alta |
| RF-3 | Funcional | Login con credenciales erróneas → 401 genérico | Spec 001 | Alta |
| RF-4 | Funcional | Token con `expiresIn: 86400` s y `tokenType: Bearer` | `AuthResponse` en spec 001 | Alta |
| RC-1 | Restricción | JWT_SECRET ≥ 512 bits; BCrypt; estado stateless | [ADR-02](../ADR/ADR-02-autenticacion-jwt-bcrypt-rbac.md), [deployment.md](../deployment.md) | Alta |
| RC-2 | Restricción | Roles como VARCHAR + CHECK | [ADR-01](../ADR/ADR-01-persistencia-enums-jpa.md) | Alta |

## 5. Arquitectura actual y dependencias

- **C4 Contexto/Contenedores:** [C4 L2](../architecture.md#2-diagrama-de-contenedores-c4-nivel-2).
- **Flujo actual:** formulario → `/api/auth/register` o `/login` → servicio → repositorio → DTO → token en `localStorage` → interceptor Axios.
- **Dependencias y restricciones:** Spring Security 6, JJWT 0.12.6, BCrypt, Axios, React Router.

## 6. Alternativas consideradas

| Alternativa | Descripción | Ventajas | Inconvenientes / riesgos | Evidencia disponible |
|---|---|---|---|---|
| A | Sesiones stateful | Revocación sencilla | Estado en servidor; contradice ADR-02 | [ADR-02](../ADR/ADR-02-autenticacion-jwt-bcrypt-rbac.md) |
| B | JWT + BCrypt (elegida) | Stateless, estándar del proyecto | Token no revocable | [ADR-02, decisión](../ADR/ADR-02-autenticacion-jwt-bcrypt-rbac.md) |
| C | IdP externo | Identidad gestionada | Infra nueva fuera de alcance | [ADR-02](../ADR/ADR-02-autenticacion-jwt-bcrypt-rbac.md) |

**Criterios de comparación:** coherencia con ADR-02, simplicidad, seguridad de credenciales.
**Elección propuesta y justificación:** B; la feature materializa una decisión ya aceptada, no introduce una alternativa nueva.

## 7. Diseño propuesto

### 7.1. Visión y diagramas C4

- **Contexto (L1):** ciudadano → UrbanPulse; [C4 L1](../architecture.md#1-diagrama-de-contexto-c4-nivel-1).
- **Contenedores (L2):** SPA ↔ API REST ↔ PostgreSQL; [C4 L2](../architecture.md#2-diagrama-de-contenedores-c4-nivel-2).
- **Componentes (L3):** `AuthController`, `AuthService`, `JwtService`, interceptor Axios.
- **Secuencias:** registro → 201; login → validación BCrypt → token → redirección `/`.

### 7.2. Interfaces y contratos

- `POST /api/auth/register` → 201 `{message, email}`; 400/409 según caso.
- `POST /api/auth/login` → 200 `AuthResponse{token, tokenType:"Bearer", expiresIn:86400, role, fullName}`; 401 genérico.
- Errores con `ErrorResponse`; detalle en [docs/api.md](../api.md).

### 7.3. Modelo y gestión de datos

Tabla `users`: `full_name`, `email` único en minúsculas, `password_hash` BCrypt, `role` (`CITIZEN`) como cadena con `CHECK` (ADR-01). Sin tablas de sesión.

### 7.4. Fallos, concurrencia y resiliencia

409 si email duplicado; 400 si validación; 401 si credenciales incorrectas. El interceptor elimina el token ante 401 y redirige a `/login`. No hay reintentos de login.

### 7.5. Seguridad

Contraseña nunca en claro; `passwordHash` no se serializa (ADR-03); 401 genérico para no revelar existencia del email; secreto JWT ≥ 512 bits fuera del repo.

### 7.6. Operación y despliegue

Cuentas semilla con roles ADMIN/OPERATOR/CITIZEN en [README.md](../../README.md). Mismo despliegue Compose del resto del sistema; no hay SLOs numéricos de autenticación.

## 8. Decisiones y trade-offs

| Decisión propuesta | Beneficio | Coste / consecuencia | ¿Requiere ADR? |
|---|---|---|---|
| Registro público solo como CITIZEN | Menor superficie de ataque | Altas internas por otro canal | No — spec 001 |
| Token en `localStorage` | Persistencia tras recarga | Exposición a XSS | No — ADR-02 |
| DTOs de auth | Sin fuga de `passwordHash` | Clases adicionales | No — ADR-03 |

### 8.1. Decisiones arquitectónicas derivadas

- [ADR-02: JWT + BCrypt + RBAC](../ADR/ADR-02-autenticacion-jwt-bcrypt-rbac.md) — **aceptado**. Decisión rectora de la feature.
- [ADR-01](../ADR/ADR-01-persistencia-enums-jpa.md) — persistencia del rol.
- [ADR-03](../ADR/ADR-03-desacoplamiento-dtos-mappers.md) — DTOs de petición/respuesta.

## 9. Plan de implementación y migración

1. Entidad `Usuario` + repositorio con email único y rol `CITIZEN`.
2. `AuthService`/`AuthController` con BCrypt y emisión JWT.
3. Formularios React, guardias e interceptor Axios.
4. Verificar con `mvn test`, `npm run lint` y `vite build`; reversión: desactivar rutas `/register` y `/login`.

**Dependencias:** ADR-01, ADR-02, ADR-03. **Impacto en versiones anteriores:** ninguno.

## 10. Estrategia de verificación y evaluación

| Requisito | Prueba/experimento | Entorno y datos | Métrica / umbral | Evidencia esperada |
|---|---|---|---|---|
| RF-1/RF-2 | `AuthServiceTest`, `AuthControllerTest`, `ValidationDtoTest` | Local, H2 | 0 fallos | [QA note 001](../001-registro-e-inicio-de-sesion-de-ciudadanos/qa-note.md): 59 tests |
| RF-3/RF-4 | `JwtServiceTest`, `GlobalExceptionHandlerTest` | Local | Token válido y 401 genérico | QA note 001 |
| RC-1 | Revisión de secretos | Repositorio/CI | 0 secretos versionados | Revisión de PR |

No hay umbrales numéricos de cobertura para esta feature ni SLOs de latencia; build frontend registrado en 258 ms.

## 11. Riesgos, hipótesis y cuestiones abiertas

| ID | Tipo | Descripción | Impacto | Acción y responsable |
|---|---|---|---|---|
| R-1 | Riesgo | XSS puede leer `localStorage` | Medio | Escaping de React y revisión de dependencias |
| R-2 | Riesgo | Caducidad de 24 h sin revocación | Medio | Aceptado en ADR-02 |
| Q-1 | Pregunta | ¿Definir umbral de cobertura para autenticación? | Bajo | Decisión pendiente del equipo |

## 12. Revisión y resultado

- **Comentarios de revisión:** pendientes.
- **Resolución:** pendiente de revisión técnica.
- **Decisiones registradas:** [ADR-02](../ADR/ADR-02-autenticacion-jwt-bcrypt-rbac.md), [ADR-01](../ADR/ADR-01-persistencia-enums-jpa.md), [ADR-03](../ADR/ADR-03-desacoplamiento-dtos-mappers.md).
- **Diferencias observadas tras implementar:** el token se guarda en `localStorage` y se purga ante 401; detalle no recogido en el ADR pero coherente con él.

## Checklist de aprobación

- [x] Problema y alcance delimitados.
- [x] Drivers y restricciones documentados, sin atribuir al enunciado requisitos inexistentes.
- [x] Al menos dos alternativas comparadas o justificación de por qué no hay alternativas viables.
- [x] Diagramas, datos y contratos coherentes.
- [x] Fallos, seguridad, operación y costes considerados en proporción al cambio.
- [x] Criterios de aceptación verificables y plan de pruebas reproducible.
- [x] Riesgos y cuestiones abiertas explícitos.
- [ ] Revisión técnica realizada; ADR enlazados cuando corresponde.
