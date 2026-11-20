# Tarea 6: Servicios API y Modales de Triaje y Asignación en Frontend

**Feature:** `005-gestion-y-triaje-operativo-de-incidencias`  
**Estado:** Completada ✅  
**Requisitos funcionales relacionados:** RF-1, RF-2, RF-8, RF-9, RF-10, RF-11, RF-12  

---

## 1. Resumen de la Tarea

En esta tarea se ha desarrollado la infraestructura de cliente HTTP y los componentes modales accesibles en el Frontend de UrbanPulse para la gestión integral del triaje operativo, la priorización asistida con cálculo en tiempo real y la asignación multidepartamental con selección dependiente de técnicos:

1. **`triageService.js` (`frontend/src/services/triageService.js`):**
   - Integración directa con la instancia Axios `api` configurada en `api.js` (con inyección automática de Bearer JWT).
   - Documentación exhaustiva en español con formato JSDoc para todas las funciones, parámetros y promesas de retorno.
   - Implementación de los 16 métodos cliente que cubren todo el ciclo de vida y operaciones de triaje:
     * `calculatePriority(incidentId, { streetCut, safetyRisk })` &rarr; `POST /api/incidents/:id/priority/calculate` (RF-1).
     * `updatePriority(incidentId, { priority, priorityReason })` &rarr; `PUT /api/incidents/:id/priority` (RF-2).
     * `validateIncident(incidentId, data)` &rarr; `POST /api/incidents/:id/validate`.
     * `rejectIncident(incidentId, { reason })` &rarr; `POST /api/incidents/:id/reject`.
     * `reevaluateIncident(incidentId, { targetStatus, reason })` &rarr; `POST /api/incidents/:id/reevaluate`.
     * `startWork(incidentId)` &rarr; `POST /api/incidents/:id/start-work`.
     * `resolveIncident(incidentId, data)` &rarr; `POST /api/incidents/:id/resolve`.
     * `closeIncident(incidentId)` &rarr; `POST /api/incidents/:id/close`.
     * `reopenIncident(incidentId, { reason })` &rarr; `POST /api/incidents/:id/reopen`.
     * `assignDepartmentsAndTechnicians(incidentId, { departmentIds, technicianIds })` &rarr; `POST /api/incidents/:id/assignments` (RF-8, RF-9, RF-10).
     * `deactivateAssignments(incidentId, { departmentIds, technicianIds })` &rarr; `POST /api/incidents/:id/assignments/deactivate` (RF-11, RF-12).
     * `getIncidentComments(incidentId)` &rarr; `GET /api/incidents/:id/comments`.
     * `addIncidentComment(incidentId, { comment, isInternal })` &rarr; `POST /api/incidents/:id/comments`.
     * `updateIncidentComment(incidentId, commentId, { comment })` &rarr; `PUT /api/incidents/:id/comments/:commentId`.
     * `getDepartments()` &rarr; `GET /api/departments` (RF-8).
     * `getTechniciansByDepartment(departmentId)` &rarr; `GET /api/departments/:id/technicians` (RF-9).

2. **Ampliación de `incidentService.js` (`frontend/src/services/incidentService.js`):**
   - Adición del método `getIncidentPublicComments(incidentId)` documentado con JSDoc, que invoca `GET /api/incidents/:id/comments` para habilitar el seguimiento ciudadano transparente de incidencias.
   - Inclusión en la exportación por defecto `incidentService`.

3. **Modal de Asistente de Priorización (`PriorityAssistantModal.jsx` y `PriorityAssistantModal.css`):**
   - Modal accesible según directrices WAI-ARIA (`role="dialog"`, `aria-modal="true"`, `aria-labelledby`, captura de tecla Escape, trampa/autofoco en botón de cierre y cierre al hacer clic en el backdrop).
   - Formulario de evaluación con casilla para corte de vía y desplegable para nivel de riesgo de seguridad (`NONE`, `LOW`, `HIGH`).
   - Botón interactivo "Calcular prioridad sugerida" que llama a la API asistida y despliega una tarjeta de recomendación destacada con la etiqueta de prioridad calculada y la justificación técnica.
   - Sincronización automática de la prioridad definitiva (`finalPriority`) y su justificación (`finalReason`), permitiendo al operador ratificarla o ajustarla antes de confirmar.
   - Guardado mediante `updatePriority` con emisión reactiva del callback `onPriorityUpdated` y `onConfirm`.
   - Manejo completo de estados de carga (`calculating`, `saving`) y errores de red.

4. **Modal de Asignación Multidepartamental (`AssignmentModal.jsx` y `AssignmentModal.css`):**
   - Modal accesible con atributos ARIA y navegación por teclado.
   - Carga dinámica del catálogo de departamentos municipales al abrir mediante `getDepartments()`.
   - Selección múltiple de departamentos mediante tarjetas interactivas accesibles con casillas de verificación.
   - Carga bajo demanda y almacenamiento en caché de los técnicos adscritos a cada departamento seleccionado (`getTechniciansByDepartment`).
   - Selector dependiente de técnicos por departamento con chips interactivos; al desmarcar un departamento, se deseleccionan automáticamente sus técnicos vinculados.
   - Sección de visualización de asignaciones activas existentes de la incidencia, con soporte para desactivación lógica unitaria mediante `deactivateAssignments`.
   - Validación en cliente que exige seleccionar al menos un departamento antes de enviar.
   - Confirmación mediante `assignDepartmentsAndTechnicians` con emisión de `onAssignmentsUpdated`.

---

## 2. Cobertura de Pruebas Unitarias con Vitest

Se han desarrollado tres suites de pruebas unitarias exhaustivas con Vitest y `@testing-library/react`:

### `triageService.test.js` (17 pruebas)
- Pruebas de integración HTTP para los 16 métodos con simulación de Axios:
  * Invocación correcta de URL, verbo HTTP y payload esperado.
  * Cobertura de métodos de ciclo de vida (`calculatePriority`, `updatePriority`, `validateIncident`, `rejectIncident`, `reevaluateIncident`, `startWork`, `resolveIncident`, `closeIncident`, `reopenIncident`).
  * Cobertura de asignaciones y desactivaciones (`assignDepartmentsAndTechnicians`, `deactivateAssignments`).
  * Cobertura de comentarios y notas (`getIncidentComments`, `addIncidentComment`, `updateIncidentComment`).
  * Cobertura de catálogos (`getDepartments`, `getTechniciansByDepartment`).
  * Verificación de la exportación default con todas las referencias ligadas.

### `incidentService.test.js` (actualización a 7 pruebas)
- Verificación del nuevo método `getIncidentPublicComments(incidentId)` invocando `GET /api/incidents/:id/comments`.
- Disponibilidad a través de named export y default export.

### `PriorityAssistantModal.test.jsx` (6 pruebas)
- Descarte y no renderizado si `isOpen=false` o `incident=null`.
- Renderizado accesible y presentación de datos de la incidencia.
- Simulación de cálculo de prioridad asistida con actualización del badge sugerido y justificación.
- Modificación manual por el operador y confirmación guardando la prioridad.
- Visualización de errores de API.
- Cierre mediante botón de cierre y tecla Escape.

### `AssignmentModal.test.jsx` (6 pruebas)
- Descarte y no renderizado si `isOpen=false` o `incident=null`.
- Renderizado y carga del catálogo de departamentos municipales.
- Visualización de asignaciones activas y ejecución de desactivación lógica.
- Validación impidiendo guardar sin ningún departamento seleccionado.
- Selección multidepartamental y carga dependiente de técnicos con guardado exitoso.
- Cierre mediante botón Cancelar y tecla Escape.

---

## 3. Verificación de Calidad

- **Pruebas de Frontend (`npm test`):** 13 archivos de prueba ejecutados, 116 pruebas pasadas (100% de éxito), 0 advertencias.
- **Linter de Frontend (`npm run lint`):** 0 errores, 0 advertencias con ESLint.
