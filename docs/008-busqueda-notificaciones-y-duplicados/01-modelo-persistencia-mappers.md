# Tarea 1: Modelo Relacional, Entidad `Notification`, Repositorio Spring Data JPA y Mapeadores

**Feature:** `008-busqueda-notificaciones-y-duplicados`  
**Estado:** Completada ✅ (Subtareas 1.1 a 1.5 implementadas y verificadas)  
**Requisitos funcionales relacionados:** RF-12, RF-14, RF-16, RNF-Persistencia e Integridad, [ADR-08](../../docs/ADR/ADR-08-notificaciones-in-app-deteccion-duplicados-filtrado-multicriterio.md)  

---

## 1. Resumen de la Implementación

En esta primera tarea de la Feature 008 se ha construido la infraestructura de modelo de datos, persistencia relacional, capa de acceso Spring Data JPA y mapeo de transferencia desacoplado para el subsistema de **Notificaciones In-App**:

### 1.1. Modificación del Esquema DDL Relacional (`database/init/diagramUrbanPulse.sql`)
- **Tabla `notifications`:**
  - `id` (`BIGSERIAL PRIMARY KEY`): Identificador único secuencial.
  - `user_id` (`BIGINT NOT NULL`): Clave foránea al usuario destinatario en `users(id)`.
  - `incident_id` (`BIGINT NOT NULL`): Clave foránea a la incidencia relacionada en `incidents(id)`.
  - `type` (`VARCHAR(50) NOT NULL`): Categoría funcional del evento (`STATUS_CHANGE`, `DUPLICATE_SUGGESTION`, etc.).
  - `title` (`VARCHAR(150) NOT NULL`): Título informativo resumido del aviso.
  - `message` (`TEXT NOT NULL`): Cuerpo descriptivo del aviso.
  - `is_read` (`BOOLEAN DEFAULT false NOT NULL`): Indicador booleano de lectura.
  - `created_at` (`TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL`): Marca temporal del aviso.
- **Claves Foráneas e Integridad Referencial:**
  - `ALTER TABLE "notifications" ADD FOREIGN KEY ("user_id") REFERENCES "users" ("id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;`
  - `ALTER TABLE "notifications" ADD FOREIGN KEY ("incident_id") REFERENCES "incidents" ("id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;`
- **Índices de Optimización:**
  - `CREATE INDEX idx_notifications_user_id ON "notifications" ("user_id");`
  - `CREATE INDEX idx_notifications_is_read ON "notifications" ("is_read");`
- **Secuencia de Autoincremento:**
  - `SELECT setval('notifications_id_seq', (SELECT COALESCE(MAX(id), 1) FROM "notifications"));`

### 1.2. Entidad JPA de Dominio (`org.bulbaseros.entity.Notification`)
- Mapeada sobre la tabla `notifications` en PostgreSQL.
- Campos con relaciones `@ManyToOne(fetch = FetchType.LAZY)` hacia `Usuario` e `Incidencia` con columnas `user_id` e `incident_id`.
- Anotaciones Lombok: `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`, con `@Builder.Default` para inicializar `isRead = false`.
- Método de ciclo de vida `@PrePersist` que asegura `createdAt = OffsetDateTime.now()` e `isRead = false` si no han sido inicializados explícitamente.
- Javadoc exhaustivo en español a nivel de clase y en cada atributo de persistencia.

### 1.3. Repositorio Spring Data JPA (`org.bulbaseros.repository.NotificationRepository`)
- Interfaz que extiende `JpaRepository<Notification, Long>` con métodos de consulta optimizados:
  - `List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);`: Recupera las notificaciones de un usuario en orden cronológico inverso para alimentar la bandeja de notificaciones.
  - `long countByUserIdAndIsReadFalse(Long userId);`: Conteo atómico de avisos no leídos para el badge numérico reactivo.
  - `Optional<Notification> findByIdAndUserId(Long id, Long userId);`: Búsqueda de notificación validando pertenencia del usuario para evitar ataques de acceso cruzado inseguro (IDOR).
- Javadoc riguroso detallando parámetros `@param` y retornos `@return`.

### 1.4. DTO de Respuesta y Mapeador Desacoplado
- **DTO `NotificationResponse` (`org.bulbaseros.dto.NotificationResponse`):**
  - Proyecta los campos serializables (`id`, `incidentId`, `type`, `title`, `message`, `isRead`, `createdAt`) abstrayendo las entidades de persistencia.
- **Mapeador `NotificationMapper` (`org.bulbaseros.mappers.NotificationMapper`):**
  - Componente Spring (`@Component`) que extiende `BaseMapper<Notification, NotificationResponse>`.
  - Sobrescribe `toDto(Notification entity)` resolviendo de manera segura referencias nulas hacia `Incident` (`incident != null ? incident.getId() : null`).
  - Implementa método utilitario `toEntity(Usuario, Incidencia, String, String, String)`.

---

## 2. Batería de Pruebas

### 2.1. Pruebas de Persistencia `@DataJpaTest` (`NotificationRepositoryTest`)
- **`shouldPersistNotificationWithPrePersistDefaults`:** Verifica persistencia, generación de clave primaria y asignación predeterminada en `@PrePersist` (`createdAt != null`, `isRead == false`).
- **`shouldFindNotificationsByUserIdOrderByCreatedAtDesc`:** Verifica ordenación cronológica descendente de las notificaciones del usuario y aislamiento frente a notificaciones de terceros.
- **`shouldCountUnreadNotificationsByUserId`:** Valida que el conteo discrimine notificaciones leídas (`isRead = true`) y filtre exclusivamente las no leídas del usuario consultado.
- **`shouldFindByIdAndUserIdWithUserIsolation`:** Valida recuperación por identificador único garantizando que otro usuario no pueda consultar avisos ajenos.

### 2.2. Pruebas Unitarias de Mapeo (`NotificationMapperTest`)
- **`shouldReturnNullWhenEntityIsNull`:** Retorno seguro de `null` ante entrada nula.
- **`shouldMapEntityToDtoCorrectly`:** Transformación exhaustiva de todos los campos.
- **`shouldMapEntityWithNullIncidentSafely`:** Comprobación de que una notificación sin incidencia asociada genera `incidentId = null` sin lanzar `NullPointerException`.
- **`shouldMapEntityListToDtoList`:** Mapeo de colecciones con `toDtoList`.
- **`shouldHandleNullOrEmptyListSafely`:** Manejo seguro de listas nulas o vacías.
- **`shouldConstructEntityViaToEntityMethod`:** Construcción correcta de entidad con `toEntity`.

---

## 3. Resultados de Compilación y Suite de Pruebas

Ejecución de la suite completa de pruebas del backend con Maven (`mvn clean test`):

```text
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running org.bulbaseros.mappers.NotificationMapperTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.173 s -- in org.bulbaseros.mappers.NotificationMapperTest
[INFO] Running org.bulbaseros.repository.NotificationRepositoryTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 8.998 s -- in org.bulbaseros.repository.NotificationRepositoryTest
...
[INFO] Results:
[INFO] 
[INFO] Tests run: 481, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

- **Tests ejecutados:** 481 pruebas en verde.
- **Fallos y errores:** 0 (100% de éxito).
- **Regresiones:** Ninguna.

---

## 4. Estado y Próximos Pasos

Las subtareas 1.1 a 1.5 de la Tarea 1 han quedado completamente implementadas, probadas y documentadas.  
Conforme a las directrices establecidas, la subtarea 1.6 queda reservada para la fase posterior de validación formal (`sdd-validate`).

**Siguiente paso:** Proceder con la **Tarea 2: Servicio de Notificaciones y Controlador REST con Seguridad RBAC** (`NotificationService`, `NotificationController`, endpoints `/api/notifications/**` y seguridad Spring Security).
