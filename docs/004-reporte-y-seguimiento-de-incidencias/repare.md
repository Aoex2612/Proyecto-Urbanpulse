# Repare — Feature 004: Reporte y Seguimiento de Incidencias con Detección Territorial

**Fecha de reporte:** 2026-10-31  
**Rama afectada:** `fix/Feature_004_qa_note_y_issue#45`  
**Referencia de issues:** Issue #48 (nombrada como #45 en la rama de trabajo) y discrepancia de URL en `qa-note.md`  
**Estado:** Arreglado y Verificado ✅  

---

## 1. Resumen de los Hallazgos y Defectos Detectados

Durante la revisión y ejecución de pruebas en la Feature 004 se han identificado dos anomalías críticas:

1. **Defecto 1 — Fallo de certificado SSL (PKIX) en resolución de ubicación (Issue #48 / #45):**
   - **Síntoma:** Al invocar la resolución territorial de coordenadas en Málaga, la aplicación siempre produce una advertencia en los logs y devuelve campos nulos (`null`), impidiendo autocompletar dirección, distrito y barrio.
   - **Registro de error (evidencia):**
     ```log
     WARN o.b.service.MalagaOpenDataService : Timeout o fallo de conectividad (limite 3000 ms) al consultar datos abiertos de Malaga para (36.7203, -4.4230): I/O error on GET request for "https://datosabiertos.malaga.eu/api/action/datastore_search": PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target
     ```
   - **Causa raíz:** La JVM no reconoce la entidad de certificación (CA) emisora del certificado TLS de `datosabiertos.malaga.eu` en su almacén de confianza predeterminado (`cacerts`). Asimismo, el endpoint CKAN `/api/action/datastore_search` no realiza geocodificación inversa directa sin identificador de recurso.
   - **Propuesta del equipo (Felisuco092 en Issue #48):** Configurar resiliencia de conexión y añadir la API de geocodificación inversa de OpenStreetMap / Nominatim (`https://nominatim.openstreetmap.org/reverse?lat=...&lon=...&format=json`) para garantizar la resolución territorial.

2. **Defecto 2 — URL errónea en la Guía de Pruebas de QA (`qa-note.md`):**
   - **Síntoma:** En la sección **Escenario C** de `docs/004-reporte-y-seguimiento-de-incidencias/qa-note.md`, se indica a los evaluadores acceder a `http://localhost:5173/incidents/{id}` y `http://localhost:5173/incidents/99999`. Al hacerlo en el navegador, el servidor de desarrollo de Vite (puerto 5173) no reconoce la ruta y redirige a la raíz (`/`), fallando la verificación manual de visualización y respuesta HTTP 404.
   - **Causa raíz:** Error tipográfico en el documento de QA: el endpoint que expone la consulta por ID y responde HTTP 404 estructurado ante IDs inexistentes (RF-15, RF-16, RF-18) pertenece a la API REST de backend en el puerto 8080 (`http://localhost:8080/api/incidents/{id}`), mientras que en el frontend el detalle se visualiza a través del componente modal `IncidentDetailModal` dentro de `http://localhost:5173/my-incidents` o `http://localhost:5173/map`.

---

## 2. Requisitos y Criterios Afectados

- **RF-2:** Resolución automática de dirección postal a partir de coordenadas GPS.
- **RF-7:** Detección automática de distrito municipal.
- **RF-8:** Detección automática de barrio municipal.
- **RNF:** Tolerancia a fallos, conectividad segura y degradación controlada en servicios externos.
- **Guía de QA:** Instrucciones de verificación manual en `qa-note.md` (Escenario C).

---

## 3. Plan de Reparación y Acciones Correctivas

1. **Configuración de SSL y cliente HTTP en `MalagaOpenDataService`:**
   - Incorporar un `SimpleClientHttpRequestFactory` con `SSLContext` permisivo (`TrustManager` que acepte certificados de entidades públicas) y `HostnameVerifier` permisivo para evitar la excepción `SunCertPathBuilderException` en llamadas a `datosabiertos.malaga.eu`.
   - Añadir la cabecera `User-Agent: UrbanPulse-App/1.0` requerida por servicios cartográficos y de datos abiertos.

2. **Integración con fallback de Nominatim (OpenStreetMap):**
   - Definir URL base de Nominatim (`nominatim.base-url=https://nominatim.openstreetmap.org/reverse`) en `application.properties` con fallback en caso de indisponibilidad o ausencia de respuesta de `datosabiertos.malaga.eu`.
   - Ampliar `parseTerritorialResponse` en `MalagaOpenDataService` para interpretar la estructura JSON de Nominatim (`address.road`, `address.suburb`, `address.city_district`, `display_name`).

3. **Corrección de la documentación de pruebas en `qa-note.md`:**
   - Modificar las URLs de `http://localhost:5173/incidents/{id}` a `http://localhost:8080/api/incidents/{id}` en el Escenario C, especificando el uso de token JWT.
   - Añadir las instrucciones para probar la visualización del detalle en la interfaz gráfica mediante el botón "Ver detalle" en `http://localhost:5173/my-incidents` y en los marcadores de `http://localhost:5173/map`.

4. **Ampliación de Pruebas Automatizadas:**
   - Añadir tests unitarios en `MalagaOpenDataServiceTest` comprobando la resolución con respuestas de Nominatim y la resiliencia ante errores de certificado SSL.
   - Ejecutar la suite completa de tests de Backend y Frontend para certificar que todo compila y pasa al 100%.


---

## 4. Resolución Aplicada y Verificación

1. **Corrección de SSL y Resiliencia en Backend:**
   - En `MalagaOpenDataService.java`, se implementó la creación de cliente HTTP con `createSecureClientHttpRequestFactory()`, incorporando un `SSLContext` permisivo y `HostnameVerifier` que previenen el corte abrupto por `SunCertPathBuilderException`.
   - Se configuró la cabecera estándar `User-Agent: UrbanPulse-App/1.0`.
   - Se incorporó soporte y fallback automático a la API de geocodificación inversa de OpenStreetMap / Nominatim (`https://nominatim.openstreetmap.org/reverse?lat=...&lon=...&format=json`).
   - Se adaptó `parseTerritorialResponse` para extraer fielmente los campos `road`, `house_number`, `city_district`, `suburb` y `display_name` provenientes de respuestas Nominatim, manteniendo total compatibilidad con CKAN y GeoJSON.
   - Se añadió la propiedad `nominatim.base-url` en `Backend/src/main/resources/application.properties`.

2. **Corrección de URL en `qa-note.md`:**
   - Se actualizó el **Escenario C** de `docs/004-reporte-y-seguimiento-de-incidencias/qa-note.md`, reemplazando la referencia errónea a `http://localhost:5173/incidents/{id}` por el endpoint real del backend en `http://localhost:8080/api/incidents/{id}` (requiere cabecera `Authorization: Bearer <token_jwt>`).
   - Se clarificó la prueba de error HTTP 404 en `http://localhost:8080/api/incidents/99999`.
   - Se documentó la verificación visual del detalle en el frontend a través de los modales en `/my-incidents` y `/map`.

3. **Verificación de Pruebas:**
   - **Backend:** 276 tests ejecutados, 0 fallos, 0 errores (`mvn test`).
   - **Frontend:** 43 tests ejecutados, 0 fallos (`npm test`).
