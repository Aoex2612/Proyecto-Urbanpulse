# Tech stack y convenciones

_Cómo está construido el proyecto y las reglas que todo el código debe respetar. Es la referencia técnica que ningún plan de feature debería contradecir._

## Tecnologías

- **Lenguaje:** Java 21 (Backend) / JavaScript ES2022+ (Frontend).
- **Framework / runtime:** Spring Boot 3.4.3 (Java) / Node.js 20+ con Vite 6 (React 19).
- **Seguridad:** Spring Security 6 con autenticación perimetral sin estado (Stateless) mediante JWT (JJWT 0.12.6, algoritmo HMAC-SHA256) y codificación de contraseñas con BCrypt.
- **Base de datos:** PostgreSQL 16 (Alpine) orquestada con Docker Compose para desarrollo/producción local; H2 Database en modo compatibilidad PostgreSQL para ejecución ágil de pruebas unitarias y de integración.
- **Persistencia:** Spring Data JPA con Hibernate 6. Las enumeraciones se persisten como cadenas (`@Enumerated(EnumType.STRING)`) emparejadas con restricciones `CHECK` en el DDL (ADR-01).
- **Tests:** JUnit 5, Mockito y Spring Security Test para backend; Vitest/Testing Library para frontend.
- **Cliente HTTP:** Axios 1.x con interceptores para inyección automática de cabecera `Authorization: Bearer <token>` y captura de expiración de sesión (401).
- **Cartografía / Mapas:** Leaflet 1.9.x y React-Leaflet con teselas abiertas OpenStreetMap para visualización interactiva de incidencias y captura geolocalizada de coordenadas en frontend.

## Archivos / módulos clave

- `Backend/` — Servidor de API REST desacoplado, lógica de negocio y persistencia en Spring Boot.
  - `src/main/java/org/bulbaseros/config/` — Configuración de seguridad perimetral, CORS y beans de infraestructura.
  - `src/main/java/org/bulbaseros/controller/` — Controladores REST con endpoints públicos y protegidos con RBAC.
  - `src/main/java/org/bulbaseros/entity/` — Entidades JPA que mapean el modelo de dominio relacional.
  - `src/main/java/org/bulbaseros/security/` — Filtro JWT (`JwtAuthenticationFilter`), servicio criptográfico y manejadores de error (401/403).
  - `src/main/java/org/bulbaseros/service/` — Servicios de lógica de negocio (registro con hash BCrypt, login, emisión de tokens).
- `frontend/` — Aplicación web cliente SPA con React 19 y enrutamiento con React Router 7.
  - `src/components/` — Componentes transversales de protección de rutas (`ProtectedRoute`, `RoleGuard`), visualización cartográfica con Leaflet y trazabilidad (`Timeline`).
  - `src/pages/` — Vistas de la aplicación (`LoginPage`, `RegisterPage`, `HomePage`, `OperatorPanelPage`, `MapPage`).
  - `src/services/` — Cliente Axios (`api.js`) y capa de servicio de autenticación y sesión (`authService.js`).
- `database/` — Infraestructura de base de datos relacional.
  - `init/diagramUrbanPulse.sql` — Esquema DDL completo, claves foráneas, índices, trigger de auditoría y datos iniciales (seed).
  - `README.md` — Parámetros de conexión y comandos de orquestación.
- `docker-compose.yml` — Definición del servicio de base de datos PostgreSQL con volumen persistente y healthcheck.
- `docs/ADR/` — Registro formal de Decisiones de Arquitectura (ADRs).

## Comandos

- `docker compose up -d` — Levanta la base de datos PostgreSQL local en segundo plano.
- `cd Backend && mvn spring-boot:run` — Arranca el backend en desarrollo (`http://localhost:8080`).
- `cd Backend && mvn clean test` — Ejecuta la suite completa de pruebas unitarias y de integración del backend.
- `cd frontend && npm run dev` — Inicia el servidor de desarrollo del cliente web (`http://localhost:5173`).
- `cd frontend && npm run lint` — Valida el estilo y buenas prácticas de código con ESLint.
- `cd frontend && npm run build` — Compila y empaqueta el frontend para producción.

## Modelo de datos / dominio

- `users` — Identidad y acceso. Almacena `password_hash` con BCrypt y el rol asignado (`CITIZEN`, `OPERATOR`, `TECHNICIAN`, `ADMIN`, `ANALYST`) mediante `VARCHAR(30)` y restricción `CHECK`.
- `departments` — Catálogo de áreas operativas municipales para enrutamiento y asignación de trabajos.
- `incidents` — Entidad central del dominio geolocalizada con latitud y longitud decimales. Mantiene `status` (`REPORTED`, `VALIDATED`, etc.) y `priority` con restricciones `CHECK` y marcas temporales `created_at` y `updated_at`.
- `assignments` — Vinculación temporal de incidencias a departamentos y técnicos responsables.
- `status_changes` — Histórico inmutable para auditoría y trazabilidad completa del ciclo de vida de cada aviso.
- `attachments` — Metadatos de evidencias fotográficas o documentales almacenadas fuera de la base de datos relacional.

## Convenciones

- **Código:** Clases en `PascalCase`, métodos y variables en `camelCase`, constantes y valores enum en `UPPER_CASE`.
- **Documentación de código:** Obligatorio comentar con **Javadoc** en clases, métodos públicos, parámetros (`@param`), retornos (`@return`) y excepciones (`@throws`) en backend; y **JSDoc** equivalente en frontend.
- **Git y ramas:** Flujo partiendo desde `develop` con formato `<tipo>/<descripcion-kebab-case>` (ej. `feature/...`, `fix/...`, `docs/...`).
- **Commits:** Obligatorio seguir el estándar **Conventional Commits** redactado en **español** (`feat: ...`, `fix: ...`, `docs: ...`, `test: ...`, `refactor: ...`, `chore: ...`). Sustituir siempre `ñ` por `n` y prescindir de tildes en ramas y commits.
- **Manejo de errores:** Códigos HTTP semánticos (200, 201, 400, 401, 403, 404, 409, 500) con respuestas JSON estructuradas (`ErrorResponse`). Las denegaciones de login responden con 401 genérico para mitigar enumeración de usuarios.

## Límites duros

- **Seguridad:** Nunca versionar ficheros `.env` con secretos reales ni registrar contraseñas en texto plano en logs (`Usuario.toString()` omite deliberadamente `passwordHash`).
- **Persistencia:** No utilizar tipos específicos de base de datos no soportados por el estándar JPA sin un ADR que lo justifique expresamente.
- **Desacoplamiento:** El frontend jamás accede directamente a la base de datos; toda interacción se realiza a través de la API REST protegida por JWT.
- **Flujo SDD:** Ninguna funcionalidad se implementa sin su correspondiente especificación (`spec.md`) aprobada y desglosada en tareas.
