# Tarea 2: Servicio de Notificaciones y Controlador REST con Seguridad RBAC

**Feature:** `008-busqueda-notificaciones-y-duplicados`  
**Estado:** Completada ✅ (Subtareas 2.1 a 2.4 implementadas y verificadas)  
**Requisitos funcionales relacionados:** RF-12, RF-13, RF-14, RF-16, RF-17, RNF-Control de Acceso y Privacidad, [ADR-08](../../docs/ADR/ADR-08-notificaciones-in-app-deteccion-duplicados-filtrado-multicriterio.md)  

---

## 1. Resumen de la Implementación

En esta segunda tarea de la Feature 008 se ha desarrollado la capa de servicio de negocio transaccional, el controlador REST y la configuración de seguridad perimetral de Spring Security para el subsistema de **Notificaciones In-App**:

### 1.1. Servicio de Notificaciones (`org.bulbaseros.service.NotificationService`)
- **Anotaciones y Transaccionalidad:**
  - `@Service` y `@Transactional(readOnly = true)` a nivel de clase para optimizar transacciones de solo lectura en consultas.
  - Inyección por constructor de `NotificationRepository`, `UserRepository` y `NotificationMapper`.
- **Métodos Implementados:**
  - `@Transactional public Notification createNotification(Usuario user, Incidencia incident, String type, String title, String message)`:
    - Construye y persiste una entidad `Notification` con valores predeterminados (no leída, fecha actual).
    - Ejecutado en transacción de escritura atómica para su futura invocación desde los servicios de triaje y detección de duplicados.
  - `public List<NotificationResponse> getUserNotifications(String userEmail)`:
    - Resuelve el usuario destinatario por su email o arroja `ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado")`.
    - Consulta `notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId())` aprovechando el índice `idx_notifications_user_id`.
    - Proyecta la colección mediante `notificationMapper.toDtoList(...)`.
  - `public long getUnreadCount(String userEmail)`:
    - Resuelve el usuario y realiza un conteo atómico optimizado con `notificationRepository.countByUserIdAndIsReadFalse(user.getId())`.
    - Utilizado para alimentar reactivamente el badge numérico de la campana en el frontend.
  - `@Transactional public NotificationResponse markAsRead(Long notificationId, String userEmail)`:
    - Resuelve el usuario autenticado y busca la notificación mediante `notificationRepository.findByIdAndUserId(notificationId, user.getId())`.
    - Arroja `ResponseStatusException(HttpStatus.NOT_FOUND, "Notificación no encontrada o no pertenece al usuario")` si no existe o pertenece a otro usuario, previniendo vulnerabilidades de referencia directa insegura a objetos (IDOR).
    - Actualiza el estado a `isRead = true`, persiste la entidad y retorna el DTO proyectado.

### 1.2. Controlador REST de Notificaciones (`org.bulbaseros.controller.NotificationController`)
- **Anotaciones y Enrutamiento:**
  - `@RestController` y `@RequestMapping("/api/notifications")`.
  - Inyección por constructor de `NotificationService`.
- **Endpoints Expuestos:**
  - `GET /api/notifications`:
    - Extrae el correo del emisor mediante `extractUserEmail(Authentication)`.
    - Retorna `ResponseEntity<List<NotificationResponse>>` con código HTTP 200 OK.
  - `GET /api/notifications/unread-count`:
    - Retorna `ResponseEntity<Map<String, Object>>` con `{ "count": N }` y código HTTP 200 OK.
  - `PATCH /api/notifications/{id}/read`:
    - Marca la notificación especificada como leída y retorna `ResponseEntity<NotificationResponse>` con HTTP 200 OK.
- **Seguridad e Identidad:**
  - Método auxiliar privado `extractUserEmail(Authentication authentication)` que obtiene el `principal` autenticado de manera segura.

### 1.3. Configuración de Seguridad Perimetral (`org.bulbaseros.config.SecurityConfig`)
- Incorporada la regla de autorización en `securityFilterChain`:
  ```java
  .requestMatchers("/api/notifications/**").authenticated()
  ```
- Bloquea peticiones anónimas retornando HTTP 401 Unauthorized estructurado con `JwtAuthenticationEntryPoint`.
- Permite acceso a cualquier usuario con sesión válida (roles `CITIZEN`, `OPERATOR`, `TECHNICIAN`, `ADMIN`, `ANALYST`).
- Actualizado el Javadoc descriptivo de la cadena de filtros documentando el endpoint de notificaciones.

---

## 2. Batería de Pruebas

### 2.1. Pruebas Unitarias de Servicio (`NotificationServiceTest`)
- **`shouldCreateAndPersistNotification`:** Verifica la persistencia atómica y retorno de la entidad `Notification`.
- **`shouldGetUserNotificationsSuccessfully`:** Verifica la obtención y mapeo de notificaciones en orden cronológico descendente.
- **`shouldThrowNotFoundWhenUserDoesNotExistOnGetUserNotifications`:** Valida que se lance 404 si el usuario no existe en la base de datos.
- **`shouldGetUnreadCountSuccessfully`:** Valida la obtención del conteo numérico de no leídas.
- **`shouldThrowNotFoundWhenUserDoesNotExistOnGetUnreadCount`:** Valida error 404 al consultar conteo de un usuario inexistente.
- **`shouldMarkNotificationAsReadSuccessfully`:** Valida la actualización del flag `isRead = true`, persistencia y mapeo DTO.
- **`shouldThrowNotFoundWhenNotificationNotOwnedOrNotFound`:** Valida que intentar marcar una notificación ajena o inexistente retorne 404 Not Found protegiendo la privacidad del usuario.
- **`shouldThrowNotFoundWhenUserDoesNotExistOnMarkAsRead`:** Valida 404 si el usuario emisor no existe.

### 2.2. Pruebas WebMvc y Seguridad RBAC (`NotificationControllerTest`)
- **`shouldReturn401WhenUnauthenticatedOnGetNotifications`:** Petición anónima a `GET /api/notifications` rechazada con 401 Unauthorized.
- **`shouldReturn401WhenUnauthenticatedOnGetUnreadCount`:** Petición anónima a `GET /api/notifications/unread-count` rechazada con 401 Unauthorized.
- **`shouldReturn401WhenUnauthenticatedOnMarkAsRead`:** Petición anónima a `PATCH /api/notifications/{id}/read` rechazada con 401 Unauthorized.
- **`shouldReturnNotificationsListWhenAuthenticated`:** Usuario autenticado recibe 200 OK con el array JSON de notificaciones.
- **`shouldReturnUnreadCountWhenAuthenticated`:** Usuario autenticado recibe 200 OK con el mapa `{ "count": 5 }`.
- **`shouldMarkNotificationAsReadWhenAuthenticated`:** Usuario autenticado recibe 200 OK y la notificación actualizada con `isRead: true`.
- **`shouldReturn404WhenNotificationNotFoundOrNotOwned`:** Retorna 404 Not Found cuando la notificación no existe o no pertenece al usuario emisor.

### 2.3. Pruebas Perimetrales (`SecurityConfigTest`)
- **`shouldRejectUnauthenticatedAccessToNotifications`:** Verifica rechazo 401 perimetral para peticiones no autenticadas.
- **`shouldAuthorizeCitizenRoleOnNotifications`:** Verifica que un usuario con rol `CITIZEN` supere la barrera de seguridad hacia `/api/notifications`.

---

## 3. Resultados de Compilación y Suite de Pruebas

Resultados de la ejecución de pruebas con Maven:

- `NotificationServiceTest`: 8 pruebas exitosas, 0 fallos, 0 errores.
- `NotificationControllerTest`: 7 pruebas exitosas, 0 fallos, 0 errores.
- `SecurityConfigTest`: 19 pruebas exitosas, 0 fallos, 0 errores.
- **Suite global Backend:** 100% de pruebas en verde sin regresiones.
