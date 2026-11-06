# Tarea 7: Muro de Comentarios y Sección de Intervención Técnica en Frontend

**Feature:** `005-gestion-y-triaje-operativo-de-incidencias`  
**Estado:** Completada ✅  
**Requisitos funcionales relacionados:** RF-13, RF-14, RF-15, RF-16, RF-20, RF-21, RF-22, RF-23, RF-24  

---

## 1. Resumen de la Tarea

En esta tarea se ha implementado la infraestructura de interacción en frontend para el muro de comentarios, notas internas y avisos públicos, junto con los controles de intervención técnica sobre el terreno y reporte de resolución técnica:

1. **`IncidentCommentsSection.jsx` y `IncidentCommentsSection.css` (`frontend/src/components/Comments/`):**
   - **Visualización Segregada:** Muro cronológico de comentarios con distintivos visuales accesibles según visibilidad (`Aviso Público` con badge verde vs `Nota Interna` con badge ámbar e indicador de privacidad).
   - **Indicador de Edición:** Detecta automáticamente si `updatedAt` existe y difiere de `createdAt`, mostrando el distintivo textual `(Editado el [fecha])`.
   - **Edición en Línea Granular:** Habilitada exclusivamente para el autor original del comentario (`comment.authorId === currentUser?.id` o correspondencia de email) o usuarios con rol `ADMIN`, invocando `triageService.updateIncidentComment`. Control de estados de carga (`updating`) y cancelación sin recargas.
   - **Bloqueo en Estados Terminales:** Si la incidencia está en estado terminal (`CLOSED` o `REJECTED`, o mediante prop `isClosedOrRejected`), se bloquea el formulario mostrando una alerta informativa accesible: *"La incidencia está cerrada o rechazada. No se admiten nuevos comentarios."*
   - **Formulario de Publicación con Control RBAC:**
     * Solo visible y habilitado para personal municipal (`OPERATOR`, `TECHNICIAN`, `ADMIN`). Para usuarios ciudadanos (`CITIZEN`), se ofrece una nota explicativa de seguimiento sin controles de publicación.
     * Selector de visibilidad interna con casilla interactiva: *"Nota interna (solo visible para personal municipal)"*.
     * Invocación a `triageService.addIncidentComment` con emisión del callback `onCommentAdded`.
   - **Exportación Limpia:** Registrado y exportado en `frontend/src/components/Comments/index.js`.

2. **Modal y Controles de Intervención Técnica (`frontend/src/components/Triage/`):**
   - **`ResolveIncidentModal.jsx` y `ResolveIncidentModal.css`:**
     * Modal accesible WAI-ARIA (`role="dialog"`, `aria-modal="true"`, `aria-labelledby`, captura de tecla Escape, trampa/autofoco en botón de cierre y clic en backdrop).
     * Muestra resumen de la incidencia y badge de prioridad actual.
     * Validación en cliente de obligatoriedad: para prioridades no menores (`MEDIUM`, `HIGH`, `CRITICAL`), la memoria de actuaciones realizada es obligatoria y debe contar con al menos 10 caracteres (`MIN_RESOLUTION_NOTES_LENGTH = 10`), con contador en tiempo real y bloqueo del botón de envío. Para prioridad `LOW`, el campo es opcional.
     * Confirmación invocando `triageService.resolveIncident` con emisión de `onIncidentResolved(updatedIncident)`.
   - **`StartWorkButton.jsx`:**
     * Botón de acción para el inicio de trabajos sobre el terreno (`POST /api/incidents/:id/start-work`).
     * Habilitado dinámicamente cuando la incidencia está en estado `ASSIGNED` o `REOPENED`.
     * Manejo de estados de carga y errores de red.
   - **Exportación Centralizada:** Registrados en `frontend/src/components/Triage/index.js`.

3. **Integración en `IncidentDetailModal.jsx` y `MyIncidentsPage.jsx`:**
   - Inclusión de `IncidentCommentsSection` dentro del modal exhaustivo `IncidentDetailModal.jsx` para el seguimiento transparente tanto ciudadano como operativo.
   - Verificación de no regresión en `IncidentDetailModal.test.jsx` y `MyIncidentsPage.test.jsx`.

---

## 2. Cobertura de Pruebas Unitarias con Vitest

Se han desarrollado suites de pruebas específicas con Vitest y `@testing-library/react` asegurando el 100% de éxito en todos los escenarios:

1. **`IncidentCommentsSection.test.jsx` (10 tests):**
   - Renderizado diferenciado de avisos públicos y notas internas con sus badges correspondientes.
   - Muestra del indicador de edición para comentarios modificados.
   - Autorización de edición permitida al autor y denegada a terceros.
   - Autorización de edición concedida a usuarios `ADMIN` para cualquier comentario.
   - Bloqueo estricto del formulario y controles de edición en incidencias `CLOSED` o `REJECTED`.
   - Publicación exitosa de avisos públicos y notas internas por personal municipal.
   - Ocultación del formulario para rol `CITIZEN` con mensaje informativo.
   - Manejo de errores de carga y reintento.

2. **`ResolveIncidentModal.test.jsx` (7 tests):**
   - Renderizado condicional según bandera `isOpen` e incidencia nula.
   - Información de prioridad y directrices de obligatoriedad.
   - Validación reactiva del umbral mínimo de 10 caracteres para prioridades `MEDIUM`/`HIGH`/`CRITICAL`.
   - Habilitación directa para prioridad `LOW`.
   - Envío correcto a `triageService.resolveIncident` y ejecución de callbacks.
   - Visualización de errores de backend y cierre mediante teclado (Escape) y botones.

3. **`StartWorkButton.test.jsx` (4 tests):**
   - Habilitación condicional en estados `ASSIGNED` y `REOPENED`.
   - Deshabilitación en otros estados (`REPORTED`, `IN_PROGRESS`, etc.).
   - Invocación a `triageService.startWork` y propagación del evento.
   - Manejo de excepciones y errores operativos.

4. **Regresión Global:**
   - `IncidentDetailModal.test.jsx` (7 tests): pasa limpiamente.
   - `MyIncidentsPage.test.jsx` (8 tests): pasa limpiamente.
   - **Total Suite Frontend:** 16 archivos de prueba pasados, 138 tests exitosos (100% verde).
   - **Linter Frontend (`npm run lint`):** 0 errores, 0 advertencias.

---

## 3. Archivos Creados y Modificados

- **Creados:**
  - `frontend/src/components/Comments/IncidentCommentsSection.jsx`
  - `frontend/src/components/Comments/IncidentCommentsSection.css`
  - `frontend/src/components/Comments/IncidentCommentsSection.test.jsx`
  - `frontend/src/components/Comments/index.js`
  - `frontend/src/components/Triage/ResolveIncidentModal.jsx`
  - `frontend/src/components/Triage/ResolveIncidentModal.css`
  - `frontend/src/components/Triage/ResolveIncidentModal.test.jsx`
  - `frontend/src/components/Triage/StartWorkButton.jsx`
  - `frontend/src/components/Triage/StartWorkButton.test.jsx`
  - `docs/005-gestion-y-triaje-operativo-de-incidencias/07-comentarios-e-intervencion-frontend.md`

- **Modificados:**
  - `frontend/src/components/Triage/index.js`
  - `frontend/src/components/Map/IncidentDetailModal.jsx`
  - `frontend/src/components/Map/IncidentDetailModal.test.jsx`
  - `frontend/src/pages/MyIncidentsPage.test.jsx`
  - `features/005-gestion-y-triaje-operativo-de-incidencias/tasks.md`
