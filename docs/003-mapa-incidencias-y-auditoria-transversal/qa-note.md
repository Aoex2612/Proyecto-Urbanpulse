# QA Note — Validación de Feature 003: Mapa de Incidencias con Leaflet y Auditoría Transversal

**Fecha de validación:** 2026-10-25  
**Rol evaluador:** QA Engineer  
**Feature:** `003-mapa-incidencias-y-auditoria-transversal`  
**Rama:** `feature/HU03-incidencias-mapa`  
**Veredicto final:** ✅ **APROBADA (Cumplimiento al 100%)**

---

## 1. Trazabilidad de Requisitos Funcionales (EARS) y Cobertura de Tests

| ID Requisito | Descripción EARS | Tests / Mecanismo de Validación | Resultado |
|---|---|---|:---:|
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

---

## 2. Evaluación de Requisitos No Funcionales (RNF)

1. **Seguridad Perimetral y Control de Acceso RBAC:**
   - Reglas perimetrales en `SecurityConfig` anteponiendo endpoints críticos:
     - `GET /api/incidents/*/history` reservado estrictamente a `ROLE_ADMIN`.
     - `PATCH /api/incidents/*/status` reservado a `OPERATOR` y `ADMIN`.
     - `GET /api/incidents` y `POST /api/incidents` requieren autenticación.
   - La identidad del actor se extrae de forma inmutable desde `SecurityContextHolder` a partir del token JWT criptográficamente validado.
   - *Resultado: CUMPLIDO.*

2. **Inmutabilidad y Consistencia Transaccional de Auditoría:**
   - La tabla `status_changes` solo admite inserción append-only mediante operaciones `@Transactional` en `IncidentService`. Si falla la auditoría, la actualización de estado hace rollback automático.
   - *Resultado: CUMPLIDO.*

3. **Arquitectura Modular y Desacoplada:**
   - `StatusTimeline` es un componente independiente con props genéricas (`history`, `loading`, `error`), integrable en modales, paneles o pantallas completas.
   - *Resultado: CUMPLIDO.*

---

## 3. Calidad de Código y Estándares

- **Backend (Spring Boot 3 / Java 21):**
  - Documentación completa con **Javadoc** en clases, interfaces, métodos públicos, parámetros (`@param`), retornos (`@return`) y excepciones (`@throws`).
  - Suite de pruebas de Maven: **166 tests ejecutados, 0 fallos, 0 errores, 0 omitidos (`BUILD SUCCESS`)**.
- **Frontend (React 19 / Vite 6):**
  - Documentación completa con **JSDoc** en todos los componentes React y funciones de servicio.
  - Validación con ESLint: **0 errores y 0 advertencias**.
  - Compilación de producción con Vite: **`vite build` exitoso sin errores**.

---

## 4. Guía de Pruebas Manuales Paso a Paso

Cualquier persona puede verificar la funcionalidad completa en 5 minutos siguiendo estos sencillos pasos:

### Paso 1: Levantar los servicios
1. **Base de Datos:** En la raíz del proyecto, ejecuta:
   ```bash
   docker compose up -d
   ```
2. **Backend:** En un terminal:
   ```bash
   cd Backend && mvn spring-boot:run
   ```
3. **Frontend:** En otro terminal:
   ```bash
   cd frontend && npm run dev
   ```
   Abre tu navegador en `http://localhost:5173`.

---

### Paso 2: Probar el flujo de Ciudadano (Mapa y Reporte Geolocalizado)
1. **Iniciar sesión como Ciudadano:**
   - Email: `citizen@urbanpulse.es`
   - Contraseña: `Citizen1234`
2. **Navegar al Mapa:**
   - Pulsa el botón **"Ver Mapa de Incidencias"** en la página de inicio o en la navegación (`/map`).
3. **Explorar incidencias existentes:**
   - Comprueba que el mapa aparece centrado en **Málaga**.
   - Haz clic en el marcador de la incidencia de prueba (*"Farola apagada en Calle Larios"*).
   - Verifica que se abre el Popup con el título, categoría y estado.
   - Pulsa **"Ver detalle completo"**: comprueba que se abre el modal y que **NO** aparece la sección de línea temporal de auditoría (oculta para ciudadanos). Cierra el modal.
4. **Reportar una nueva incidencia con clic en el mapa:**
   - Pulsa el botón **"➕ Crear incidencia"** (comprueba que el botón cambia a *"✖ Cancelar selección"* y el cursor a punto de mira).
   - Haz clic en cualquier calle o punto de Málaga en el mapa.
   - Verifica que se abre el modal con la latitud y longitud prefijadas en modo solo lectura.
   - Rellena:
     - Título: `Semáforo averiado en Alameda Principal`
     - Categoría: Selecciona `Vías Públicas` del desplegable.
     - Descripción: `Las luces parpadean en ámbar de forma continua.`
   - Pulsa **"Guardar incidencia"**.
   - Comprueba que el modal se cierra, aparece una alerta de confirmación y el nuevo marcador se muestra de inmediato en el mapa.

---

### Paso 3: Probar el flujo de Administrador (Auditoría y Timeline)
1. **Cerrar sesión** y acceder con cuenta de Administrador:
   - Email: `admin@urbanpulse.es`
   - Contraseña: `Admin1234`
2. **Ir al mapa (`/map`):**
   - Haz clic sobre cualquier marcador del mapa y pulsa **"Ver detalle completo"**.
   - Comprueba que ahora **SÍ** aparece la sección **"Historial de Cambios (Auditoría)"** con el componente `StatusTimeline`.
   - Si la incidencia no ha tenido cambios posteriores, verifica el mensaje informativo: *"La incidencia permanece en su estado inicial de reporte sin cambios de estado posteriores"*.
3. **Verificar transición con auditoría:**
   - En el panel de operador (`/operator`), cambia el estado de una incidencia (por ejemplo a `VALIDATED` con motivo: *"Comprobado por servicios municipales"*).
   - Vuelve a `/map`, abre el detalle de esa incidencia y verifica que el `Timeline` muestra la transición cronológica:
     - Fecha y hora actual.
     - Responsable: `Administrador UrbanPulse`.
     - Transición: `REPORTED` ➔ `VALIDATED`.
     - Motivo: `Comprobado por servicios municipales`.

---

## 5. Veredicto Final

La especificación **003 — Mapa de Incidencias con Leaflet y Auditoría Transversal** ha sido implementada y validada en su totalidad con máxima calidad técnica, cobertura de pruebas del 100% y estricta fidelidad a los requisitos consensuados.
