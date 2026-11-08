# Tarea 3: Lógica de Cálculo de Proximidad y Servicio de Activos (HU16)

## Resumen de la Tarea

En esta tarea se ha implementado el servicio de negocio `UrbanAssetService` y su suite de pruebas unitarias exhaustivas `UrbanAssetServiceTest` para la Feature 007 ("Activos Urbanos y Contexto Externo"). Se han satisfecho íntegramente los requisitos funcionales RF-1, RF-2, RF-3, RF-4, RF-6 y RF-7 especificados en `spec.md`.

## Componentes Implementados

### 1. Servicio de Activos Urbanos (`UrbanAssetService.java`)
Ubicación: `Backend/src/main/java/org/bulbaseros/service/UrbanAssetService.java`

- **Radio de búsqueda máximo:** Constante configurable `DEFAULT_MAX_RADIUS_METERS = 50.0`.
- **Cálculo de Distancia Geodésica con Haversine (`calculateDistanceMeters`):**
  - Aplica la fórmula trigonométrica de Haversine sobre el radio medio de la Tierra (6.371.000 m).
  - Devuelve la distancia geodésica exacta en metros redondeada a 2 posiciones decimales.
  - Soporta conmutatividad y coordenadas idénticas (retornando 0.0).
- **Cálculo de Nivel de Confianza Normalizado (`calculateConfidenceScore`):**
  - Implementa la función matemática: `Math.max(0.0, Math.min(1.0, 1.0 - (distanceMeters / 50.0)))`.
  - Normaliza los valores garantizando que a 0 metros la confianza es `1.0`, a 25 metros es `0.5`, y a 50 metros o más es `0.0`.
  - Redondea el valor a 4 decimales.
- **Asociación de Activos Próximos (`associateNearbyAssets(Incidencia incident)`):**
  - Ejecuta un prefiltrado espacial eficiente mediante caja delimitadora (bounding box) sobre `ActivoUrbanoRepository.findByLatitudeBetweenAndLongitudeBetween(...)` utilizando `deltaLat ~ 50 / 111320` y `deltaLon ~ 50 / (111320 * cos(lat))`.
  - Para cada candidato, calcula la distancia exacta de Haversine y descarta los que superen los 50.0 metros.
  - Ordena los activos candidatos en orden ascendente por distancia geodésica.
  - Construye y persiste en `AsociacionIncidenciaActivoRepository` cada vínculo con `isConfirmed = false` y clave compuesta `AsociacionIncidenciaActivoId`.
  - Retorna la lista mapeada de `IncidentAssetResponse` a través de `UrbanAssetMapper`.
  - **Manejo de casos límite (RF-6):** Si no existen activos en el radio de 50 metros, no lanza error ni interrumpe el flujo, devolviendo una lista vacía.
- **Confirmación Manual de Activo (`confirmAssetAssociation(Long incidentId, Long assetId)`):**
  - Busca la asociación correspondiente en `AsociacionIncidenciaActivoRepository`.
  - Si no existe (RF-7), lanza una excepción estructurada `ResponseStatusException(HttpStatus.NOT_FOUND)` gestionada globalmente por `GlobalExceptionHandler`.
  - Marca `isConfirmed = true`, persiste la actualización y retorna el DTO actualizado.
- **Desvinculación de Activo (`removeAssetAssociation(Long incidentId, Long assetId)`):**
  - Comprueba la existencia previa de la relación; si no existe, lanza `ResponseStatusException(HttpStatus.NOT_FOUND)`.
  - Elimina la asociación mediante `deleteByIncidentIdAndAssetId`.
- **Consulta de Activos por Incidencia (`getAssetsByIncidentId(Long incidentId)`):**
  - Recupera todas las asociaciones de una incidencia ordenadas por proximidad y las transforma a DTOs.

### 2. Pruebas Unitarias (`UrbanAssetServiceTest.java`)
Ubicación: `Backend/src/test/java/org/bulbaseros/service/UrbanAssetServiceTest.java`

- `shouldReturnZeroDistanceForIdenticalCoordinates`: Comprueba distancia 0 entre puntos idénticos.
- `shouldCalculateAccurateDistanceBetweenKnownPoints`: Valida Haversine con puntos conocidos de Málaga (Alameda Principal vs. Plaza de la Marina).
- `shouldBeCommutativeDistanceCalculation`: Valida que la distancia de A a B sea idéntica a la de B a A.
- `shouldReturnMaxConfidenceForZeroDistance`: Valida confianza 1.0 para 0 metros.
- `shouldReturnHalfConfidenceForHalfMaxDistance`: Valida confianza 0.5 para 25 metros.
- `shouldReturnZeroConfidenceForMaxRadiusDistance`: Valida confianza 0.0 para 50 metros.
- `shouldClampConfidenceToZeroForDistancesExceedingLimit`: Valida acotación inferior a 0.0 para distancias > 50 metros.
- `shouldHandleNegativeDistancesGracefully`: Robustez ante distancias negativas.
- `shouldAssociateNearbyAssetsAndExcludeFarOnes`: Valida prefiltrado, cálculo de distancia, filtrado de activos > 50m, ordenación ascendente por distancia y persistencia con `isConfirmed = false`.
- `shouldReturnEmptyListWhenNoAssetsWithinRadius`: Comprueba RF-6 cuando no hay activos en el bounding box.
- `shouldReturnEmptyListWhenBoundingBoxAssetsAreBeyondFiftyMeters`: Comprueba RF-6 cuando los activos en el bounding box caen fuera del círculo de 50m.
- `shouldReturnEmptyListWhenIncidentOrCoordinatesAreNull`: Comprueba robustez ante incidencias sin coordenadas.
- `shouldConfirmAssetAssociationSuccessfully`: Valida RF-3 para la confirmación de activos por operadores.
- `shouldThrowNotFoundWhenConfirmingNonExistentAssociation`: Valida RF-7 con HTTP 404 ante confirmación de activo inexistente.
- `shouldThrowBadRequestWhenConfirmIdsAreNull`: Valida validación de argumentos en confirmación.
- `shouldRemoveAssetAssociationSuccessfully`: Valida RF-4 para desvinculación de asociaciones.
- `shouldThrowNotFoundWhenRemovingNonExistentAssociation`: Valida RF-7 con HTTP 404 ante borrado de activo inexistente.
- `shouldThrowBadRequestWhenRemoveIdsAreNull`: Valida validación de argumentos en desvinculación.
- `shouldRetrieveAndSortAssetsByIncidentId`: Valida recuperación ordenada de activos asociados.
- `shouldReturnEmptyListWhenNoAssociationsOrNullId`: Valida consultas vacías de activos asociados.

## Cumplimiento de Criterios EARS y Reglas de Proyecto
- **RF-1, RF-2:** Asociación geográfica en radio de 50 metros con cálculo de Haversine y cálculo de confianza.
- **RF-3, RF-4:** Métodos de confirmación y desvinculación de mobiliario por operadores/técnicos.
- **RF-6:** Comportamiento transparente ante ausencia de activos cercanos (lista vacía sin excepción).
- **RF-7:** Retorno de excepción 404 ante relaciones no encontradas.
- **Convenciones AGENTS.md:** Javadoc exhaustivo en clase y métodos públicos, inyección por constructor de Spring, transaccionalidad declarativa `@Transactional` e invocación de mappers en la capa de servicios.
