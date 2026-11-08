# Tarea 4: Patrón Circuit Breaker y Servicio de Ingesta de Contexto Externo con Fallback (HU17, HU18)

## Resumen de la Tarea

En esta tarea se ha implementado el componente de resiliencia concurrente `CircuitBreaker` y el servicio de enriquecimiento ambiental y territorial `ExternalContextService`, junto con sus respectivas suites de pruebas unitarias exhaustivas (`CircuitBreakerTest` y `ExternalContextServiceTest`), satisfaciendo íntegramente los requisitos funcionales **RF-8, RF-9, RF-11, RF-12, RF-13, RF-14 y RF-15** definidos en la especificación (`spec.md`) y en los registros de decisión de arquitectura **ADR-06** y **ADR-09**.

---

## Componentes Implementados

### 1. Patrón Circuit Breaker Concurrente (`CircuitBreaker.java` y `CircuitState.java`)
- **Ubicación:** `Backend/src/main/java/org/bulbaseros/service/resilience/CircuitBreaker.java`
- **Ubicación Enum:** `Backend/src/main/java/org/bulbaseros/service/resilience/CircuitState.java`
- **Estados:** `CLOSED`, `OPEN`, `HALF_OPEN`.
- **Características técnicas:**
  - **Thread-safe:** Sincronización y control de transiciones de estado mediante `ReentrantLock`.
  - **Ventana móvil deslizante:** Tamaño por defecto de 10 peticiones (`DEFAULT_WINDOW_SIZE = 10`), gestionada mediante una cola deque en memoria (`callHistory`).
  - **Umbral de fallo:** 50% de fallos (`DEFAULT_FAILURE_RATE_THRESHOLD = 0.50`), provocando la transición automática a `OPEN` en cuanto 5 de las últimas 10 peticiones fallan.
  - **Tiempo de enfriamiento (cooldown):** 30.000 ms (30 segundos, configurable). Tras expirar el tiempo de espera, la siguiente llamada transiciona automáticamente a `HALF_OPEN`.
  - **Recuperación y prueba:** Una llamada exitosa en `HALF_OPEN` restablece el circuito a `CLOSED` y limpia el historial; un fallo en `HALF_OPEN` reactiva de inmediato el estado `OPEN` reiniciando el cooldown.
  - **Métodos expuestos:**
    - `boolean allowExecution()`
    - `boolean isOpen()`
    - `void recordSuccess()`
    - `void recordFailure()`
    - `CircuitState getState()`
    - `void reset()`

---

### 2. Servicio de Contexto Externo (`ExternalContextService.java`)
- **Ubicación:** `Backend/src/main/java/org/bulbaseros/service/ExternalContextService.java`
- **Mecanismos clave:**
  - **Cliente HTTP con Timeouts Estrictos (RF-12):** `RestTemplate` configurado con factoría `SimpleClientHttpRequestFactory` con timeout de conexión de 3.000 ms y timeout de lectura de 3.000 ms.
  - **Ingesta Meteorológica (RF-8):** Conector con la API pública de Open-Meteo (`https://api.open-meteo.com/v1/forecast?latitude={lat}&longitude={lon}&current=temperature_2m,precipitation,wind_speed_10m`). Extrae con robustez Jackson `temperature_2m` (°C), `precipitation` (mm) y `wind_speed_10m` (km/h).
  - **Ingesta de Tráfico (RF-11):** Integración con endpoint territorial/municipal de tráfico de Málaga.
  - **Integración con Circuit Breaker (RF-14):** Comprobación previa de `circuitBreaker.allowExecution()`. Si el circuito está abierto (`OPEN`), omite llamadas de red y retorna inmediatamente una lista vacía con 0 ms de sobrecarga de latencia.
  - **Mecanismo de Fallback Silencioso (RF-13, RF-15):** Captura de `RestClientException`, timeouts de socket, errores HTTP 4xx/5xx y excepciones de parseo; registra el fallo en `circuitBreaker.recordFailure()`, emite advertencia de log estructurada (`log.warn`) y retorna las observaciones obtenidas o una lista vacía sin arrojar excepciones ni interrumpir la creación de la incidencia.
  - **Persistencia Desacoplada (RF-9):** Almacenamiento de cada métrica en `external_observations` (`ObservacionesExternas`) y vinculación cruzada a la incidencia en `incident_contexts` (`ContextoIncidencia`).
  - **Mapeo a DTOs:** Transformación de entidades a `ExternalObservationResponse` a través de `UrbanContextMapper`.
  - **Consulta por Incidencia:** Método `getContextByIncidentId(Long incidentId)` para recuperar las métricas contextuales en la vista de detalle.

---

### 3. Pruebas Unitarias

#### A. `CircuitBreakerTest.java`
- **Ubicación:** `Backend/src/test/java/org/bulbaseros/service/resilience/CircuitBreakerTest.java`
- **Casos de prueba:**
  1. `shouldStartInClosedStateAndAllowExecution`: Inicialización en CLOSED y permiso de ejecución.
  2. `shouldRemainClosedWhenFailuresDoNotExceedThreshold`: Comprobación de permanencia en CLOSED ante tasas de error inferiores al 50%.
  3. `shouldTransitionToOpenWhenFailureThresholdReached`: Transición automática a OPEN cuando 5 de 10 peticiones fallan (50%).
  4. `shouldRejectExecutionsWhileOpen`: Rechazo inmediato de llamadas mientras el circuito permanece en OPEN.
  5. `shouldTransitionToHalfOpenAfterCooldownExpires`: Transición a HALF_OPEN tras transcurrir el periodo de enfriamiento.
  6. `shouldRecoverToClosedAfterSuccessInHalfOpen`: Recuperación a CLOSED tras una prueba exitosa en HALF_OPEN.
  7. `shouldReopenAfterFailureInHalfOpen`: Reapertura inmediata a OPEN si la prueba en HALF_OPEN falla.
  8. `shouldResetToClosedStateManually`: Validación del reseteo forzado manual a CLOSED.
  9. `shouldValidateConstructorParameters`: Validación de límites y argumentos inválidos en la configuración.

#### B. `ExternalContextServiceTest.java`
- **Ubicación:** `Backend/src/test/java/org/bulbaseros/service/ExternalContextServiceTest.java`
- **Casos de prueba:**
  1. `shouldCaptureAndSaveWeatherContextSuccessfully`: Simulación de respuesta 200 OK de Open-Meteo con JSON meteorológico completo; verifica deserialización, persistencia en repositorios, registro de éxito en Circuit Breaker y mapeo a DTOs.
  2. `shouldFallbackSilentlyOnSocketTimeout`: Simulación de `SocketTimeoutException` por latencia excesiva; verifica activación del fallback silencioso, registro de fallo en Circuit Breaker, lista vacía retornada y cero excepciones lanzadas.
  3. `shouldFallbackSilentlyOnHttpServerError`: Simulación de error HTTP 500 del servidor remoto; verifica degradación controlada y registro de fallo.
  4. `shouldDegradeImmediatelyWithoutNetworkCallWhenCircuitBreakerIsOpen`: Verifica que ante estado OPEN no se ejecutan peticiones a la red ni se consume timeout.
  5. `shouldReturnEmptyListWhenIncidentOrCoordinatesAreMissing`: Validación defensiva ante incidencias nulas o sin coordenadas.
  6. `shouldRetrieveObservationsByIncidentId`: Consulta y mapeo correcto de contexto por identificador de incidencia.
  7. `shouldReturnEmptyListWhenNoContextOrNullIncidentId`: Gestión de incidencias sin contexto previo registrado.

---

## Cumplimiento de Requisitos EARS y Arquitectura

| Requisito | Descripción | Estado |
|---|---|---|
| **RF-8** | Consulta de API meteorológica para temperatura, lluvia y viento | **Completado** (`fetchWeatherObservations`) |
| **RF-9** | Persistencia en `external_observations` e `incident_contexts` | **Completado** (guardado transaccional en repositorios) |
| **RF-11** | Ingesta de contexto de tráfico territorial | **Completado** (`fetchTrafficObservations`) |
| **RF-12** | Timeout estricto de 3.000 ms en peticiones externas | **Completado** (`configureRestTemplate` a 3000 ms) |
| **RF-13** | Fallback silencioso y logging de advertencia ante errores o timeouts | **Completado** (captura segura y degradación sin fallos) |
| **RF-14** | Circuit Breaker con ventana de 10 peticiones y umbral 50% | **Completado** (`CircuitBreaker` thread-safe y cooldown 30s) |
| **RF-15** | Retorno de lista vacía en creación de incidencia ante indisponibilidad | **Completado** (degradación transparente que no bloquea HTTP 201) |
