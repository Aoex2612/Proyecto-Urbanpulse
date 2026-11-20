# Tarea 2: Métodos de Consulta y Agregación en Capa de Repositorios en Backend

**Feature:** `009-analitica-urbana-y-reportes`  
**Estado:** Completada ✅  
**Requisitos funcionales relacionados:** RF-1, RF-2, RF-6, RF-7, RF-8, RF-11  

---

## 1. Resumen de la Tarea

En esta tarea se han implementado las consultas especializadas en la capa de persistencia de Spring Data JPA para nutrir el motor de analítica urbana de UrbanPulse. Las consultas abarcan filtrado temporal multidimensional, agregaciones territoriales (distrito y barrio), trazabilidad de estados de triaje y asignaciones departamentales activas e históricas:

### 1.1 Consultas en `IncidenciaRepository` (`org.bulbaseros.repository`)
- **`findByDateRangeAndTerritory(OffsetDateTime startDate, OffsetDateTime endDate, String district, String neighborhood)`**:
  - Consulta JPQL parametrizada que filtra incidencias en un rango temporal (`i.createdAt >= :startDate AND i.createdAt <= :endDate`).
  - Filtrado opcional y resiliente por territorio con soporte *case-insensitive* (`LOWER(i.district) = LOWER(:district)`). Si el parámetro es nulo o cadena vacía (`:district IS NULL OR :district = ''`), la cláusula no restringe el resultado.
  - Ordenación determinista descendente (`ORDER BY i.createdAt DESC`).
- **`findByCreatedAtBetweenOrderByCreatedAtDesc(OffsetDateTime startDate, OffsetDateTime endDate)`**:
  - Consulta por método de Spring Data JPA para la recuperación ordenada cronológicamente de incidencias en intervalos temporales predefinidos.

### 1.2 Consultas en `CambiosEstadoRepository` (`org.bulbaseros.repository`)
- **`findByIncidentIdInOrderByCreatedAtAsc(Collection<Long> incidentIds)`**:
  - Recuperación en bloque (batch) de las transiciones de estado para un conjunto de incidencias, ordenadas cronológicamente por `createdAt ASC`.
  - Permite al servicio analítico calcular el Tiempo Medio de Triaje (MTTT, tiempo entre creación y primer cambio `VALIDATED`/`REJECTED`) y Tiempo Medio de Resolución (MTTR, tiempo hasta `RESOLVED`) en memoria sin generar el problema de consultas $N+1$.
- **`findByIncidentIdAndToStatus(Long incidentId, String toStatus)`** y sobrecarga con enum `findByIncidentIdAndToStatus(Long incidentId, EstadoIncidencia toStatus)`:
  - Recuperación de hitos concretos del ciclo de vida de una incidencia (ej. primera validación o resolución).

### 1.3 Consultas en `TareasRepository` (`org.bulbaseros.repository`)
- **`findByIncidentIdIn(Collection<Long> incidentIds)`**:
  - Recupera todas las asignaciones históricas asociadas a un conjunto de incidencias filtradas.
- **`findByIncidentIdInAndIsActiveTrue(Collection<Long> incidentIds)`**:
  - Recupera exclusivamente las asignaciones operativas en vigor (`is_active = true`) para determinar la carga departamental actual.
- **`findByDepartmentId(Long departmentId)`** y **`findByDepartmentIdAndIsActiveTrue(Long departmentId)`**:
  - Métodos para consultar carga de trabajo acumulada y activa por departamento municipal.

---

## 2. Pruebas de Persistencia (`AnalyticsRepositoriesTest`)

Se ha desarrollado la suite de pruebas de repositorio con `@DataJpaTest` en `org.bulbaseros.repository.AnalyticsRepositoriesTest`:
1. `shouldFilterIncidentsByDateRangeAndTerritory`: Comprueba filtrado por fechas, distrito (insensible a mayúsculas/minúsculas), combinación de distrito y barrio, y comportamiento cuando los parámetros territoriales son nulos o vacíos.
2. `shouldReturnEmptyListWhenNoIncidentsMatchDateRange`: Verifica que ante intervalos temporales sin incidencias se retorna una lista vacía sin excepciones.
3. `shouldOrderIncidentsByCreatedAtDesc`: Comprueba que las incidencias se ordenan descendentemente de más reciente a más antigua.
4. `shouldFindStatusChangesByIncidentIdInOrderedByCreatedAtAsc`: Verifica la recuperación en bloque de cambios de estado ordenados ascendentemente.
5. `shouldFindStatusChangesByIncidentIdAndToStatus`: Comprueba la búsqueda por estado destino tanto mediante `String` como mediante `EstadoIncidencia`.
6. `shouldFindTasksByIncidentIdInAndActiveStatus`: Verifica el filtrado de asignaciones activas e inactivas por lote de incidencias y por departamento.

---

## 3. Decisiones Técnicas

1. **Prevención de problemas N+1 en analítica:** En lugar de consultar los cambios de estado y asignaciones una por una para cada incidencia del reporte, se diseñaron métodos basados en colecciones (`IN :incidentIds`), permitiendo realizar 2 o 3 consultas agregadas y resolver las agrupaciones en memoria dentro del servicio analítico.
2. **Filtrado territorial seguro en JPQL:** Se usó la técnica de evaluación condicional en JPQL `(:district IS NULL OR :district = '' OR LOWER(i.district) = LOWER(:district))` para mantener una única consulta flexible sin requerir Criteria API compleja.
3. **Manejo de marcas de tiempo en H2 para tests:** Dado que `@CreationTimestamp` autogenera la fecha en inserción con `updatable=false`, los tests emplean actualizaciones nativas con vaciado de caché (`entityManager.clear()`) para simular con precisión incidencias históricas y validar la ordenación temporal.
