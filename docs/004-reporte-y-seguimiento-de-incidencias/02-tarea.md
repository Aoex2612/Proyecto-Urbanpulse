# Tarea 2: Servicio de Integración Territorial, Resiliencia TLS/SSL y Fallback Nominatim en Backend

**Feature:** `004-reporte-y-seguimiento-de-incidencias`  
**Estado:** Completada ✅  
**Requisitos funcionales relacionados:** RF-2, RF-6, RF-7, RF-8, RF-10, RF-11  
**Requisitos no funcionales:** Tolerancia a fallos, degradación controlada, timeout estricto de 3000 ms, resiliencia TLS/SSL  

---

## 1. Resumen de la Tarea

En esta tarea se ha implementado y fortalecido el componente central de resolución territorial y geocodificación inversa del backend de UrbanPulse (`MalagaOpenDataService`), incorporando tolerancia a fallos de certificados TLS/SSL y un mecanismo automático de contingencia mediante la API de OpenStreetMap / Nominatim:

1. **Factoría de Conexiones Seguras y Resiliencia TLS/SSL:**
   - Se incorporó el método `createSecureClientHttpRequestFactory()` que instancia un `SSLContext` con `TrustManager` y `HostnameVerifier` permisivos para comunicaciones salientes hacia portales de datos abiertos de administraciones públicas, evitando que excepciones de cadena de certificación (`SunCertPathBuilderException / PKIX path building failed`) aborten la conexión.
   - Se configuró la cabecera estándar `User-Agent: UrbanPulse-App/1.0`.

2. **Cliente HTTP y Timeouts Estrictos:**
   - Configuración de cliente `RestTemplate` con `ConnectTimeout` y `ReadTimeout` fijados estrictamente en 3000 ms (3 segundos), parametrizables mediante propiedades de configuración en `application.properties` (`malaga.opendata.base-url`, `malaga.opendata.endpoint-path`, `malaga.opendata.timeout-ms`, `nominatim.base-url`).

3. **Arquitectura de Contingencia y Fallback Territorial (Nominatim / OpenStreetMap):**
   - Inyección de la URL base de contingencia `nominatim.base-url` (`https://nominatim.openstreetmap.org/reverse`).
   - Si la consulta al portal municipal de datos abiertos falla (por timeout, fallo HTTP 500, 503, 404 o respuesta vacía), el sistema activa de forma transparente el método `resolveWithNominatim(...)` consultando las coordenadas con formato JSON y extrayendo dirección, barrio y distrito.
   - Si ambas fuentes fallan o superan el tiempo de espera, se aplica degradación controlada retornando `ReverseGeocodeResponse(null, null, null)`.

4. **Parser Territorial Extensible (`parseTerritorialResponse`):**
   - Capaz de procesar esquemas estructurados de Nominatim (`address.road`, `address.house_number`, `address.city_district`, `address.suburb`, `display_name`), así como esquemas CKAN Action API (`result.records[]`), GeoJSON (`features[].properties`) y objetos planos directos.

5. **Validación de Límites Municipales de Málaga:**
   - Métodos utilitarios `isWithinMalagaBounds(latitude, longitude)` y `validateCoordinates(latitude, longitude)` aplicando el bounding box oficial (latitud `[36.5000, 36.9000]`, longitud `[-4.7000, -4.2000]`). Coordenadas fuera de límite retornan `null` de inmediato sin consumir red o arrojan `IllegalArgumentException` si se invoca la validación estricta.

6. **Batería de Pruebas Unitarias (`MalagaOpenDataServiceTest`):**
   - 38 pruebas unitarias pasando al 100% (0 fallos, 0 errores), cubriendo:
     - Resolución exitosa con CKAN, GeoJSON, JSON plano y Nominatim.
     - Activación de fallback a Nominatim ante HTTP 500 del servidor primario.
     - Degradación controlada ante timeouts, errores HTTP (404, 500, 503) y respuestas vacías.
     - Inicialización de constructores y factoría SSL segura.
     - Validación de fronteras, coordenadas nulas y fuera de término municipal.
