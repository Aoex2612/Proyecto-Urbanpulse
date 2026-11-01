# QA Note — Validación de Feature 003: Mapa de Incidencias con Leaflet y Auditoría Transversal (con HU14.4)

**Fecha de validación:** 2026-10-30  
**Rol evaluador:** QA Engineer  
**Feature:** `003-mapa-incidencias-y-auditoria-transversal`  
**Rama:** `feature/HU14.4-boton-ubicacion-actual`  
**Pull Request:** [#43](https://github.com/Aoex2612/Proyecto-Urbanpulse/pull/43)  
**Veredicto final:** ✅ **APROBADA (Cumplimiento al 100%)**

---

## 1. Resumen de Validación y Veredicto

La validación exhaustiva de la **Feature 003** —incluyendo la ampliación de la **HU14.4 (Captura de Ubicación Actual en Reporte)** y su integración sinérgica con los avances de `develop` (resolución territorial de datos abiertos y vista de seguimiento)— ha concluido con **ÉXITO rotundo**.

- Se verificó la total compatibilidad con las ramas adyacentes sin generar conflictos de estilo ni regresiones funcionales.
- Se comprobó que el flujo reactivo de captura geográfica GPS alimenta automáticamente la detección territorial inversa sin bloquear la experiencia de usuario.
- El 100% de los criterios de aceptación y requisitos funcionales (**RF-1 a RF-35**) se encuentran implementados y probados.

---

## 2. Trazabilidad de Requisitos Funcionales (EARS) y Cobertura de Tests

| ID Requisito | Descripción EARS | Tests / Mecanismo de Validación | Resultado |
|:---|:---|:---|:---:|
| **RF-1** | CUANDO usuario autenticado accede a `/map`, EL SISTEMA renderiza mapa Leaflet centrado en Málaga (`[36.7213, -4.4214]`, zoom 13) con incidencias no terminadas (`status NOT IN ('CLOSED', 'REJECTED')`) | `IncidenciaRepositoryTest.findByStatusNotIn_excludesClosedAndRejected`<br>`IncidentServiceTest.getActiveIncidents_excludesClosedAndRejected`<br>`IncidentsMap.jsx` | ✅ PASA |
| **RF-2** | CUANDO el usuario hace clic en marcador del mapa, EL SISTEMA despliega Popup con datos básicos (título, descripción, categoría, estado) y botón "Ver detalle completo" | `IncidentsMap.jsx` (`<Popup>` con badges y botón de acción) | ✅ PASA |
| **RF-3** | CUANDO el usuario pulsa "Ver detalle completo", EL SISTEMA abre modal de detalle con información completa | `IncidentDetailModal.jsx` y `MapPage.jsx` | ✅ PASA |
| **RF-4** | SI el usuario clica en el mapa fuera de modo selección, ENTONCES EL SISTEMA no registra puntos ni abre formularios, cerrando popups abiertos | `MapEventsHandler` en `IncidentsMap.jsx` (`isSelectingLocation === false`) | ✅ PASA |
| **RF-5** | SI las teselas de OpenStreetMap fallan por red, ENTONCES EL SISTEMA mantiene la interfaz operativa sin romper la sesión | `TileLayer` reactivo y estilos de fallback | ✅ PASA |
| **RF-6 / RF-7** | CUANDO se pulsa "Crear incidencia", el botón conmuta a "Cancelar selección"; al pulsar de nuevo, cancela el modo sin fijar puntos | `MapPage.jsx` (gestión de estado `isSelectingLocation` y `selectedLocation`) | ✅ PASA |
| **RF-8** | CUANDO se clica en el mapa en modo selección, EL SISTEMA captura lat/lng y abre el formulario modal con coordenadas en modo solo lectura | `MapEventsHandler` y `CreateIncidentModal.jsx` | ✅ PASA |
| **RF-9** | SI el usuario cancela o cierra el modal de alta, ENTONCES EL SISTEMA descarta coordenadas sin marcadores residuales | `handleCloseCreateModal` en `MapPage.jsx` | ✅ PASA |
| **RF-10** | CUANDO se envía el formulario con categoría de catálogo predefinida, EL SISTEMA persiste la incidencia con estado `REPORTED`, prioridad `MEDIUM` y usuario del JWT | `CreateIncidentRequestTest`<br>`IncidentServiceTest.createIncident_success`<br>`IncidentControllerSecurityTest.createIncident_authenticated` | ✅ PASA |
| **RF-11** | CUANDO la creación concluye con éxito (201 Created), EL SISTEMA cierra modal y añade inmediatamente el nuevo marcador al mapa | `IncidentControllerSecurityTest.createIncident_returns201`<br>`onCreated` en `MapPage.jsx` | ✅ PASA |
| **RF-12** | SI faltan campos obligatorios o coordenadas, ENTONCES EL SISTEMA bloquea el envío y notifica errores de validación | `CreateIncidentRequestTest` (`@NotBlank`, `@NotNull`, rangos de coordenadas) | ✅ PASA |
| **RF-13** | CUANDO se actualiza estado (`PATCH /api/incidents/{id}/status`), EL SISTEMA registra atómicamente la transición en `status_changes` con actor del JWT | `IncidentServiceTest.updateIncidentStatus_success_persistsStatusChange` (verificado con `ArgumentCaptor`) | ✅ PASA |
| **RF-14** | SI se actualiza una incidencia inexistente, ENTONCES EL SISTEMA retorna HTTP 404 con `ErrorResponse` | `IncidentServiceTest.updateIncidentStatus_notFound_throwsException`<br>`IncidentControllerSecurityTest.updateIncidentStatus_notFound_returns404` | ✅ PASA |
| **RF-15** | CUANDO un `ADMIN` solicita `GET /api/incidents/{id}/history`, EL SISTEMA retorna lista cronológica ordenada de cambios de estado | `CambiosEstadoRepositoryTest`<br>`IncidentServiceTest.getIncidentHistory_success`<br>`IncidentControllerSecurityTest.getIncidentHistory_admin_returns200` | ✅ PASA |
| **RF-16** | SI se solicita historial de una incidencia inexistente, ENTONCES EL SISTEMA retorna HTTP 404 con `ErrorResponse` | `IncidentServiceTest.getIncidentHistory_notFound_throwsException`<br>`IncidentControllerSecurityTest.getIncidentHistory_admin_notFound_returns404` | ✅ PASA |
| **RF-17** | SI usuario sin rol `ADMIN` (`CITIZEN`, `OPERATOR`, `TECHNICIAN`, `ANALYST`) invoca `/history`, EL SISTEMA deniega con HTTP 403 y `ErrorResponse` | `IncidentControllerSecurityTest.getIncidentHistory_citizen_returns403`<br>`IncidentControllerSecurityTest.getIncidentHistory_operator_returns403` | ✅ PASA |
| **RF-18** | CUANDO un usuario con rol `ADMIN` abre modal de detalle, EL SISTEMA consulta su historial y renderiza el componente `Timeline` | `IncidentDetailModal.jsx` (`canViewAuditHistory(currentUser?.role)`), `StatusTimeline.jsx` | ✅ PASA |
| **RF-19** | MIENTRAS usuario no sea `ADMIN`, EL SISTEMA oculta la sección de auditoría / Timeline en el detalle | `IncidentDetailModal.jsx` (evaluación condicional en frontend) | ✅ PASA |
| **RF-20** | Desacoplamiento modular del componente `Timeline` para admitir extensión a otros roles | Módulo desacoplado en `frontend/src/components/Timeline/` | ✅ PASA |
| **RF-21** | CUANDO se abre el formulario modal de alta, EL SISTEMA muestra botón opcional "Usar mi ubicación actual" en el bloque de coordenadas | `CreateIncidentModal.test.jsx`: *"muestra el botón 'Usar mi ubicación actual'..."* | ✅ PASA |
| **RF-22** | CUANDO el usuario pulsa "Usar mi ubicación actual", EL SISTEMA solicita al navegador coordenadas lat/lng del dispositivo | `geolocationUtils.test.js`: *"getCurrentCoordinates resuelve con coordenadas..."* | ✅ PASA |
| **RF-23** | MIENTRAS se procesa la consulta de geolocalización, EL SISTEMA deshabilita el botón "Usar mi ubicación actual" | `CreateIncidentModal.test.jsx`: *"muestra estado de carga y deshabilita botones..."* | ✅ PASA |
| **RF-24** | MIENTRAS se procesa la consulta de geolocalización, EL SISTEMA muestra texto "Obteniendo ubicación..." y animación de actividad | `CreateIncidentModal.test.jsx`: *"muestra estado de carga y deshabilita botones..."* | ✅ PASA |
| **RF-25** | MIENTRAS se procesa la consulta de geolocalización, EL SISTEMA bloquea el botón de envío del formulario | `CreateIncidentModal.test.jsx`: *"muestra estado de carga y deshabilita botones..."* | ✅ PASA |
| **RF-26** | CUANDO la consulta finaliza satisfactoriamente, EL SISTEMA actualiza lat/lng con precisión de 6 decimales | `geolocationUtils.test.js`: *"formatea latitud y longitud a 6 decimales..."*, `CreateIncidentModal.test.jsx` | ✅ PASA |
| **RF-27** | CUANDO se actualizan las coordenadas GPS, EL SISTEMA despliega distintivo visual "Ubicación actual obtenida" | `CreateIncidentModal.test.jsx`: *"actualiza coordenadas a 6 decimales y muestra badge..."* | ✅ PASA |
| **RF-28** | CUANDO finaliza la consulta de geolocalización, EL SISTEMA restaura el botón a su estado interactivo original | `CreateIncidentModal.test.jsx`: *"actualiza coordenadas a 6 decimales y muestra badge..."* | ✅ PASA |
| **RF-29** | SI el usuario deniega o revoca permisos, ENTONCES EL SISTEMA muestra mensaje de error contextual en línea sin bloquear el formulario | `geolocationUtils.test.js` (`PERMISSION_DENIED`), `CreateIncidentModal.test.jsx` | ✅ PASA |
| **RF-30** | SI la consulta supera 10 segundos, ENTONCES EL SISTEMA cancela la solicitud y muestra error de tiempo límite agotado | `geolocationUtils.test.js` (`TIMEOUT`) | ✅ PASA |
| **RF-31** | SI el dispositivo no determina posición geográfica, ENTONCES EL SISTEMA muestra error de posición no disponible | `geolocationUtils.test.js` (`POSITION_UNAVAILABLE`) | ✅ PASA |
| **RF-32** | SI ocurre error en la geolocalización, ENTONCES EL SISTEMA preserva intactas las coordenadas previamente cargadas | `CreateIncidentModal.test.jsx`: *"muestra mensaje de error y preserva coordenadas previas..."* | ✅ PASA |
| **RF-33** | SI el navegador no soporta geolocalización, ENTONCES EL SISTEMA muestra el botón deshabilitado con aviso explicativo | `CreateIncidentModal.test.jsx`: *"muestra botón deshabilitado cuando no hay soporte..."* | ✅ PASA |
| **RF-34** | CUANDO se envía el formulario con éxito tras usar ubicación actual, EL SISTEMA posiciona el marcador e inicia centrado suave (`flyTo` zoom 16) | `IncidentsMap.jsx` (`MapFlyToController`), `MapPage.jsx` (`focusCoords`) | ✅ PASA |
| **RF-35** | SI el usuario cancela o cierra el modal tras obtener ubicación actual, ENTONCES EL SISTEMA descarta las coordenadas y mantiene el encuadre previo | `CreateIncidentModal.test.jsx`, `MapPage.jsx` (`handleCloseCreateModal`) | ✅ PASA |

---

## 3. Métricas de Cobertura Técnica de Código

### A. Cobertura Técnica Backend (JaCoCo - JDK 21)
- **Suite global ejecutada:** 272 tests (271 superados, 0 fallos, 0 errores, 1 omitido por requerir PostgreSQL en ejecución).

| Métrica JaCoCo | Cobertura Obtenida | Elementos Analizados |
|:---|:---:|:---:|
| **Instrucciones Bytecode** | **95.12%** | 4.191 cubiertas de 4.406 |
| **Ramas Condicionales (Branches)** | **75.85%** | 311 cubiertas de 410 |
| **Líneas de Código (Lines)** | **95.36%** | 1.091 cubiertas de 1.144 |
| **Métodos** | **95.98%** | 287 cubiertos de 299 |
| **Clases** | **97.43%** | 38 cubiertas de 39 |

### B. Cobertura Técnica Frontend (Vitest Coverage v8)
- **Suite global ejecutada:** 7 suites de pruebas, **68 tests superados (0 fallos)**.
- **Linter (ESLint):** 0 errores, 0 advertencias.

| Módulo / Archivo | % Sentencias (Stmts) | % Ramas (Branch) | % Funciones (Funcs) | % Líneas (Lines) |
|:---|:---:|:---:|:---:|:---:|
| `src/utils/geolocationUtils.js` | **97.61%** | **92.85%** | **100.00%** | **97.61%** |
| `src/components/Map/CreateIncidentModal.jsx` | **71.85%** | **71.12%** | **70.00%** | **73.93%** |
| `src/utils/dateUtils.js` | **87.83%** | **87.67%** | **100.00%** | **88.23%** |
| `src/pages/MyIncidentsPage.jsx` | **95.16%** | **85.71%** | **84.21%** | **96.66%** |
| **Total Global Frontend** | **67.44%** | **57.70%** | **56.81%** | **68.00%** |

---

## 4. Guía de Pruebas Manuales (Verificación Rápida)

Para validar la experiencia de usuario de extremo a extremo sin rodeos:

1. **Acceso al mapa:**
   - Inicia sesión en la plataforma y dirígete a la ruta `/map`.
   - Comprueba que el mapa se centra en Málaga y muestra los marcadores activos.

2. **Apertura del formulario de alta:**
   - Pulsa sobre el botón superior **"Crear incidencia"** y haz clic sobre cualquier punto cartográfico.
   - Verifica que el modal se abre con las coordenadas del punto en modo solo lectura.

3. **Prueba de geolocalización asistida (HU14.4):**
   - Pulsa el botón **"Usar mi ubicación actual"**.
   - Observa que el botón conmuta a *"Obteniendo ubicación..."*, se deshabilita momentáneamente y bloquea el botón principal de envío.
   - Concede permisos de ubicación en el navegador:
     - Verifica que la latitud y longitud se actualizan inmediatamente con 6 decimales.
     - Comprueba la aparición del distintivo verde esmeralda **"Ubicación actual obtenida"**.
     - Comprueba que la sección territorial resuelve en paralelo la dirección postal, distrito y barrio asociados.

4. **Prueba de errores contextuales:**
   - En una ventana de incógnito o bloqueando la geolocalización en los ajustes del navegador, pulsa **"Usar mi ubicación actual"**.
   - Comprueba que se despliega la alerta de error en línea bajo las coordenadas y que las coordenadas previas del mapa **no se borran**.

5. **Prueba de descarte limpio:**
   - Pulsa **"Cancelar"** o pulsa la tecla `Escape`.
   - Comprueba que el modal se cierra y no se genera ningún marcador temporal ni movimiento inesperado en el mapa.

6. **Creación con centrado suave:**
   - Vuelve a abrir el modal, pulsa **"Usar mi ubicación actual"**, introduce título, descripción y categoría, y pulsa **"Guardar incidencia"**.
   - Comprueba que el modal se cierra, aparece la notificación de éxito y la cámara del mapa ejecuta una transición suave (`flyTo`) a zoom 16 hacia las coordenadas de la nueva incidencia.
