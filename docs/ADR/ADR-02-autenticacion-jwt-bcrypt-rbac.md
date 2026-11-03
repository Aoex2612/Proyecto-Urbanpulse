# ADR-02: Autenticación perimetral sin estado mediante JWT y BCrypt con RBAC

* Estado: aceptado
* Fecha: 2026-10-24

## Contexto
UrbanPulse es una plataforma web desacoplada compuesta por un frontend SPA (React/Vite) y un backend de API REST (Spring Boot). Para habilitar el acceso seguro de ciudadanos, operarios y administradores municipales, el sistema requería un mecanismo de autenticación y autorización que permitiese:
1. Validar la identidad de los usuarios de forma segura y proteger las contraseñas en almacenamiento.
2. Permitir el consumo de la API REST desde clientes web modernos sin acoplar estado en sesión HTTP en el servidor (garantizando escalabilidad horizontal).
3. Gestionar permisos y control de acceso granular basado en roles (`CITIZEN`, `OPERATOR`, `ADMIN`, etc.) en cada endpoint.

## Opciones consideradas
* **Opción 1: Sesiones HTTP tradicionales basadas en cookies de sesión (Stateful):** Almacenar la sesión en la memoria de Spring Boot (HttpSession) o clúster de sesiones (Redis).
* **Opción 2: Autenticación sin estado (Stateless) mediante JSON Web Tokens (JWT) y hashing BCrypt:** Emisión de un token firmado criptográficamente tras el login y verificación perimetral mediante un filtro de seguridad en cada petición.
* **Opción 3: Servidor de identidad delegado externo (OAuth2 / OpenID Connect con Keycloak o Auth0):** Delegar la gestión de identidades y emisión de tokens a un IdP externo.

## Decisión tomada
Optamos por la **Opción 2: Autenticación sin estado mediante JWT (JJWT con algoritmo HMAC-SHA256) y BCrypt para hash de contraseñas**, complementada con un filtro de seguridad Spring Security (`JwtAuthenticationFilter`) y control de acceso basado en roles (RBAC).

### Razón / Por qué se tomó la decisión
* **Desacoplamiento y arquitectura REST stateless:** El servidor no necesita guardar estado de sesión en memoria ni coordinar réplicas, lo que maximiza el rendimiento y la escalabilidad de la API.
* **Seguridad de credenciales robusta:** BCrypt incluye salt aleatorio intrínseco y coste computacional configurable, mitigando ataques de fuerza bruta y rainbow tables sobre `password_hash`.
* **Soporte RBAC nativo:** Los claims del token transportan el rol y la identidad del usuario, permitiendo a Spring Security poblar el `SecurityContext` y aplicar anotaciones como `@PreAuthorize` o reglas perimetrales en `SecurityFilterChain` sin sobrecargar la base de datos en cada consulta.
* **Autonomía y menor complejidad operacional:** No introduce dependencias de software complejas adicionales (como desplegar y mantener Keycloak) para las necesidades actuales de la plataforma.

## Consecuencias
* **Positivo:** Escalabilidad total del servidor de API, control estricto de accesos por rol (RBAC), interoperabilidad con interceptores de Axios en el frontend y compatibilidad transparente en pruebas unitarias y de integración.
* **Negativo:** Los tokens JWT válidos no se pueden invalidar de forma inmediata en el servidor sin implementar una lista de revocación (blacklist o Redis); se mitiga definiendo un tiempo de expiración acotado.
* **Neutral:** El frontend asume la responsabilidad de almacenar el token de forma segura (localStorage/memoria) e inyectarlo en cada cabecera `Authorization: Bearer <token>`.
