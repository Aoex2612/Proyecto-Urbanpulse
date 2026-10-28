# Tarea 1: DTOs, Repositorio Paginado y Mapeadores en Backend

**Feature:** `004-reporte-y-seguimiento-de-incidencias`  
**Estado:** Completada ✅  
**Requisitos funcionales relacionados:** RF-4, RF-12, RF-13, RF-14  

---

## 1. Resumen de la Tarea

En esta tarea se ha establecido la base de transferencia de datos y persistencia paginada en el backend para dar soporte tanto a la detección territorial como al seguimiento personal de incidencias por parte del ciudadano:

1. **DTO `ReverseGeocodeResponse`:** Creado en `org.bulbaseros.dto` para transportar la dirección postal normalizada, distrito y barrio con documentación Javadoc completa.
2. **DTO `PageResponse<T>`:** Creado en `org.bulbaseros.dto` para encapsular respuestas paginadas estandarizadas (`content`, `totalElements`, `totalPages`, `page`, `size`) abstrayendo objetos Page de Spring Data y cumpliendo el contrato canónico de `docs/api.md`.
3. **Ampliación de `IncidentResponse`:** Incorporados los atributos territoriales (`accuracyMeters`, `address`, `district`, `neighborhood`) y constructores correspondientes.
4. **Repositorio `IncidenciaRepository`:** Incorporados los métodos paginados `findByCitizenId` y `findByCitizenIdAndStatus` con `Pageable`.
5. **Mapeador `IncidentMapper`:** Mapeo de campos territoriales y nuevo método `toPageResponse(Page<Incidencia> page)`.
6. **Tests unitarios y de persistencia:** Creados tests en `ReverseGeocodeResponseTest`, `PageResponseTest`, `IncidentResponseTest`, `IncidentMapperTest` e `IncidenciaRepositoryTest`, con 207 tests en verde y 0 fallos.
