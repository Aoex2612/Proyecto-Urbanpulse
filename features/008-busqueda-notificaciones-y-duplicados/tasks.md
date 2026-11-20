# 008 — Búsqueda, Notificaciones y Duplicados — Tareas

_Checklist accionable derivada del `plan.md`. Tareas principales y subtareas pequeñas y concretas; marca `[x]` al completarlas._

## [x] Tarea 1: Modelo Relacional, Entidad `Notification`, Repositorio Spring Data JPA y Mapeadores - RF: RF-12, RF-14, RF-16, RNF-Persistencia e Integridad
- [x] 1.1 Diseñar e incorporar el script DDL para la tabla `notifications` en `database/init/diagramUrbanPulse.sql` con claves foráneas (`user_id`, `incident_id`), columnas requeridas (`type`, `title`, `message`, `is_read`, `created_at`) e índices de optimización (`user_id`, `is_read`) - RF: RF-12.
- [x] 1.2 Crear entidad JPA `Notification` en `Backend/src/main/java/org/bulbaseros/entity/Notification.java` documentada con Javadoc, anotaciones de Lombok (`@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`) y ciclo `@PrePersist` - RF: RF-12.
- [x] 1.3 Crear repositorio JPA `NotificationRepository` en `org.bulbaseros.repository` con métodos de consulta `findByUserIdOrderByCreatedAtDesc`, `countByUserIdAndIsReadFalse` y `findByIdAndUserId` - RF: RF-13, RF-14, RF-16.
- [x] 1.4 Crear DTO desacoplado `NotificationResponse` en `org.bulbaseros.dto` y mapeador `NotificationMapper` en `org.bulbaseros.mappers` extendiendo `BaseMapper<Notification, NotificationResponse>` con Javadoc riguroso - RF: RF-14, RF-16.
- [x] 1.5 Crear pruebas `@DataJpaTest` en `NotificationRepositoryTest` y pruebas unitarias de mapeo en `NotificationMapperTest` validando persistencia, conteo de pendientes y conversión DTO - RF: RF-12, RF-14, RF-16.
- [x] 1.6 Validar contra los criterios de aceptación de `spec.md`.

## [x] Tarea 2: Servicio de Notificaciones y Controlador REST con Seguridad RBAC - Depende de Tarea 1 - RF: RF-12, RF-13, RF-14, RF-16, RF-17, RNF-Control de Acceso y Privacidad
- [x] 2.1 Crear servicio de negocio `NotificationService` en `org.bulbaseros.service` con métodos para crear notificaciones, listar notificaciones del usuario autenticado de forma descendente, obtener conteo de no leídas y marcar individualmente como leída validando pertenencia del usuario - RF: RF-12, RF-13, RF-14, RF-16, RF-17.
- [x] 2.2 Crear controlador REST `NotificationController` en `org.bulbaseros.controller` exponiendo endpoints autenticados `GET /api/notifications`, `GET /api/notifications/unread-count` y `PATCH /api/notifications/{id}/read` protegidos con anotaciones de seguridad Spring Security - RF: RF-13, RF-14, RF-16.
- [x] 2.3 Actualizar `SecurityConfig` garantizando las políticas de acceso autenticado para las rutas `/api/notifications/**` - RF: RF-13, RF-14.
- [x] 2.4 Crear pruebas unitarias con Mockito en `NotificationServiceTest` y pruebas de integración MockMvc con usuarios simulados (`@WithMockUser`) en `NotificationControllerTest` verificando respuestas 200, 401, 403 y 404 - RF: RF-12, RF-13, RF-14, RF-16, RF-17.
- [x] 2.5 Validar contra los criterios de aceptación de `spec.md`.

## [x] Tarea 3: Integración de Notificaciones en el Ciclo de Vida de Incidencias - Depende de Tarea 2 - RF: RF-12
- [x] 3.1 Integrar `NotificationService` en `IncidentTriageService` y `IncidentService` para registrar automáticamente notificaciones de tipo `STATUS_CHANGE` ante cualquier transición de estado válida (`VALIDATED`, `REJECTED`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`, `REOPENED`, etc.) - RF: RF-12.
- [x] 3.2 Determinar destinatarios del aviso: el ciudadano creador de la incidencia (`incident.getCitizen()`) y todo el personal operativo/técnico asignado activo mediante consulta a `TareasRepository` - RF: RF-12.
- [x] 3.3 Crear pruebas unitarias en `IncidentTriageServiceTest` y `IncidentServiceTest` verificando que cada cambio de estado persiste notificaciones con título y descripción contextual para todos los implicados - RF: RF-12.
- [x] 3.4 Validar contra los criterios de aceptación de `spec.md`.

## [x] Tarea 4: Detección Espaciotemporal y Semántica de Duplicados - Depende de Tarea 1, Tarea 2 - RF: RF-7, RF-8, RF-9, RF-10, RF-11, RNF-Rendimiento
- [x] 4.1 Implementar servicio `DuplicateDetectionService` en `org.bulbaseros.service` con cálculo geodésico de Haversine ($\le 60\text{ metros}$), ventana temporal de registro ($\le 48\text{ horas}$), coincidencia exacta de categoría y concordancia léxica/semántica de palabras clave normalizadas en título y descripción - RF: RF-7, RF-11.
- [x] 4.2 Implementar método para emitir alertas de tipo `DUPLICATE_SUGGESTION` dirigidas exclusivamente a los roles `OPERATOR` y `TECHNICIAN` ante coincidencias detectadas - RF: RF-8.
- [x] 4.3 Implementar endpoint y lógica para confirmación de duplicado por parte de operador/técnico, actualizando el estado de la incidencia a `REJECTED` con motivo de duplicidad y enlace a la incidencia original sin revocar el derecho del ciudadano a seguir reportando incidencias - RF: RF-9, RF-10.
- [x] 4.4 Crear pruebas unitarias con Mockito en `DuplicateDetectionServiceTest` validando umbrales espaciales (dentro/fuera de $60\text{ m}$), temporales (dentro/fuera de $48\text{ h}$), discrepancia de categorías y aislamiento de notificaciones a roles autorizados - RF: RF-7, RF-8, RF-9, RF-10, RF-11.
- [x] 4.5 Validar contra los criterios de aceptación de `spec.md`.

## [x] Tarea 5: Filtrado Multicriterio Dinámico con JPA Specifications - Depende de Tarea 1 - RF: RF-1, RF-2, RF-3, RF-4, RF-5, RF-6, RNF-Rendimiento de Búsqueda
- [x] 5.1 Crear clase de especificaciones dinámicas `IncidentSpecifications` en `org.bulbaseros.specification` construyendo predicados con `CriteriaBuilder` para búsqueda de texto libre sin distinción de mayúsculas ni acentos sobre título y descripción, categoría exacta y rango de fechas de creación (`startDate`, `endDate`) - RF: RF-1, RF-2.
- [x] 5.2 Incorporar predicado por estado (`status`) en `IncidentSpecifications` con validación RBAC en servicio/controlador asegurando que dicho criterio esté disponible únicamente para roles internos (`OPERATOR`, `TECHNICIAN`, `ADMIN`, `ANALYST`) y bloqueado para `CITIZEN` - RF: RF-3, RF-4.
- [x] 5.3 Actualizar `IncidenciaRepository` extendiendo `JpaSpecificationExecutor<Incidencia>` y exponer endpoint `GET /api/incidents/search` en `IncidentController` recibiendo parámetros opcionales de búsqueda multicriterio - RF: RF-1, RF-2, RF-3, RF-4, RF-5, RF-6.
- [x] 5.4 Crear pruebas unitarias en `IncidentSpecificationsTest` y pruebas de integración MockMvc en `IncidentControllerTest` verificando combinaciones de filtros, restricción RBAC para ciudadanos y respuestas vacías - RF: RF-1, RF-2, RF-3, RF-4, RF-5, RF-6.
- [x] 5.5 Validar contra los criterios de aceptación de `spec.md`.

## [x] Tarea 6: Componente de Notificaciones en Frontend (`NotificationBell` y Servicio) - Depende de Tarea 2 - RF: RF-13, RF-14, RF-15, RF-16, RF-17, RNF-Usabilidad y Accesibilidad
- [x] 6.1 Crear módulo cliente `frontend/src/services/notificationService.js` con Axios documentado con JSDoc (`getNotifications`, `getUnreadCount`, `markAsRead`) - RF: RF-13, RF-14, RF-16.
- [x] 6.2 Crear componente `NotificationBell.jsx` y su hoja de estilos `NotificationBell.css` implementando icono de campana con badge numérico reactivo, menú desplegable con scroll interno, diferenciación visual de leídas/no leídas, botón individual de marcado y mensaje amigable ante bandeja vacía - RF: RF-13, RF-14, RF-16, RF-17.
- [x] 6.3 Conectar la interacción de clic en una notificación para abrir la ficha modal de detalle de la incidencia correspondiente (`IncidentDetailModal`) - RF: RF-15.
- [x] 6.4 Integrar el componente `NotificationBell` en las cabeceras de navegación principales (`Navbar.jsx`, `MapPage.jsx` y `OperatorPanelPage.jsx`) - RF: RF-13.
- [x] 6.5 Crear pruebas unitarias con Vitest y React Testing Library en `notificationService.test.js` y `NotificationBell.test.jsx` validando renderizado, badge numérico, despliegue, marcado como leída y apertura de modal - RF: RF-13, RF-14, RF-15, RF-16, RF-17.
- [x] 6.6 Validar contra los criterios de aceptación de `spec.md`.

## [x] Tarea 7: Barra de Búsqueda y Filtrado Multicriterio en Frontend (`IncidentFilterBar`) - Depende de Tarea 5 - RF: RF-1, RF-2, RF-3, RF-4, RF-5, RF-6, RNF-Usabilidad y Accesibilidad
- [x] 7.1 Crear componente reutilizable `IncidentFilterBar.jsx` y su hoja de estilos `IncidentFilterBar.css` con input de texto libre, selector desplegable de categoría, selectores de rango de fechas y selector de estado condicionado por rol (oculto e inaccesible para rol `CITIZEN`) - RF: RF-1, RF-2, RF-3, RF-4.
- [x] 7.2 Implementar botón de restablecimiento rápido de filtros y mensaje informativo cuando la combinación de filtros no arroje resultados - RF: RF-5, RF-6.
- [x] 7.3 Integrar `IncidentFilterBar` en `MapPage.jsx` conectándolo con la capa de marcadores de Leaflet para refresco en tiempo real - RF: RF-1, RF-2, RF-3, RF-4, RF-5, RF-6.
- [x] 7.4 Integrar `IncidentFilterBar` en `OperatorPanelPage.jsx` aplicando los criterios sobre el listado y tabla de gestión operativa - RF: RF-1, RF-2, RF-3, RF-4, RF-5, RF-6.
- [x] 7.5 Crear pruebas unitarias con Vitest en `IncidentFilterBar.test.jsx` y pruebas de integración en `MapPage.test.jsx` y `OperatorPanelPage.test.jsx` comprobando filtrado, ocultación de estado para ciudadanos y limpieza de filtros - RF: RF-1, RF-2, RF-3, RF-4, RF-5, RF-6.
- [x] 7.6 Validar contra los criterios de aceptación de `spec.md`.

## [x] Tarea 8: Visualización y Gestión de Duplicados en Panel Operativo Frontend - Depende de Tarea 4, Tarea 6, Tarea 7 - RF: RF-8, RF-9, RF-10
- [x] 8.1 Incorporar en la tabla de incidencias de `OperatorPanelPage.jsx` un indicador / badge de advertencia visual ("Posible Duplicado") destacando las incidencias con sugerencia de duplicidad activa - RF: RF-8.
- [x] 8.2 Añadir acción contextual en el panel operativo para permitir al operador o técnico confirmar el duplicado, abriendo diálogo de confirmación que invoca la transición a `REJECTED` vinculando el identificador de la incidencia original - RF: RF-9, RF-10.
- [x] 8.3 Crear pruebas unitarias en `OperatorPanelPage.test.jsx` validando la presencia del badge de sugerencia de duplicado, la apertura del diálogo de confirmación y el rechazo exitoso - RF: RF-8, RF-9, RF-10.
- [x] 8.4 Validar contra los criterios de aceptación de `spec.md`.

## [x] Tarea 9: Verificación Integral, Cobertura y Validación de Calidad - Depende de Tareas 1 a 8 - RF: Todos (RF-1 al RF-17), RNF: Todos
- [x] 9.1 Ejecutar suite completa de pruebas unitarias y de integración de Backend con Maven (`mvn clean test`) verificando 100% de tests exitosos y cobertura de código JaCoCo $\ge 80\%$.
- [x] 9.2 Ejecutar suite completa de pruebas de Frontend con Vitest (`npm run test:coverage`) verificando 100% de tests en verde y cobertura $\ge 80\%$.
- [x] 9.3 Ejecutar análisis estático de código en Frontend con ESLint (`npm run lint`) garantizando 0 errores y 0 advertencias.
- [x] 9.4 Realizar comprobación cruzada de todos los criterios de aceptación de `spec.md` y preparar la ejecución de la validación formal con la skill `sdd-validate`.

---

## Mantenimiento (checklist recurrente)

- [x] Verificar que la creación de nuevas entidades y columnas mantenga compatibilidad con las migraciones DDL de PostgreSQL.
- [x] Confirmar que no se rompan las suites de pruebas existentes de triaje, asignaciones y comentarios.
- [x] Comprobar que los tiempos de respuesta del endpoint de búsqueda combinada se mantengan por debajo de 400 ms.
