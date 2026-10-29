# Tarea 2: Servicio de Integración con Datos Abiertos de Málaga y Degradación Controlada en Backend

**Feature:** `004-reporte-y-seguimiento-de-incidencias`  
**Estado:** Completada ✅  
**Requisitos funcionales relacionados:** RF-2, RF-6, RF-7, RF-8, RF-10, RF-11  
**Requisitos no funcionales:** Tolerancia a fallos, degradación controlada, timeout estricto de 3000 ms  

---

## 1. Resumen de la Tarea

En esta tarea se ha implementado el componente central de resolución territorial y geocodificación inversa del backend de UrbanPulse, garantizando el aislamiento de fallos externos mediante la política de degradación controlada prescrita en el [ADR-06](../../docs/ADR/ADR-06-integracion-datos-abiertos-malaga-degradacion-controlada.md):

1. **Servicio `MalagaOpenDataService`:** Creado en `org.bulbaseros.service` con documentación Javadoc exhaustiva en clases, métodos y parámetros.
2. **Cliente HTTP y Timeouts Estrictos:** Configurado cliente `RestTemplate` con `connectTimeout` y `readTimeout` fijados en 3000 ms (3 segundos), parametrizables mediante propiedades (`malaga.opendata.base-url`, `malaga.opendata.endpoint-path`, `malaga.opendata.timeout-ms`).
3. **Validación de Límites Municipales:** Definición de constantes para el término municipal de Málaga (latitud `[36.5000, 36.9000]`, longitud `[-4.7000, -4.2000]`), complementado con métodos utilitarios `isWithinMalagaBounds(latitude, longitude)` y `validateCoordinates(latitude, longitude)`.
4. **Resolución Territorial Asistida:** Implementación del método público `ReverseGeocodeResponse resolveLocation(Double latitude, Double longitude)`, capaz de consultar la API de Datos Abiertos de Málaga (`datosabiertos.malaga.eu`) y parsear con resiliencia esquemas CKAN Action API (`result.records[]`), GeoJSON (`features[].properties`) o JSON directos.
5. **Política de Degradación Controlada:** Ante timeouts (`SocketTimeoutException`, `ResourceAccessException`), indisponibilidad del servicio externo (HTTP 500, 503), respuestas 404, respuestas malformadas, coordenadas fuera de límites o ausencia de coincidencia, el servicio captura limpiamente la excepción, registra un log de advertencia y retorna un objeto `ReverseGeocodeResponse(null, null, null)`, asegurando que nunca se bloquee el flujo de creación o consulta de avisos.
6. **Batería de Pruebas Unitarias Exhaustivas (`MalagaOpenDataServiceTest`):** 34 casos de prueba estructurados con JUnit 5, AssertJ, Mockito y `MockRestServiceServer` cubriendo todos los escenarios exigidos:
   - Resolución exitosa con esquema CKAN Action API.
   - Resolución exitosa con esquema GeoJSON FeatureCollection.
   - Resolución con esquema JSON plano y arrays directos.
   - Respuestas sin coincidencia (records vacíos, features vacíos, JSON vacío, respuesta en blanco).
   - Degradación controlada ante timeout de 3 segundos (`SocketTimeoutException`).
   - Degradación controlada ante error HTTP 500 Internal Server Error.
   - Degradación controlada ante HTTP 503, HTTP 404 y fallos generales de conectividad.
   - Coordenadas nulas y fuera de los límites municipales de Málaga.
   - Verificación de límites frontera e internalidad geográfica.
   - Validación estricta de coordenadas con `IllegalArgumentException`.
   - Inicialización mediante `RestTemplateBuilder` y constructores sobrecargados.
   - Cobertura de código resultante superior al 99% de instrucciones y 100% de métodos en JaCoCo, alcanzando 241 tests en verde en la suite global de Backend.
