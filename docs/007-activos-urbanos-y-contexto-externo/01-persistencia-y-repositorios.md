# Tarea 1: Capa de Persistencia y Repositorios JPA para Activos y Contexto

## Resumen de la Tarea

En esta tarea se ha establecido la infraestructura de persistencia relacional con Spring Data JPA para la gestión de activos urbanos, sus asociaciones geolocalizadas con incidencias y la ingesta de observaciones del contexto externo (meteorología y tráfico).

## Repositorios Implementados

1. **`ActivoUrbanoRepository` (`org.bulbaseros.repository.ActivoUrbanoRepository`)**:
   - `findByLatitudeBetweenAndLongitudeBetween(Double minLat, Double maxLat, Double minLon, Double maxLon)`: Realiza una consulta derivada de Spring Data JPA para filtrar activos urbanos dentro de una caja delimitadora espacial (*bounding box*). Permite optimizar el cálculo geodésico posterior descartando de forma temprana activos fuera del radio de proximidad.
   - `findByAssetCode(String assetCode)`: Permite la localización unívoca por código de inventario del activo municipal.

2. **`AsociacionIncidenciaActivoRepository` (`org.bulbaseros.repository.AsociacionIncidenciaActivoRepository`)**:
   - `findByIncidentId(Long incidentId)`: Obtiene todas las vinculaciones de activos asociadas a una incidencia determinada.
   - `findByIncidentIdAndAssetId(Long incidentId, Long assetId)`: Recupera la asociación singular entre una incidencia y un activo específico.
   - `deleteByIncidentIdAndAssetId(Long incidentId, Long assetId)`: Permite desvincular un activo urbano de una incidencia a petición de un operador o administrador.

3. **`ObservacionesExternasRepository` (`org.bulbaseros.repository.ObservacionesExternasRepository`)**:
   - Repositorio CRUD estándar para almacenar observaciones meteorológicas o lecturas de tráfico en `external_observations`.

4. **`ContextoIncidenciaRepository` (`org.bulbaseros.repository.ContextoIncidenciaRepository`)**:
   - `findByIncidentId(Long incidentId)`: Recupera las relaciones entre una incidencia y sus observaciones externas vinculadas.

## Fixtures y Pruebas Unitarias/Integración

Se ha implementado la suite de pruebas `UrbanAssetsAndContextRepositoriesTest` (`Backend/src/test/java/org/bulbaseros/repository/UrbanAssetsAndContextRepositoriesTest.java`) utilizando `@DataJpaTest` y `TestEntityManager` con H2 en memoria:
- **`shouldFindActivoUrbanoByAssetCode`**: Comprueba la búsqueda por identificador de activo y la gestión de ausencia.
- **`shouldFindActivosInBoundingBox`**: Valida que el prefiltrado por latitud/longitud acota correctamente los activos del centro histórico de Málaga frente a elementos alejados (ej. Teatinos).
- **`shouldManageAsociacionIncidenciaActivo`**: Verifica el ciclo de vida completo de la asociación: persistencia, consulta por incidencia, consulta por clave compuesta, confirmación manual por operador y eliminación/desvinculación.
- **`shouldManageObservacionesExternasAndContextoIncidencia`**: Valida la persistencia desacoplada de observaciones climáticas y de tráfico, y su vinculación contextual con la incidencia.

Además, la base de datos de inicialización (`database/init/diagramUrbanPulse.sql`) cuenta con fixtures precargados de 60 activos distribuidos por el municipio de Málaga (`urban_assets`) y asociaciones iniciales de muestra.
