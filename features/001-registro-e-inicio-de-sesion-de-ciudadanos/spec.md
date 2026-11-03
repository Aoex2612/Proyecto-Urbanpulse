# Spec 001 — Registro e Inicio de Sesión de Ciudadanos
**Estado:** implementado ✅

## Contexto y objetivo
Permitir a los ciudadanos registrarse en la plataforma UrbanPulse y autenticarse mediante correo electrónico y contraseña. Al iniciar sesión, el sistema proveerá credenciales de acceso seguras (token JWT) para que el ciudadano pueda interactuar con el resto de servicios de la plataforma, manteniendo su identidad persistida y protegida.

## Usuarios / actores
- **Ciudadano no registrado:** Usuario nuevo que desea crear una cuenta para reportar o consultar incidencias.
- **Ciudadano registrado:** Usuario con cuenta existente que desea identificarse para acceder a las funciones protegidas de la plataforma.

## Historias de usuario
- **H1:** Como ciudadano quiero registrarme con mi nombre completo, correo electrónico y contraseña para tener una cuenta personal en UrbanPulse con rol de ciudadano.
- **H2:** Como ciudadano registrado quiero iniciar sesión con mi correo electrónico y contraseña para acceder de forma autenticada al sistema y gestionar mis reportes.

## Requisitos funcionales (criterios de aceptación en EARS)

### Registro de Ciudadano (H1)
- **RF-1:** CUANDO el usuario envía una solicitud de registro con datos válidos (`fullName` y `email` de máximo 150 caracteres, y `password` de al menos 8 caracteres con letra y dígito) y el correo no existe previamente, EL SISTEMA creará un nuevo registro de usuario con rol `CITIZEN`, contraseña cifrada con BCrypt y responderá con código HTTP 201 y cuerpo JSON `{ "message": "Usuario registrado exitosamente", "email": "<email>" }`.
- **RF-2:** SI el correo electrónico proporcionado ya se encuentra registrado en el sistema, ENTONCES EL SISTEMA denegará el registro sin duplicar datos y responderá con código HTTP 409 (Conflicto) y mensaje descriptivo de error.
- **RF-3:** SI alguno de los campos obligatorios está ausente, `fullName` o `email` exceden 150 caracteres, el correo no cumple el formato estándar (`nombre@dominio.tld`) o la contraseña no contiene al menos 8 caracteres con al menos una letra y un número, ENTONCES EL SISTEMA responderá con código HTTP 400 (Bad Request).
- **RF-4:** CUANDO el usuario interactúa con la interfaz de registro (`/register`), EL SISTEMA validará en cliente los campos obligatorios, longitud máxima de 150 caracteres, formato de email y requisitos de contraseña antes de emitir la petición.
- **RF-5:** CUANDO el registro en la interfaz (`/register`) finaliza con éxito (HTTP 201), EL SISTEMA mostrará confirmación al usuario y lo redirigirá a la pantalla de `/login`.

### Inicio de Sesión y Autenticación (H2)
- **RF-6:** CUANDO el usuario envía credenciales válidas (`email` y `password`), EL SISTEMA validará la contraseña contra el hash almacenado y entregará un token JWT con vigencia de 24 horas y claims mínimos (`sub` con el identificador/email del usuario y `role` con el rol del usuario).
- **RF-7:** SI el usuario introduce un correo no registrado o una contraseña incorrecta, ENTONCES EL SISTEMA devolverá un error genérico HTTP 401 (Unauthorized) sin revelar si el fallo proviene del correo o de la contraseña.
- **RF-8:** CUANDO el inicio de sesión es exitoso en la pantalla de `/login`, EL SISTEMA almacenará el token JWT en `localStorage` y redirigirá al usuario a la ruta por defecto `/`.
- **RF-9:** EL SISTEMA inyectará de forma automática la cabecera `Authorization: Bearer <token>` mediante un interceptor de Axios en todas las peticiones salientes posteriores.
- **RF-10:** SI una petición saliente protegida recibe un error HTTP 401 (por token expirado, malformado o manipulado), ENTONCES EL SISTEMA eliminará el token de `localStorage` y redirigirá al usuario a `/login`.
- **RF-11:** MIENTRAS el usuario mantenga una sesión autenticada con token válido, SI intenta acceder a las rutas `/login` o `/register`, ENTONCES EL SISTEMA lo redirigirá a `/`.

## Requisitos no funcionales
- **Seguridad:** Las contraseñas nunca deben almacenarse ni registrarse en texto plano; deben cifrarse con BCrypt. No se permite descarte de espacios por trim en contraseñas para respetar la cadena elegida por el usuario.
- **Formato y vigencia de Token:** Tokens según estándar RFC 7519 (JWT), firmados con secreto del servidor y con expiración de 24 horas (86.400 segundos).
- **Validación y UX:** Validación reactiva en cliente para prevenir llamadas innecesarias a la API y notificaciones claras de error.

## Casos límite
- Formato de correo electrónico: se ignoran espacios en blanco laterales y se normaliza a minúsculas antes de la validación y persistencia.
- Longitud máxima en BD: `fullName` y `email` restringidos a 150 caracteres; cualquier longitud superior es rechazada en validación con HTTP 400.
- Concurrencia de registros simultáneos: la base de datos aplica la restricción UNIQUE en `users.email` para prevenir condiciones de carrera.
- Token JWT malformado o manipulado: capturado como 401 por el backend y purgado en cliente.

## Fuera de alcance
- Recuperación de contraseña por correo electrónico.
- Confirmación de cuenta mediante enlace de activación.
- Autenticación OAuth2 / proveedores externos (Google, GitHub, etc.).
- Registro público de roles administrativos u operativos (`OPERATOR`, `TECHNICIAN`, `ADMIN`, `ANALYST`).

## Criterios de aceptación
- [x] Endpoint `POST /api/auth/register` operativo, guarda en base de datos con rol `CITIZEN` y contraseña hasheada en BCrypt, devolviendo HTTP 201 con mensaje JSON.
- [x] Validación de email duplicado devolviendo HTTP 409 y evitando registros redundantes.
- [x] Validación de campos vacíos, longitud máxima de 150 caracteres y formato de contraseña devolviendo HTTP 400.
- [x] Endpoint `POST /api/auth/login` operativo, validando credenciales y entregando token JWT válido por 24 horas con claims `sub` y `role`.
- [x] Error HTTP 401 genérico devuelto ante credenciales erróneas o usuario inexistente.
- [x] Pantallas `/login` y `/register` funcionales en React con validación en cliente.
- [x] Redirección automática a `/login` tras registro exitoso y a `/` tras login exitoso.
- [x] Redirección a `/` si un usuario ya autenticado visita `/login` o `/register`.
- [x] Token JWT guardado en `localStorage` e interceptor de Axios configurado para inyectar `Authorization: Bearer <token>` y purgar sesión con redirección a `/login` ante HTTP 401.
- [x] Suite de pruebas unitarias y de integración en backend cubriendo registro y login (casos de éxito y error).

## Dudas abiertas
- Ninguna. Todos los requisitos y casos límite han sido acordados.
