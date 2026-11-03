# Spec 002 — Autenticación y Control de Acceso por Roles (RBAC)
**Estado:** implementado ✅

## Contexto y objetivo
Garantizar la protección perimetral e interna de UrbanPulse mediante el control de acceso basado en roles (RBAC). El backend debe validar en cada petición HTTP a recursos protegidos la autenticidad e integridad del token JWT e impedir accesos no autorizados a operaciones sensibles (como el triaje y cambio de estado de incidencias). En el frontend, la aplicación debe salvaguardar las rutas de acceso restringido impidiendo la navegación anónima o con rol insuficiente, redirigiendo adecuadamente e informando al usuario sin comprometer la seguridad ni cerrar innecesariamente su sesión.

## Usuarios / actores
- **Usuario Anónimo:** Persona sin sesión iniciada ni token válido que intenta consultar recursos protegidos.
- **Ciudadano (`CITIZEN`):** Usuario registrado que puede reportar y consultar sus incidencias, pero carece de permisos administrativos u operativos.
- **Operador (`OPERATOR`):** Personal municipal encargado de validar, rechazar, clasificar, priorizar y gestionar el triaje de incidencias.
- **Administrador (`ADMIN`):** Usuario con permisos totales de gestión, configuración, triaje y supervisión del sistema.

## Historias de usuario
- **H1 (Backend RBAC & JWT):** Como sistema quiero validar el token JWT y los roles en cada petición entrante a rutas protegidas para responder con HTTP 401 si el token no es válido o falta, y con HTTP 403 Forbidden si el rol del usuario no tiene permisos suficientes para el endpoint solicitado.
- **H2 (Frontend ProtectedRoute & RoleGuard):** Como usuario de la plataforma quiero que el sistema me guíe y restrinja el acceso según mi estado de autenticación y rol, redirigiendo a `/login` si soy anónimo o a `/` con un mensaje visual de alerta si no cuento con el rol requerido, sin cerrar mi sesión activa.

## Requisitos funcionales (criterios de aceptación en EARS)

### Filtro y Autorización Backend (H1)
- **RF-1:** CUANDO una petición HTTP arriba a un endpoint protegido sin cabecera `Authorization` o sin un formato válido `Bearer <token>` (admitiendo insensibilidad a mayúsculas/minúsculas en el prefijo `bearer`), EL SISTEMA denegará el acceso retornando código HTTP 401 (Unauthorized) con un cuerpo JSON normalizado `ErrorResponse` conteniendo `status: 401`, `error: "Unauthorized"`, `message: "Token de autenticación ausente o inválido"`, `timestamp` y `path`.
- **RF-2:** SI el token JWT provisto en la cabecera `Authorization` ha expirado, está malformado o su firma criptográfica no es válida en una ruta protegida, ENTONCES EL SISTEMA denegará el acceso retornando código HTTP 401 (Unauthorized) con un cuerpo JSON normalizado `ErrorResponse` conteniendo `status: 401`, `error: "Unauthorized"`, `message: "Token de autenticación expirado o inválido"`, `timestamp` y `path`.
- **RF-3:** CUANDO una petición arriba a una ruta pública permitida (como `/api/auth/**` o `/h2-console/**`), EL SISTEMA ignorará la ausencia o invalidez del token JWT permitiendo la tramitación normal del endpoint público sin emitir error 401.
- **RF-4:** CUANDO el token JWT provisto en un endpoint protegido es válido, EL SISTEMA extraerá la identidad del usuario (`sub`) y su rol (`role`), inyectando la autenticación con la autoridad correspondiente (`ROLE_<ROL>`) en el contexto de seguridad de la petición (`SecurityContextHolder`).
- **RF-5:** CUANDO un usuario autenticado con rol `CITIZEN` (u otro rol sin permisos de triaje) invoca el endpoint de cambio de estado de incidencias (`PATCH /api/incidents/{id}/status`), EL SISTEMA denegará la operación respondiendo con código HTTP 403 (Forbidden) y cuerpo JSON normalizado `ErrorResponse` conteniendo `status: 403`, `error: "Forbidden"`, `message: "Acceso denegado: permisos insuficientes"`, `timestamp` y `path`.
- **RF-6:** CUANDO un usuario autenticado con rol `OPERATOR` o `ADMIN` invoca el endpoint de gestión operativa de incidencias (`PATCH /api/incidents/{id}/status`), EL SISTEMA autorizará la ejecución de la petición retornando respuesta HTTP exitosa (HTTP 200).

### Protección de Rutas y Navegación Frontend (H2)
- **RF-7:** SI un usuario anónimo (sin sesión activa ni token) intenta navegar a una ruta protegida (como `/operator-panel`), ENTONCES EL SISTEMA lo redirigirá a la pantalla de `/login`.
- **RF-8:** SI un usuario autenticado intenta acceder a una ruta protegida cuyos roles autorizados excluyen su rol (por ejemplo, un `CITIZEN` accediendo a `/operator-panel`), ENTONCES EL SISTEMA lo redirigirá a la vista principal (`/`) transmitiendo el motivo del bloqueo vía estado de navegación (`location.state = { accessDenied: true, message: "Acceso denegado: no dispones de permisos de operador o administrador" }`).
- **RF-9:** CUANDO la vista principal (`/`) detecta en su estado de navegación una alerta de acceso denegado, EL SISTEMA renderizará un mensaje o banner visual destacando la advertencia al usuario.
- **RF-10:** MIENTRAS el usuario experimente una denegación de acceso por rol no autorizado en frontend o backend (HTTP 403), EL SISTEMA mantendrá la sesión del usuario intacta en `localStorage` sin ejecutar `logout` ni eliminar el token.
- **RF-11:** CUANDO un usuario autenticado con rol `OPERATOR` o `ADMIN` accede a la ruta `/operator-panel`, EL SISTEMA renderizará la vista del panel de gestión operativa.
- **RF-12:** EL SISTEMA modulará el control de acceso en frontend mediante la composición de componentes (`ProtectedRoute` para autenticación y `RoleGuard` para verificación de roles específicos).

## Requisitos no funcionales
- **Seguridad perimetral:** Inspección sin estado (stateless) en backend mediante filtro servlet antes del filtro de autorización de Spring Security (`UsernamePasswordAuthenticationFilter`).
- **Estandarización de errores:** Todas las respuestas de error de autenticación y autorización (401 y 403) deben ceñirse fielmente a la estructura del DTO `ErrorResponse` (`timestamp`, `status`, `error`, `message`, `path`).
- **Resiliencia y UX en Frontend:** Mensajes visuales no bloqueantes ante accesos denegados sin interrumpir la sesión legítima del usuario.

## Casos límite
- Formato de cabecera `Authorization`: se admiten variantes en mayúsculas/minúsculas (`Bearer`, `bearer`) y espacios múltiples entre el prefijo y el token (`Bearer    <token>`).
- Token JWT manipulado o firmado con clave secreta errónea: capturado por el filtro y transformado en HTTP 401 controlado sin propagar trazas 500 no capturadas.
- Token válido pero sin claim `role` o con rol inexistente: el usuario se autentica sin autoridades válidas, resultando en HTTP 403 ante cualquier endpoint que exija rol.
- Token expirado en rutas públicas: no bloquea ni afecta el login o registro.
- Usuario manipulando manualmente el objeto `user` en `localStorage` para eludir `RoleGuard`: el frontend mostraría la vista, pero cualquier interacción con los endpoints del backend responderá con HTTP 403 estricto al ser validado contra el token firmado en servidor.

## Fuera de alcance
- Persistencia completa de la máquina de estados de incidencias en base de datos (se provee el endpoint representativo `PATCH /api/incidents/{id}/status` para verificar la seguridad de triaje).
- Gestión administrativa de usuarios (crear/modificar roles de usuarios desde interfaz gráfica).
- Asignación dinámica de permisos granulares por usuario (se utiliza RBAC estándar según `RolUsuario`).

## Criterios de aceptación
- [x] Filtro backend (`JwtAuthenticationFilter`) implementado e integrado en `SecurityConfig`, extrayendo rol y registrando las autoridades en `SecurityContextHolder`.
- [x] Peticiones sin token o con token corrupto/expirado a endpoints protegidos responden HTTP 401 con DTO `ErrorResponse` en JSON.
- [x] Rutas públicas (`/api/auth/**`, `/h2-console/**`) permanecen accesibles sin token o con token inválido.
- [x] Endpoint `PATCH /api/incidents/{id}/status` protegido exclusivamente para roles `OPERATOR` y `ADMIN`.
- [x] Peticiones de usuarios con rol `CITIZEN` a `PATCH /api/incidents/{id}/status` son rechazadas con HTTP 403 Forbidden y DTO `ErrorResponse`.
- [x] Componente `RoleGuard` implementado y compuesto con `ProtectedRoute` en el Frontend.
- [x] Navegación de usuario anónimo a `/operator-panel` redirige a `/login`.
- [x] Navegación de usuario con rol `CITIZEN` a `/operator-panel` redirige a `/` mostrando alerta visual sin cerrar sesión.
- [x] Navegación de usuario con rol `OPERATOR` o `ADMIN` a `/operator-panel` renderiza correctamente el panel.
- [x] Pruebas unitarias y de integración de backend y frontend ejecutadas y en verde.

## Dudas abiertas
- Ninguna. Requisitos, clarificaciones y casos límite formalizados.
