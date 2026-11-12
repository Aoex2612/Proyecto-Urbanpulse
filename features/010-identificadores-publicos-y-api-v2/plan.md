# 010 — Identificadores Publicos y API v2 — Plan

_Plan de implementacion para la resolucion de la Issue #59. Respeta la `constitution/` y se sustenta en [ADR-10](../../docs/ADR/ADR-10-identificadores-publicos-uuid-y-versionado-api-v2.md)._

## Enfoque

El objetivo es resolver la vulnerabilidad de enumeracion y exposicion de datos internos (IDOR) sustituyendo el identificador autonumerico de base de datos por un identificador unico universal publico (`UUID v4`) en la exposicion hacia el exterior. 
Se implementa un enfoque de doble versionado en la API REST:
1. **API v1 (`/api/v1/...` y `/api/...` como alias retrocompatible):** Mantiene la firma numerica original (`Long id`) para garantizar compatibilidad con consumidores heredados o tests existentes.
2. **API v2 (`/api/v2/...`):** Nueva interfaz REST donde los identificadores de recursos en path variables y query parameters son `UUID` (o cadenas UUID validas) y donde los DTOs de respuesta transforman el campo `id` en el valor publico (`UUID.toString()`), blindando por completo las claves primarias internas de la base de datos.
3. **Frontend:** Actualizacion de los servicios cliente Axios (`incidentService.js`, `triageService.js`, `notificationService.js`, `analyticsService.js`, `authService.js`), rutas de navegacion y pruebas unitarias de Vitest para consumir la API v2 y operar sin fricciones con identificadores UUID.

---

## Implementacion

### Fase 1: Base de Datos y Persistencia
1. **Actualizar script de inicializacion SQL:**
   - Modificar [`database/init/diagramUrbanPulse.sql`](file:///home/joalts8/Ingenieria/Cc/Proyecto-Urbanpulse/database/init/diagramUrbanPulse.sql) para asegurar la extension `pgcrypto` y anadir la columna `public_id UUID UNIQUE NOT NULL DEFAULT gen_random_uuid()` a todas las tablas principales: `departments`, `users`, `incidents`, `attachments`, `status_changes`, `assignments`, `incident_comments`, `urban_assets`, `external_observations` y `notifications`.
   - Anadir indices unicos correspondientes para busquedas de alto rendimiento: `CREATE UNIQUE INDEX idx_<table>_public_id ON <table> (public_id);`.
2. **Entidades JPA:**
   - Actualizar las clases en `Backend/src/main/java/org/bulbaseros/entity/`:
     - [`Incidencia.java`](file:///home/joalts8/Ingenieria/Cc/Proyecto-Urbanpulse/Backend/src/main/java/org/bulbaseros/entity/Incidencia.java)
     - [`Usuario.java`](file:///home/joalts8/Ingenieria/Cc/Proyecto-Urbanpulse/Backend/src/main/java/org/bulbaseros/entity/Usuario.java)
     - [`Departamento.java`](file:///home/joalts8/Ingenieria/Cc/Proyecto-Urbanpulse/Backend/src/main/java/org/bulbaseros/entity/Departamento.java)
     - [`ActivoUrbano.java`](file:///home/joalts8/Ingenieria/Cc/Proyecto-Urbanpulse/Backend/src/main/java/org/bulbaseros/entity/ActivoUrbano.java)
     - [`ArchivoAdjunto.java`](file:///home/joalts8/Ingenieria/Cc/Proyecto-Urbanpulse/Backend/src/main/java/org/bulbaseros/entity/ArchivoAdjunto.java)
     - [`ComentariosIncidencias.java`](file:///home/joalts8/Ingenieria/Cc/Proyecto-Urbanpulse/Backend/src/main/java/org/bulbaseros/entity/ComentariosIncidencias.java)
     - [`Notification.java`](file:///home/joalts8/Ingenieria/Cc/Proyecto-Urbanpulse/Backend/src/main/java/org/bulbaseros/entity/Notification.java)
     - [`Tareas.java`](file:///home/joalts8/Ingenieria/Cc/Proyecto-Urbanpulse/Backend/src/main/java/org/bulbaseros/entity/Tareas.java)
     - [`CambiosEstado.java`](file:///home/joalts8/Ingenieria/Cc/Proyecto-Urbanpulse/Backend/src/main/java/org/bulbaseros/entity/CambiosEstado.java)
     - [`ObservacionesExternas.java`](file:///home/joalts8/Ingenieria/Cc/Proyecto-Urbanpulse/Backend/src/main/java/org/bulbaseros/entity/ObservacionesExternas.java)
   - Anadir el atributo `@Column(name = "public_id", nullable = false, unique = true) private UUID publicId;` con generacion automatica en `@PrePersist` si es nulo:
     ```java
     @PrePersist
     public void prePersist() {
         if (this.publicId == null) {
             this.publicId = UUID.randomUUID();
         }
     }
     ```
3. **Repositorios Spring Data JPA:**
   - Incorporar metodos de consulta por identificador publico en repositorios:
     - `Optional<Incidencia> findByPublicId(UUID publicId);`
     - `Optional<Usuario> findByPublicId(UUID publicId);`
     - `Optional<Departamento> findByPublicId(UUID publicId);`
     - `Optional<ActivoUrbano> findByPublicId(UUID publicId);`
     - `Optional<Notification> findByPublicId(UUID publicId);`
     - etc.

### Fase 2: Mappers y DTOs
1. **Actualizar DTOs:**
   - Adaptar o parametrizar DTOs para que el campo `id` represente la clave publica (formato `String` o `UUID`):
     - `IncidentResponse.java`: campo `id` representado como `String` (o sobrecarga/adaptador para v1 y v2).
     - `DepartmentResponse.java`, `UserSummaryResponse.java`, `NotificationResponse.java`, `IncidentAssetResponse.java`, `AttachmentResponse.java`.
2. **Mappers:**
   - Actualizar mappers en `Backend/src/main/java/org/bulbaseros/mappers/` para mapear `entity.getPublicId().toString()` hacia `dto.setId(...)` en el contexto v2, o disponer de metodos de mapeo v1 (`Long id`) y v2 (`String publicId`).

### Fase 3: Capa de Servicios y Logica de Negocio
1. **Servicios Backend:**
   - Extender [`IncidentService.java`](file:///home/joalts8/Ingenieria/Cc/Proyecto-Urbanpulse/Backend/src/main/java/org/bulbaseros/service/IncidentService.java), [`IncidentTriageService.java`](file:///home/joalts8/Ingenieria/Cc/Proyecto-Urbanpulse/Backend/src/main/java/org/bulbaseros/service/IncidentTriageService.java), [`NotificationService.java`](file:///home/joalts8/Ingenieria/Cc/Proyecto-Urbanpulse/Backend/src/main/java/org/bulbaseros/service/NotificationService.java), etc. para permitir operaciones tanto por `Long id` (para soporte v1) como por `UUID publicId` (para v2).
   - Manejo de excepciones adecuado (`IncidentNotFoundException` cuando no exista por `publicId` o formato invalido de UUID).

### Fase 4: Controladores REST y Seguridad
1. **Versionado de Endpoints y Controladores:**
   - Mantener los controladores v1 mapeados a `/api/v1/...` (y `/api/...` como alias).
   - Crear o mapear controladores v2 bajo `/api/v2/...`:
     - `/api/v2/incidents/**`: Rutas con `@PathVariable UUID id` o `String id`.
     - `/api/v2/departments/**`: Rutas con `@PathVariable UUID id`.
     - `/api/v2/notifications/**`: Rutas con `@PathVariable UUID id`.
     - `/api/v2/analytics/**`: Compatible con parametros v2.
2. **Configuracion de Seguridad ([`SecurityConfig.java`](file:///home/joalts8/Ingenieria/Cc/Proyecto-Urbanpulse/Backend/src/main/java/org/bulbaseros/config/SecurityConfig.java)):**
   - Actualizar las reglas de `SecurityFilterChain` y `JwtAuthenticationFilter` para proteger las rutas tanto en `/api/v1/**` como en `/api/v2/**` segun los roles RBAC correspondientes.

### Fase 5: Actualizacion del Frontend
1. **Cliente API y Servicios Axios:**
   - Actualizar [`frontend/src/services/api.js`](file:///home/joalts8/Ingenieria/Cc/Proyecto-Urbanpulse/frontend/src/services/api.js) o los endpoints especificos en:
     - `incidentService.js` -> apuntar a `/api/v2/incidents/...`
     - `triageService.js` -> apuntar a `/api/v2/incidents/...` y `/api/v2/departments/...`
     - `notificationService.js` -> apuntar a `/api/v2/notifications/...`
     - `analyticsService.js` -> apuntar a `/api/v2/analytics/...`
2. **Componentes y Vistas React:**
   - Asegurar que el manejo de IDs (como claves en listas `key={incident.id}`, parametros de URL en React Router `/incidents/:id`, comparaciones y formularios) acepte cadenas UUID sin asumir que sean numericos enteros.

---

## Decisiones

- **Surrogate Key hibrida (PK numerica + UUID publico)** — Permite mantener un alto desempeno en claves foraneas e indexacion interna en PostgreSQL, mientras que la interfaz publica queda completamente desacoplada y protegida contra enumeraciones.
- **Formato UUID v4 nativo** — Descartamos esquemas propietarios o prefijados para maximizar la compatibilidad con librerias estandar de Java (`java.util.UUID`) y soporte nativo en PostgreSQL.
- **Opcion A en DTOs** — En la respuesta JSON de v2, el campo expuesto se denomina `id` conteniendo la representacion en String del UUID publico. Se descarta mantener el ID numerico para evitar cualquier fuga de informacion interna.
- **Versionado explico `/api/v2`** — Se mantiene `/api/v1` (y `/api/...`) para evitar romper suites de integracion heredadas mientras que el frontend migra completamente a `/api/v2`.

---

## Riesgos

- **Riesgo:** Validacion de formato de UUID en endpoints v2 (solicitudes con cadenas no UUID podrian generar errores 500 no controlados).
  - *Mitigacion:* Configurar validacion o manejo en `GlobalExceptionHandler` retornando `HTTP 400 Bad Request` o `HTTP 404 Not Found` ante UUIDs malformados.
- **Riesgo:** Inconsistencias en scripts de testing o fixtures predefinidos en tests unitarios.
  - *Mitigacion:* En los tests unitarios y fixtures (`@BeforeEach`), inicializar siempre `publicId` (ej. `UUID.randomUUID()`).
- **Riesgo:** Conflictos con el entorno de pruebas H2 en tests que no dispongan de `gen_random_uuid()`.
  - *Mitigacion:* Gestionar la asignacion del UUID a nivel de entidad JPA en `@PrePersist` para garantizar que funcione independientemente del motor de base de datos (PostgreSQL y H2).

---

## Estrategia de tests

- **Unitarios de Persistencia y Repositorios:**
  - Verificacion de persistencia con asignacion automatica de `public_id`.
  - Consultas `findByPublicId` tanto para identificadores existentes como inexistentes.
- **Unitarios de Servicios y Mappers:**
  - Validacion de transformacion de entidades a DTOs con sustitucion estricta de `id` por `publicId.toString()`.
  - Validacion de excepciones de no coincidencia (`IncidentNotFoundException`).
- **Integracion de Controladores (MockMvc):**
  - Pruebas para `/api/v1/incidents` comprobando funcionamiento con IDs numericos.
  - Pruebas para `/api/v2/incidents` comprobando rutas con UUIDs, respuestas con UUIDs y rechazo/404 ante UUIDs invalidos o no encontrados.
- **Frontend Tests (Vitest):**
  - Actualizacion y ejecucion de suites de pruebas de servicios (`incidentService.test.js`, `triageService.test.js`, etc.) verificando que llaman a las rutas `/api/v2/...` con IDs de tipo string/UUID.
