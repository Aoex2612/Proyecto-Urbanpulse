# 008 — Búsqueda, Notificaciones y Duplicados — Plan

_Cómo se implementa lo descrito en `spec.md`. Respeta estrictamente la `constitution/`, los principios de diseño de UrbanPulse y el [ADR-08](../../docs/ADR/ADR-08-notificaciones-in-app-deteccion-duplicados-filtrado-multicriterio.md)._

## Enfoque

La solución amplía la plataforma UrbanPulse incorporando tres subsistemas interconectados y desacoplados:
1. **Búsqueda y Filtrado Multicriterio Dinámico (HU19):** Implementación en backend mediante Spring Data JPA Specifications (`IncidentSpecifications`) para admitir consultas dinámicas combinando texto libre (título/descripción con `LOWER` e `ILIKE`), categoría, rango de fechas y estado de triaje. En frontend, creación de una barra reutilizable de filtrado (`IncidentFilterBar`) integrada tanto en el Mapa (`MapPage`) como en el Panel del Operador (`OperatorPanelPage`), ocultando y bloqueando selectores de estado para el rol `CITIZEN`.
2. **Detección y Gestión de Duplicados (HU20):** Servicio especializado (`DuplicateDetectionService`) que evalúa la proximidad geográfica mediante la fórmula matemática de Haversine ($\le 60\text{ m}$), la ventana temporal de registro ($\le 48\text{ h}$) y la coincidencia temática/léxica en categoría y texto. Cuando se detecta un duplicado, se emite una notificación de advertencia dirigida exclusivamente a operadores y técnicos, permitiendo rechazar la incidencia con estado `REJECTED` vinculada a la original sin bloquear la creación de nuevos avisos por el ciudadano.
3. **Centro de Notificaciones In-App (HU21):** Modelo relacional persistente en PostgreSQL con la entidad `Notification`, repositorio `NotificationRepository`, DTO `NotificationResponse`, mapeador `NotificationMapper` y servicio `NotificationService`. Los cambios de estado de incidencias registran notificaciones para el autor y el personal técnico/operativo asignado. En frontend, componente de campana (`NotificationBell`) en la barra de navegación con badge de contador reactivo, menú desplegable, marcado individual de lectura y navegación directa al detalle de la incidencia.

---

## Implementación

_Pasos técnicos ordenados por capas y módulos afectados:_

### 1. Base de Datos y Persistencia de Notificaciones (Backend)
- **Esquema SQL (`database/init/diagramUrbanPulse.sql` y migración DDL):**
  - Creación de la tabla `notifications`:
    - `id` (BIGSERIAL PRIMARY KEY)
    - `user_id` (BIGINT NOT NULL, FK a `users(id)` ON DELETE CASCADE)
    - `incident_id` (BIGINT NOT NULL, FK a `incidencias(id)` ON DELETE CASCADE)
    - `type` (VARCHAR(50) NOT NULL: `STATUS_CHANGE`, `DUPLICATE_SUGGESTION`)
    - `title` (VARCHAR(150) NOT NULL)
    - `message` (TEXT NOT NULL)
    - `is_read` (BOOLEAN DEFAULT FALSE NOT NULL)
    - `created_at` (TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL)
  - Índices sobre `user_id` e `is_read` para optimizar consultas de conteo y listado.
- **Entidad JPA (`Backend/src/main/java/org/bulbaseros/entity/Notification.java`):**
  - Mapeo con anotaciones Lombok (`@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`), `@PrePersist` y Javadoc completo.
- **Repositorio JPA (`Backend/src/main/java/org/bulbaseros/repository/NotificationRepository.java`):**
  - `List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);`
  - `long countByUserIdAndIsReadFalse(Long userId);`
  - `Optional<Notification> findByIdAndUserId(Long id, Long userId);`
- **DTOs y Mapeadores (`org.bulbaseros.dto` y `org.bulbaseros.mappers`):**
  - `NotificationResponse`: DTO con `id`, `incidentId`, `type`, `title`, `message`, `isRead`, `createdAt`.
  - `NotificationMapper`: Extiende `BaseMapper<Notification, NotificationResponse>` cumpliendo la convención de mappers del proyecto.

### 2. Lógica de Notificaciones y Despacho en Transiciones (Backend)
- **Servicio (`Backend/src/main/java/org/bulbaseros/service/NotificationService.java`):**
  - `createNotification(User user, Incidencia incident, String type, String title, String message)`
  - `getUserNotifications(String userEmail)`
  - `getUnreadCount(String userEmail)`
  - `markAsRead(Long notificationId, String userEmail)`
- **Integración con el Ciclo de Vida (`IncidentService` y `IncidentTriageService`):**
  - Al ejecutar transiciones de estado (`updateIncidentStatus`, `validateIncident`, `rejectIncident`, `startWork`, `resolveIncident`, `closeIncident`, `reopenIncident`, `reevaluateIncident`):
    - Disparar notificación automática al ciudadano creador del aviso (`incident.getCitizen()`).
    - Disparar notificación a los técnicos y departamentos con asignaciones activas.

### 3. Detección Espaciotemporal de Duplicados (Backend)
- **Servicio (`Backend/src/main/java/org/bulbaseros/service/DuplicateDetectionService.java`):**
  - Método `List<IncidentResponse> findPotentialDuplicates(Double lat, Double lng, String category, String title, String description, Long excludeIncidentId)`:
    - Consulta incidencias candidatas registradas en los últimos 2 días ($48\text{ h}$) con la misma categoría.
    - Aplica la fórmula de Haversine para filtrar únicamente aquellas con distancia $\le 60\text{ metros}$.
    - Evalúa coincidencia léxica/semántica sobre las palabras clave de título y descripción.
  - Método para notificar automáticamente a operadores y técnicos si un nuevo reporte coincide con incidencias previas.

### 4. Filtrado Multicriterio con JPA Specifications (Backend)
- **Especificación Dinámica (`Backend/src/main/java/org/bulbaseros/specification/IncidentSpecifications.java`):**
  - Predicados dinámicos con `CriteriaBuilder`:
    - Búsqueda de texto insensible a mayúsculas sobre `title` y `description`.
    - Filtro exacto por `category`.
    - Filtro por rango de fechas `createdAt` (`startDate`, `endDate`).
    - Filtro por `status` (solo aplicable si el rol del usuario autenticado es `OPERATOR`, `TECHNICIAN`, `ADMIN` o `ANALYST`).
- **Repositorio y Controlador (`IncidenciaRepository` y `IncidentController`):**
  - `IncidenciaRepository` extiende `JpaSpecificationExecutor<Incidencia>`.
  - Endpoint `GET /api/incidents/search` aceptando parámetros opcionales (`text`, `category`, `startDate`, `endDate`, `status`) retornando `List<IncidentResponse>` según permisos de seguridad.

### 5. Controlador REST de Notificaciones (Backend)
- **Controlador (`Backend/src/main/java/org/bulbaseros/controller/NotificationController.java`):**
  - `GET /api/notifications`: Lista las notificaciones del usuario autenticado.
  - `GET /api/notifications/unread-count`: Retorna `{ count: N }` para el badge de la campana.
  - `PATCH /api/notifications/{id}/read`: Marca una notificación individual como leída.

### 6. Componentes Frontend (React 19 + Leaflet)
- **Cliente API (`frontend/src/services/notificationService.js`):**
  - Métodos `getNotifications()`, `getUnreadCount()`, `markAsRead(id)`.
- **Barra de Navegación y Campana de Notificaciones:**
  - Componente `frontend/src/components/Notifications/NotificationBell.jsx` y hoja de estilos `NotificationBell.css`.
  - Icono de campana interactivo con badge numérico animado, menú desplegable flotante con scroll, indicador visual de leído/no leído, botón de marcar individualmente como leída y apertura del modal de detalle (`IncidentDetailModal`) al hacer clic en un aviso.
  - Inclusión en cabeceras de navegación globales (`Navbar`, `MapPage`, `OperatorPanelPage`).
- **Barra de Búsqueda y Filtrado Multicriterio:**
  - Componente reutilizable `frontend/src/components/Filters/IncidentFilterBar.jsx` y `IncidentFilterBar.css`.
  - Campos: Input de texto libre con icono de búsqueda, selector de categoría, selector de rango de fechas y selector de estado (condicionado a rol no ciudadano con `RoleGuard` o verificación de permisos).
  - Botón de limpieza rápida de filtros y mensaje de estado vacío si no hay coincidencias.
  - Integración en `MapPage.jsx` y `OperatorPanelPage.jsx`.
- **Alertas de Duplicados en Triaje Operativo:**
  - Indicador / badge de "Posible Duplicado" en la tabla del panel de operador con enlace a la incidencia origen y botón contextual de confirmación para rechazar con motivo `DUPLICATED`.

---

## Decisiones

_Elecciones de diseño relevantes y su justificación. Alternativas descartadas y por qué._

- **Persistencia Relacional de Notificaciones vs Brokers Externos (ADR-08):**
  - *Decisión:* Tabla relacional `notifications` en PostgreSQL y endpoints REST.
  - *Justificación:* Evita dependencias pesadas innecesarias (Kafka/RabbitMQ) y garantiza consistencia transaccional ACID con el ciclo de vida de incidencias.
- **Fórmula de Haversine en Capa de Servicio vs Extensión PostGIS:**
  - *Decisión:* Cálculo matemático de distancia en Java sobre coordenadas estándar (latitud/longitud).
  - *Justificación:* Mantiene la base de datos portable en PostgreSQL estándar sin requerir extensiones PostGIS adicionales en Docker, garantizando un cálculo ultrarrápido sobre el subconjunto acotado de incidencias de las últimas 48 horas.
- **JPA Specifications vs Múltiples Consultas `@Query` fijas:**
  - *Decisión:* `IncidentSpecifications` combinando predicados dinámicos en una única consulta SQL optimizada.
  - *Justificación:* Facilita combinar libremente cualquiera de los filtros (solo texto, texto + categoría, solo fecha, etc.) sin generar combinaciones explosivas de métodos en el repositorio.
- **Exclusión de Filtro de Estado para Ciudadanos en Backend y Frontend:**
  - *Decisión:* Validación perimetral en Spring Security ignorando o rechazando el parámetro `status` si el token JWT pertenece a un `CITIZEN`, además de ocultar el elemento visual en la interfaz.
  - *Justificación:* Seguridad en profundidad; previene que usuarios modifiquen peticiones de red manualmente para saltarse la regla de negocio.

---

## Riesgos

_Qué puede salir mal o requerir cuidado, y cómo se mitiga._

- **Sobrecarga de notificaciones generadas en transiciones:**
  - *Mitigación:* Generar notificaciones únicamente ante cambios de estado reales y efectivos (`oldStatus != newStatus`), filtrando destinatarios para no notificar al propio actor que ejecuta la acción (ej. el operador no necesita recibir una notificación de que él mismo cambió el estado).
- **Falsos positivos o negativos en detección de duplicados:**
  - *Mitigación:* La sugerencia de duplicado es estrictamente asistida; el sistema nunca rechaza automáticamente una incidencia sin la intervención explícita de un operador o técnico.
- **Rendimiento de búsqueda textual en tablas grandes:**
  - *Mitigación:* Consultas acotadas a incidencias activas e índices B-Tree sobre categorías y fechas para reducir drásticamente el espacio de búsqueda antes de aplicar el predicado textual.

---

## Estrategia de tests

- **Backend (JUnit 5 + Mockito + MockMvc):**
  - `NotificationTest`, `NotificationResponseTest`, `NotificationMapperTest`: Cobertura de entidades y transformaciones.
  - `NotificationServiceTest`: Tests unitarios de creación de notificaciones, conteo de no leídas, marcado de lectura y aislamiento por usuario.
  - `DuplicateDetectionServiceTest`: Tests de cálculo de distancia de Haversine ($\le 60\text{ m}$ y $> 60\text{ m}$), ventana de tiempo ($\le 48\text{ h}$ y $> 48\text{ h}$), coincidencias de categoría y afinidad léxica.
  - `IncidentSpecificationsTest`: Cobertura de predicados dinámicos combinados.
  - `NotificationControllerTest`: Tests de integración MockMvc con autenticación JWT, comprobando respuestas 200 OK, 401 Unauthorized y 404 Not Found.
- **Frontend (Vitest + React Testing Library):**
  - `NotificationBell.test.jsx`: Renderizado de campana, visualización del badge numérico, despliegue del menú, clic en notificación para abrir detalle y llamada a marcar como leída.
  - `IncidentFilterBar.test.jsx`: Filtrado reactivo por texto y categoría, comprobación de que el selector de estado no es visible para `CITIZEN` y sí para `OPERATOR`, y botón de limpiar filtros.
  - Tests de integración en `MapPage` y `OperatorPanelPage` verificando la aplicación fluida de los filtros.
- **Cobertura técnica:**
  - Garantizar cobertura en JaCoCo superior al 80% en los nuevos componentes de backend y 100% de tests en verde en Vitest y ESLint.
