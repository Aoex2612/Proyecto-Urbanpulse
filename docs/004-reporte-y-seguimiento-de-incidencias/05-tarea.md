# Tarea 5: Capa de Servicio y Formulario de Alta con Solo Lectura en Frontend

**Feature:** `004-reporte-y-seguimiento-de-incidencias`  
**Estado:** Completada ✅  
**Requisitos funcionales relacionados:** RF-1, RF-2, RF-3, RF-5, RF-9, RF-11  
**Requisitos no funcionales:** Tolerancia a fallos, degradación controlada, accesibilidad, validación en cliente, usabilidad responsive  

---

## 1. Resumen de la Tarea

En esta tarea se ha desarrollado e integrado la capa de servicio cliente y la detección territorial en el formulario de creación asistida de incidencias (`CreateIncidentModal`), dotando a la aplicación de resolución automática de la dirección postal, barrio y distrito municipal a partir de las coordenadas geográficas fijadas sobre el mapa cartográfico interactivo, con las siguientes características:

1. **Ampliación de `frontend/src/services/incidentService.js`:**
   - **Método `getMyIncidents(params = {})`:**
     - Realiza peticiones `GET /api/incidents/my-incidents` con query params (`page`, `size`, `status`).
     - Retorna el objeto `PageResponse` con `{ content, totalElements, totalPages, page, size }`.
     - Documentado con JSDoc completo indicando tipos de retorno y excepciones.
   - **Método `reverseGeocode(latitude, longitude)`:**
     - Realiza peticiones `GET /api/incidents/geocode/reverse` enviando `latitude` y `longitude` como parámetros de consulta.
     - Retorna `{ address, district, neighborhood }`.
     - Implementa degradación controlada (`try/catch`): si la llamada externa falla por timeout, indisponibilidad de red o error de servidor, captura la excepción y retorna `{ address: null, district: null, neighborhood: null }` sin interrumpir el flujo del usuario.
     - Documentado con JSDoc completo.
   - Ambos métodos exportados como funciones nombradas y dentro del objeto `default`.

2. **Resolución Territorial y Campos de Solo Lectura en `CreateIncidentModal.jsx`:**
   - Al abrirse el modal y recibir `selectedLocation` con coordenadas válidas:
     - Activa el estado de carga `isLoadingLocation(true)` para presentar el indicador visual accesible ("Resolviendo dirección...").
     - Invoca `incidentService.reverseGeocode(latNumber, lngNumber)` de forma asíncrona no bloqueante.
     - Actualiza el estado territorial `locationInfo` con los valores resueltos de dirección, barrio y distrito.
   - **Modo Estrictamente de Solo Lectura (`readOnly`):**
     - Los campos `Dirección postal`, `Barrio` y `Distrito` se presentan bloqueados con atributo `readOnly` y `aria-readonly="true"`, impidiendo la edición manual por parte de la ciudadanía según las directrices consensuadas del sistema.
   - **Etiqueta Informativa "Sin asignar":**
     - Si la dirección, el barrio o el distrito son nulos o vacíos (o si falla la geocodificación), el valor de los campos y las etiquetas visuales asociadas muestran `"Sin asignar"`.
   - **Payload de Envío Enriquecido:**
     - Al enviar el formulario, el payload para `incidentService.createIncident` incluye opcionalmente los datos resueltos (`address`, `district`, `neighborhood`), permitiendo que el backend los utilice o los resuelva de manera coordinada.
   - **Limpieza de Formulario y Descarte:**
     - Al pulsar "Cancelar" o el botón de cierre, se limpian todos los campos, vista previa de adjuntos, estado territorial y se descarta la selección notificando mediante `onClose` u `onCancel`.

3. **Estilos Coherentes y Responsivos en `Map.css`:**
   - Reglas añadidas para `.territorial-section`, `.territorial-grid` (con adaptación móvil a una columna), `.form-label-row`, `.unassigned-badge` y el indicador de carga `.location-resolving-indicator` con animación sutil.

4. **Batería de Pruebas Unitarias con Vitest y React Testing Library:**
   - `frontend/src/components/Map/CreateIncidentModal.test.jsx`:
     - Verificación de creación sin adjuntos (0 imágenes).
     - Validación de formato no permitido de imágenes (solo JPEG/PNG).
     - Adjunto multimedia y previsualización de imagen.
     - Invocación automática de `reverseGeocode` con coordenadas y muestra de dirección, barrio y distrito resueltos.
     - Verificación de que dirección, barrio y distrito son de solo lectura (`readOnly`).
     - Verificación de visualización de `"Sin asignar"` ante respuesta con valores nulos o fallo de red.
     - Verificación de descarte y limpieza al pulsar cancelar (`onClose` / `onCancel`).
     - Validación de campos requeridos (título, categoría, descripción).
     - Visualización del indicador "Resolviendo dirección...".
   - `frontend/src/services/incidentService.test.js`:
     - Verificación de `getMyIncidents` con query params y formato `PageResponse`.
     - Verificación de `reverseGeocode` con coordenadas.
     - Verificación de degradación controlada ante excepciones HTTP 500 / error de red retornando `{ address: null, district: null, neighborhood: null }`.
   - Resultados: **5 archivos de prueba, 32 tests ejecutados, 100% verde (0 fallos), 0 warnings en linter (`npm run lint`)**.

---

## 2. Archivos Modificados / Creados

- `frontend/src/services/incidentService.js`: Adición de métodos `getMyIncidents` y `reverseGeocode` con degradación controlada y JSDoc.
- `frontend/src/components/Map/CreateIncidentModal.jsx`: Integración de geocodificación inversa, indicador de carga, campos territoriales en solo lectura y etiquetas "Sin asignar".
- `frontend/src/components/Map/Map.css`: Estilos visuales para sección territorial, etiquetas "Sin asignar" e indicador de resolución.
- `frontend/src/components/Map/CreateIncidentModal.test.jsx`: Pruebas unitarias de integración del modal y resolución territorial.
- `frontend/src/services/incidentService.test.js`: Nueva suite de pruebas unitarias para la capa de servicios de incidencias.
- `frontend/src/utils/dateUtils.js`: Ajuste de robustez en rangos de fecha UTC para presets temporales.
- `features/004-reporte-y-seguimiento-de-incidencias/tasks.md`: Marcado de la Tarea 5 como completada `[x]`.
- `docs/004-reporte-y-seguimiento-de-incidencias/05-tarea.md`: Documentación técnica de la tarea.
