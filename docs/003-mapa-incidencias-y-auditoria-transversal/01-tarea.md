# Tarea 1: Entidades, DTOs y Repositorios en Backend (Persistencia y Auditoria)

**Feature:** `003-mapa-incidencias-y-auditoria-transversal`  
**Estado:** Completada  
**Requisitos funcionales relacionados:** RF-1, RF-10, RF-13, RF-15  

---

## 1. Resumen de la Tarea

En esta tarea se establecio la base de persistencia y transferencia de datos del backend para dar soporte tanto a la visualizacion cartografica con Leaflet como a la auditoria transversal inmutable de transiciones de estado de incidencias:

1. Se verificaron y ajustaron las entidades JPA `Incidencia` y `CambiosEstado`, garantizando compatibilidad absoluta con el esquema relacional PostgreSQL (`incidents` y `status_changes`), incorporando anotaciones `@ManyToOne`, claves foraneas, marcas de auditoria `@CreationTimestamp` y `@UpdateTimestamp`, callbacks de ciclo de vida `@PrePersist`/`@PreUpdate` y documentacion exhaustiva mediante Javadoc.
2. Se creo `IncidenciaRepository`, proveyendo metodos de consulta para recuperar incidencias activas en el mapa mediante exclusion de estados concluidos (`findByStatusNotIn` con enums y `findByStatusNotInStrings` con cadenas mediante JPQL).
3. Se creo `CambiosEstadoRepository`, proveyendo el metodo `findByIncidentIdOrderByCreatedAtAsc` para recuperar la traza historica de cambios de estado ordenada cronologicamente.
4. Se implementaron los DTOs requeridos con validaciones Jakarta Bean Validation y documentacion Javadoc completa:
   - `CreateIncidentRequest`: validaciones `@NotBlank` para titulo, descripcion y categoria, y `@NotNull` junto a rangos decimales para latitud y longitud.
   - `IncidentResponse`: representacion de incidencias con identificador, emisor ciudadano, categoria, coordenadas, estado, prioridad y marcas temporales, junto al metodo factoria `fromEntity`.
   - `StatusChangeResponse`: representacion historica de auditoria de transiciones con identificador, incidencia vinculada, estados previo y resultante, datos completos del usuario actor, motivo y marca temporal, junto al metodo factoria `fromEntity`.
5. Se creo una suite completa de 20 pruebas unitarias y de integracion (`@DataJpaTest` y pruebas unitarias de DTOs y validacion Bean Validation), alcanzando 136 tests exitosos (100% en verde).

---

## 2. Subtareas implementadas

### 2.1 Entidades JPA `Incidencia` y `CambiosEstado` (Subtarea 1.1)
- **`Backend/src/main/java/org/bulbaseros/entity/Incidencia.java`:**
  - Mapeo completo con tabla `incidents` (`id`, `title`, `description`, `category`, `latitude`, `longitude`, `accuracy_meters`, `address`, `district`, `neighborhood`, `status`, `priority`, `priority_reason`, `citizen_id`, `created_at`, `updated_at`).
  - Relacion `@ManyToOne(fetch = FetchType.LAZY)` hacia `Usuario` sobre la columna `citizen_id`.
  - Mapeo de enums con `@Enumerated(EnumType.STRING)` para `EstadoIncidencia` y `PrioridadIncidencia`.
  - Marcas temporales con `@CreationTimestamp` y `@UpdateTimestamp`.
  - Callbacks `@PrePersist` y `@PreUpdate` para fijar valores por defecto (`REPORTED`, `MEDIUM`) y marcas de tiempo.
  - Documentacion Javadoc completa en clase, atributos, constructores y metodos.
- **`Backend/src/main/java/org/bulbaseros/entity/CambiosEstado.java`:**
  - Mapeo con tabla `status_changes` (`id`, `incident_id`, `from_status`, `to_status`, `changed_by_user_id`, `reason`, `created_at`).
  - Relaciones `@ManyToOne(fetch = FetchType.LAZY)` hacia `Incidencia` (`incident_id`) y hacia `Usuario` (`changed_by_user_id`).
  - Marca temporal inmutable `@CreationTimestamp` con `updatable = false`.
  - Callback `@PrePersist` para garantizar la fijacion de la fecha de auditoria.
  - Constructores y documentacion Javadoc completa.

### 2.2 Repositorio `IncidenciaRepository` (Subtarea 1.2)
- **`Backend/src/main/java/org/bulbaseros/repository/IncidenciaRepository.java`:**
  - Extiende `JpaRepository<Incidencia, Long>`.
  - Metodo `List<Incidencia> findByStatusNotIn(Collection<EstadoIncidencia> statuses)` para filtrado nativo derivado.
  - Metodo `List<Incidencia> findByStatusNotInStrings(@Param("statuses") Collection<String> statuses)` mediante consulta JPQL con cast explicito para soporte con literales `String`.
  - Documentacion Javadoc completa.

### 2.3 Repositorio `CambiosEstadoRepository` (Subtarea 1.3)
- **`Backend/src/main/java/org/bulbaseros/repository/CambiosEstadoRepository.java`:**
  - Extiende `JpaRepository<CambiosEstado, Long>`.
  - Metodo `findByIncidentIdOrderByCreatedAtAsc(Long incidentId)` implementado mediante consulta JPQL sobre `c.incident.id = :incidentId ORDER BY c.createdAt ASC`.
  - Documentacion Javadoc completa.

### 2.4 DTOs de Creacion, Representacion y Auditoria (Subtarea 1.4)
- **`Backend/src/main/java/org/bulbaseros/dto/CreateIncidentRequest.java`:**
  - Validaciones Jakarta: `@NotBlank` en `title`, `description` y `category` con mensajes explicativos; `@NotNull`, `@DecimalMin` y `@DecimalMax` en `latitude` ([-90.0, 90.0]) y `longitude` ([-180.0, 180.0]).
  - Campos opcionales para `accuracyMeters`, `address`, `district` y `neighborhood`.
  - Constructores y getters/setters con Javadoc.
- **`Backend/src/main/java/org/bulbaseros/dto/IncidentResponse.java`:**
  - Campos: `id`, `title`, `description`, `category`, `latitude`, `longitude`, `status`, `priority`, `citizenId`, `citizenName`, `createdAt`, `updatedAt`.
  - Metodo factoria `fromEntity(Incidencia incidencia)` para simplificar el mapeo desde la entidad de dominio.
  - Constructores y getters/setters con Javadoc.
- **`Backend/src/main/java/org/bulbaseros/dto/StatusChangeResponse.java`:**
  - Campos: `id`, `incidentId`, `fromStatus`, `toStatus`, `changedByUserId`, `changedByUserName`, `changedByUserEmail`, `reason`, `createdAt`.
  - Metodo factoria `fromEntity(CambiosEstado cambio)` para conversion directa de registros historicos.
  - Constructores y getters/setters con Javadoc.

---

## 3. Archivos creados / modificados

| Archivo | Tipo de cambio | Descripcion |
|---|---|---|
| `Backend/src/main/java/org/bulbaseros/entity/Incidencia.java` | Modificado | Entidad JPA con mapeo a `incidents`, timestamps, callbacks y Javadoc |
| `Backend/src/main/java/org/bulbaseros/entity/CambiosEstado.java` | Modificado | Entidad JPA con mapeo a `status_changes`, relaciones `@ManyToOne` y Javadoc |
| `Backend/src/main/java/org/bulbaseros/repository/IncidenciaRepository.java` | Creado | Repositorio Spring Data JPA para incidencias y filtrado por estado |
| `Backend/src/main/java/org/bulbaseros/repository/CambiosEstadoRepository.java` | Creado | Repositorio Spring Data JPA para auditoria de cambios de estado ordenados |
| `Backend/src/main/java/org/bulbaseros/dto/CreateIncidentRequest.java` | Creado | DTO de alta con validaciones Bean Validation y Javadoc |
| `Backend/src/main/java/org/bulbaseros/dto/IncidentResponse.java` | Creado | DTO de representacion de incidencia con factoria `fromEntity` y Javadoc |
| `Backend/src/main/java/org/bulbaseros/dto/StatusChangeResponse.java` | Creado | DTO de representacion de transicion historica con factoria `fromEntity` y Javadoc |
| `Backend/src/test/java/org/bulbaseros/dto/CreateIncidentRequestTest.java` | Creado | Pruebas unitarias de validaciones Bean Validation para alta de incidencia (7 tests) |
| `Backend/src/test/java/org/bulbaseros/dto/IncidentResponseTest.java` | Creado | Pruebas unitarias de constructores, getters y `fromEntity` (4 tests) |
| `Backend/src/test/java/org/bulbaseros/dto/StatusChangeResponseTest.java` | Creado | Pruebas unitarias de constructores, getters y `fromEntity` (4 tests) |
| `Backend/src/test/java/org/bulbaseros/repository/IncidenciaRepositoryTest.java` | Creado | Pruebas de integracion `@DataJpaTest` para guardado y filtrado de incidencias activas (3 tests) |
| `Backend/src/test/java/org/bulbaseros/repository/CambiosEstadoRepositoryTest.java` | Creado | Pruebas de integracion `@DataJpaTest` para orden cronologico de auditoria (2 tests) |
| `features/003-mapa-incidencias-y-auditoria-transversal/tasks.md` | Modificado | Marcada la Tarea 1 y sus subtareas como completadas `[x]` |
| `docs/003-mapa-incidencias-y-auditoria-transversal/01-tarea.md` | Creado | Documentacion tecnica de la tarea |

---

## 4. Resultado de las Pruebas

Ejecucion de la suite completa de pruebas en `Backend`:
```
[INFO] Results:
[INFO] 
[INFO] Tests run: 136, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```
Se agregaron 20 nuevas pruebas (todas pasando satisfactoriamente), manteniendo el 100% de los tests existentes sin regresiones.
