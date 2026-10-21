# Tech stack y convenciones — UrbanPulse

_Cómo está construido el proyecto y las reglas que todo el código debe respetar. Es la referencia técnica que ningún plan de feature debería contradecir._

## Tecnologías

- **Backend:** Java (compatible con JDK 17 y JDK 21), Spring Boot 3.4.x, Apache Maven.
  - Paquete base: `org.bulbaseros`
  - Persistencia: Spring Data JPA / Hibernate.
  - Utilidad y boilerplate: Lombok (`@Data`, `@Getter`, `@Setter`, etc.) para entidades y DTOs.
  - Seguridad: Spring Security con JWT (`jjwt` 0.12.x) y contraseñas hasheadas con BCrypt.
  - Validación: Bean Validation (`spring-boot-starter-validation`).
  - CORS: Configuración de orígenes cruzados permitiendo clientes de desarrollo (`http://localhost:5173`).
- **Frontend:** React 19, Vite, Axios, React Router (`react-router-dom`), ESLint.
  - Enrutamiento: React Router para navegación SPA (`/`, `/login`, `/register`).
  - Almacenamiento de sesión: JWT en `localStorage` con interceptor de Axios para cabecera `Authorization: Bearer <token>` y captura de 401 para limpieza de sesión.
- **Base de datos:** PostgreSQL 16 (desplegado con Docker Compose en Alpine). Base de datos H2 en modo compatibilidad para tests locales y suite de integración.
- **Testing:**
  - Backend: JUnit 5 / Spring Boot Starter Test / Spring Security Test.
  - Frontend: Testing en cliente según estándares React/Vite.

## Archivos / módulos clave

- `Backend/` — Código fuente del servidor Java/Spring Boot y archivo `pom.xml`.
- `frontend/` — Código fuente de la aplicación React y configuración Vite.
- `database/init/diagramUrbanPulse.sql` — Schema inicial de PostgreSQL con tipos ENUM y tablas.
- `docker-compose.yml` — Orquestación del contenedor PostgreSQL.
- `constitution/` — Definición de directrices, principios y convenciones del proyecto.
- `features/` — Especificaciones, planes técnicos y listas de tareas por funcionalidad.

## Comandos

- `docker compose up -d` — Arranca la base de datos PostgreSQL local.
- `cd Backend && mvn clean test` — Ejecuta las pruebas del backend.
- `cd Backend && mvn spring-boot:run` — Inicia el servidor backend en desarrollo.
- `cd frontend && npm install` — Instala dependencias del cliente.
- `cd frontend && npm run dev` — Inicia el servidor de desarrollo de React con Vite.
- `cd frontend && npm run lint` — Valida el estilo y buenas prácticas de código JavaScript/React.

## Modelo de datos / dominio

- **`user_role` (ENUM):** `CITIZEN`, `OPERATOR`, `TECHNICIAN`, `ADMIN`, `ANALYST`.
- **`incident_status` (ENUM):** `REPORTED`, `VALIDATED`, `REJECTED`, `ASSIGNED`, `IN_PROGRESS`, `RESOLVED`, `REOPENED`, `CLOSED`.
- **`incident_priority` (ENUM):** `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`.
- **Tabla `users` (`Usuario`):** Almacena credenciales (`email` UNIQUE varchar(150), `password_hash` varchar(255)), `full_name` varchar(150), `role` y relación con `departments` (`department_id` opcional).
- **Tabla `departments` (`Departamento`):** Departamentos operativos del ayuntamiento (infraestructuras, medio ambiente, movilidad, etc.).
- **Tabla `incidents` (`Incidencia`):** Reportes geolocalizados vinculados a un ciudadano (`citizen_id`) con coordenadas, categoría y estado.
- **Tabla `urban_assets` (`ActivoUrbano`):** Mobiliario e infraestructura física urbana con geolocalización y código único.
- **Tabla `incident_asset_associations` (`AsociacionIncidenciaActivo`):** Relación entre incidencias y elementos de mobiliario afectados.
- **Tabla `status_changes` (`CambiosEstado`):** Registro inmutable de transiciones de estado para auditoría y trazabilidad.

## Convenciones

- **Convención de paquetes y persistencia:**
  - Las entidades de persistencia JPA se ubican en `org.bulbaseros.entity` con nombres de clase en PascalCase en español (`Usuario`, `Incidencia`, `Departamento`, `ActivoUrbano`, etc.).
  - Se unifica el modelo de dominio en torno a `org.bulbaseros.entity`, evitando duplicidades con paquetes auxiliares.
- **Convención de nombres:**
  - Java: PascalCase para clases (`AuthService`), camelCase para métodos y variables, UPPER_CASE para constantes.
  - Endpoints REST: Plurales o prefijos funcionales en minúsculas y kebab-case (p. ej. `/api/auth/register`, `/api/auth/login`).
  - Frontend: PascalCase para componentes React (`LoginForm.jsx`), camelCase para hooks y utilidades.
- **Documentación de código:**
  - Backend: Todo código (clases, métodos, interfaces) debe documentarse obligatoriamente con bloques Javadoc (`/** ... */`) detallando propósito, `@param`, `@return` y `@throws`.
  - Frontend: Documentar de forma análoga con JSDoc (`/** ... */`) en componentes, hooks, servicios y utilidades con `@param` y `@returns`.
- **Manejo de errores y respuestas:**
  - El backend responde con códigos HTTP estándar (400 Bad Request, 401 Unauthorized, 404 Not Found, 409 Conflict, 500 Internal Server Error).
  - Respuestas de error con estructura JSON consistente y mensajes descriptivos (sin filtrar detalles sensibles de seguridad en fallos de autenticación).
  - Estándar de tiempo: configuración interna de expiración en milisegundos y exposición en payloads REST en segundos (`expiresIn`).

## Límites duros

- **Seguridad de contraseñas:** NUNCA almacenar contraseñas en texto plano; siempre aplicar hashing seguro con BCrypt.
- **Credenciales en commits:** NUNCA commitear archivos `.env` ni credenciales privadas en el repositorio.
- **Control de dependencias:** No añadir nuevas dependencias críticas en `pom.xml` o `package.json` sin documentar el motivo.
- **Consistencia SDD:** Ningún código de funcionalidad se escribe sin tener previamente aprobada su especificación (`spec.md`), su plan (`plan.md`) y sus tareas (`tasks.md`).
