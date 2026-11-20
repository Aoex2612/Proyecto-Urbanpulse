# Tarea 2: DTOs y Mappers Desacoplados

## Resumen de la Tarea

En esta tarea se han diseñado e implementado los Objetos de Transferencia de Datos (DTO) y los Mappers desacoplados para la Feature 007 ("Activos Urbanos y Contexto Externo"), garantizando el estricto aislamiento de la capa de persistencia relacional JPA frente a la capa de presentación REST y el contrato de la API pública.

## Componentes Implementados

### 1. DTOs en `org.bulbaseros.dto`
- **`IncidentAssetResponse` (`IncidentAssetResponse.java`)**:
  - `assetId` (`Long`): Identificador del activo municipal en el inventario.
  - `assetCode` (`String`): Código unívoco del activo (ej. `"SEM-MAL-00142"`).
  - `type` (`String`): Tipología del activo (ej. `"SEMAFORO"`, `"FUENTE"`, `"CONTENEDOR"`).
  - `name` (`String`): Nombre descriptivo o denominación pública.
  - `latitude` (`Double`), `longitude` (`Double`): Coordenadas geográficas WGS84 del activo.
  - `distanceMeters` (`Double`): Distancia calculada en metros entre la incidencia y el activo.
  - `confidenceScore` (`Double`): Puntuación probabilística normalizada (0.0 a 1.0).
  - `isConfirmed` (`Boolean`): Indicador de confirmación manual formalizada por el operador.
  - Soporte completo con Lombok (`@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`) y Javadoc detallado en clase y atributos.

- **`ExternalObservationResponse` (`ExternalObservationResponse.java`)**:
  - `id` (`Long`): Identificador único de la observación registrada.
  - `source` (`String`): Origen del dato ambiental o de tráfico (ej. `"OPEN_METEO"`, `"DGT_TRAFFIC"`).
  - `metricType` (`String`): Clave de métrica (`"TEMPERATURE_C"`, `"PRECIPITATION_MM"`, `"WIND_SPEED_KMH"`, `"TRAFFIC_LEVEL"`).
  - `valueNum` (`Double`): Valor numérico cuantitativo registrado.
  - `valueText` (`String`): Valor textual o cualitativo registrado.
  - `observedAt` (`OffsetDateTime`): Marca temporal de la captura con zona horaria.
  - Soporte completo con Lombok (`@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`) y Javadoc exhaustivo.

- **Modificación de `IncidentResponse` (`IncidentResponse.java`)**:
  - Incorporación de los atributos:
    - `private List<IncidentAssetResponse> urbanAssets`: Lista de activos urbanos vinculados o sugeridos.
    - `private List<ExternalObservationResponse> urbanContext`: Lista de observaciones contextuales externas.
  - Métodos `getUrbanAssets()`, `setUrbanAssets(...)`, `getUrbanContext()` y `setUrbanContext(...)` con Javadoc.
  - Mantenimiento retrocompatible de todos los constructores y factorías estáticas preexistentes.

### 2. Mappers en `org.bulbaseros.mappers`
- **`UrbanAssetMapper` (`UrbanAssetMapper.java`)**:
  - Extiende de `BaseMapper<AsociacionIncidenciaActivo, IncidentAssetResponse>`.
  - Anotado con `@Component` para inyección de dependencias en Spring.
  - Método `toDto(AsociacionIncidenciaActivo entity)`: Convierte la entidad de relación extrayendo los datos descriptivos de `ActivoUrbano` y las métricas espaciales (`distanceMeters`, `confidenceScore`, `isConfirmed`). Soporta activos nulos recuperando el identificador de la clave compuesta.
  - Sobrecarga `toDto(ActivoUrbano asset, Double distanceMeters, Double confidenceScore, Boolean isConfirmed)`: Permite construir el DTO directamente a partir de la entidad de activo y las métricas computadas en memoria.
  - Herencia de `toDtoList(...)` con control seguro de nulos y colecciones vacías.

- **`UrbanContextMapper` (`UrbanContextMapper.java`)**:
  - Extiende de `BaseMapper<ObservacionesExternas, ExternalObservationResponse>`.
  - Anotado con `@Component`.
  - Convierte la entidad `ObservacionesExternas` al DTO inmutable `ExternalObservationResponse`.
  - Herencia de `toDtoList(...)` para listas de observaciones.

## Pruebas Unitarias

Se implementaron suites exhaustivas de pruebas unitarias en `Backend/src/test/java/org/bulbaseros/mappers/`:
- **`UrbanAssetMapperTest`**:
  - `shouldReturnNullWhenEntityIsNull`: Valida manejo de entradas nulas.
  - `shouldMapAsociacionToDtoCorrectly`: Verifica mapeo íntegro de todos los campos de asociación y activo.
  - `shouldMapAsociacionWithNullAssetCorrectly`: Comprueba tolerancia ante relaciones perezosas o activos nulos.
  - `shouldMapFromActivoUrbanoWithCalculatedMetrics`: Valida mapeo directo con métricas sobre la marcha.
  - `shouldReturnNullWhenActivoUrbanoIsNull`: Manejo de activo directo nulo.
  - `shouldMapListCorrectly`: Verifica transformación en bloque de listas de asociaciones.
  - `shouldReturnEmptyListWhenListIsNullOrEmpty`: Manejo de colecciones vacías o nulas.
- **`UrbanContextMapperTest`**:
  - `shouldReturnNullWhenEntityIsNull`: Retorno nulo para entidad nula.
  - `shouldMapEntityToDtoCorrectly`: Mapeo exhaustivo de métricas completas y marcas temporales `OffsetDateTime`.
  - `shouldMapEntityWithPartialDataCorrectly`: Cobertura de métricas con valores cuantitativos nulos y categóricos presentes.
  - `shouldMapListCorrectly`: Transformación por lotes de observaciones climáticas y viales.
  - `shouldReturnEmptyListWhenListIsNullOrEmpty`: Protección ante listas nulas o desprovistas de elementos.
