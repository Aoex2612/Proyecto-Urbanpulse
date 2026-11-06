# Tarea 9: Permisos Universales de Admin y Segregación de Incidencias para Técnicos

**Feature:** `005-gestion-y-triaje-operativo-de-incidencias`  
**Estado:** Completada ✅  
**Requisitos funcionales relacionados:** RF-13, RF-14, RF-15, RF-16, RF-25  

---

## 1. Resumen de la Tarea y Objetivos Operativos

En esta tarea se han implementado dos capacidades esenciales identificadas durante las pruebas operativas del sistema:

1. **Permisos Universales para Administradores en el Ciclo de Vida (RF-13, RF-14, RF-15, RF-16):**
   - Se habilita que usuarios con rol `ADMIN` puedan intervenir directamente sobre cualquier incidencia en estado `ASSIGNED` o `REOPENED` para iniciar trabajos (`startIntervention` &rarr; `IN_PROGRESS`) y resolverla (`resolveIncident` &rarr; `RESOLVED`), sin requerir figurar como técnico asignado en la tabla de asignaciones (`assignments` / `tareas`).
   - Se mantiene la validación estricta para usuarios con rol `TECHNICIAN`, quienes obligatoriamente deben contar con una asignación activa (`is_active = true`) vinculada a su identificador en dicha incidencia.

2. **Segregación de Incidencias en la Consulta para Técnicos en Campo (RF-25):**
   - Cuando un técnico (`TECHNICIAN`) consulta las incidencias activas a través de `GET /api/incidents`, el sistema segrega la información para devolver **única y exclusivamente** aquellas incidencias en las que el técnico posee una asignación técnica activa (`is_active = true`).
   - Para perfiles de supervisión y gestión (`OPERATOR`, `ADMIN`) o consultas ciudadanas y anónimas, el endpoint continúa devolviendo la totalidad de las incidencias activas en el término municipal (excluyendo únicamente estados finales `CLOSED` y `REJECTED`).

---

## 2. Modificaciones en Backend (Java 21 / Spring Boot)

### 2.1 Repositorio de Asignaciones (`TareasRepository.java`)
- Se añade la consulta derivada:
  ```java
  List<Tareas> findByTechnicianIdAndIsActiveTrue(Long technicianId);
  ```
  Permite consultar con alta eficiencia las asignaciones activas de un técnico para resolver las incidencias correspondientes.

### 2.2 Servicio de Incidencias (`IncidentService.java`)
- Se inyecta `TareasRepository` mediante `@Autowired(required = false)` manteniendo compatibilidad hacia atrás en los constructores existentes.
- Se implementa el método sobrecargado `getActiveIncidents(String userEmail)`:
  - Si el usuario existe y su rol es `RolUsuario.TECHNICIAN`:
    1. Recupera las tareas activas asociadas al ID del técnico (`findByTechnicianIdAndIsActiveTrue`).
    2. Extrae los IDs de las incidencias asociadas.
    3. Recupera y filtra dichas incidencias (excluyendo estados terminales `CLOSED` y `REJECTED`).
    4. Retorna la lista transformada a `IncidentResponse`.
  - Si el usuario tiene rol `ADMIN`, `OPERATOR`, `CITIZEN` o `userEmail` es nulo, delega en `getActiveIncidents()` devolviendo la totalidad de incidencias activas.

### 2.3 Controlador de Incidencias (`IncidentController.java`)
- El método `getActiveIncidents(Authentication authentication)` recibe el contexto de seguridad y extrae el identificador/correo del usuario (`authentication != null ? authentication.getName() : null`), delegando en `incidentService.getActiveIncidents(userEmail)`.

### 2.4 Controlador de Triaje (`IncidentTriageController.java`)
- En los endpoints de campo se amplía la autorización mediante `@PreAuthorize`:
  - `POST /api/incidents/{id}/start-work`: `@PreAuthorize("hasAnyRole('TECHNICIAN', 'ADMIN')")`
  - `POST /api/incidents/{id}/resolve`: `@PreAuthorize("hasAnyRole('TECHNICIAN', 'ADMIN')")`

### 2.5 Servicio de Triaje (`IncidentTriageService.java`)
- Los métodos `startIntervention` y `resolveIncident` invocan `validateTechnicianOrAdmin(userEmail)`, comprobando rol `ADMIN` o `TECHNICIAN`.
- En `validateTechnicianAssignment(incidentId, user)`: si el usuario es `ADMIN`, se omite la comprobación de asignación técnica previa, permitiendo la transición directa y sellando la auditoría inmutable con su identidad. Si es `TECHNICIAN`, se exige que figure en `tareasRepository.existsByIncidentIdAndTechnicianIdAndIsActiveTrue`.

---

## 3. Modificaciones en Frontend (React 19 / Vite)

### 3.1 Enrutamiento Principal (`frontend/src/App.jsx`)
- Se actualiza la ruta `/operator-panel` para autorizar explícitamente al rol `TECHNICIAN`:
  ```jsx
  <Route
    path="/operator-panel"
    element={
      <ProtectedRoute allowedRoles={['OPERATOR', 'ADMIN', 'TECHNICIAN']}>
        <OperatorPanelPage />
      </ProtectedRoute>
    }
  />
  ```

### 3.2 Portal Ciudadano / Dashboard (`frontend/src/pages/HomePage.jsx`)
- Se habilita el acceso al panel operativo si el usuario posee rol `TECHNICIAN`, `OPERATOR` o `ADMIN` (`authService.hasAnyRole(['OPERATOR', 'ADMIN', 'TECHNICIAN'])`).
- Si el usuario es `TECHNICIAN`, el enlace de cabecera se etiqueta dinámicamente como **"Mis Asignaciones"** y la tarjeta destacada presenta el título **"Mis Asignaciones Técnicas"** con llamada a la acción contextualizada.

### 3.3 Panel de Operaciones (`frontend/src/pages/OperatorPanelPage.jsx`)
- Detección reactiva de roles mediante `authService.getUser()`.
- **Adaptación visual para `TECHNICIAN`:**
  - Encabezado principal: "Mis Asignaciones Técnicas".
  - Subtítulo: "Incidencias asignadas para intervención y resolución técnica en campo."
  - Badge de rol: "Técnico de Campo".
  - Estadísticas específicas de trabajo en campo: "Pendientes de Inicio", "En Intervención", "Resueltas" y "Total Asignadas".
  - Ocultación estricta de acciones no pertinentes (Validar, Rechazar, Reevaluar, Asignar Departamentos/Técnicos, Cerrar, Reabrir, y botón Asistente de Prioridad).
  - Disponibilidad de botones de acción en campo: "Iniciar Trabajo" (`ASSIGNED` o `REOPENED`) y "Resolver" (`IN_PROGRESS`), además del botón de "💬 Comentarios".
- **Habilitación para `ADMIN`:**
  - Muestra "Iniciar Trabajo" y "Resolver" en cualquier incidencia en el estado correspondiente, junto con el set completo de acciones de triaje y gestión departamental.
- **Segregación para `OPERATOR`:**
  - Mantiene las acciones exclusivas de triaje, asignación y reevaluación sin mostrar los botones de intervención física en campo.

---

## 4. Verificación y Resultados de Pruebas

### 4.1 Pruebas de Frontend (Vitest & Testing Library)
- **`App.test.jsx`:** 5 pruebas validando el acceso a `/operator-panel` para `TECHNICIAN`, `OPERATOR` y `ADMIN`, y la redirección/bloqueo para `CITIZEN` y usuarios no autenticados.
- **`HomePage.test.jsx`:** 4 pruebas comprobando la renderización de enlaces y tarjetas según el rol.
- **`OperatorPanelPage.test.jsx`:** 14 pruebas que incluyen la adaptación reactiva del panel según rol (`TECHNICIAN`, `OPERATOR`, `ADMIN`), ocultación de acciones indebidas y ejecución de acciones contextuales.
- **Total Frontend:** 20 suites de pruebas en verde, 167 tests superados (100% éxito) y 0 errores de ESLint (`npm run lint`).

### 4.2 Pruebas de Backend (JUnit 5, Mockito & MockMvc)
- **`TareasRepositoryTest`:** Verificación con persistencia `@DataJpaTest` del método `findByTechnicianIdAndIsActiveTrue`.
- **`IncidentTriageServiceTest`:** Pruebas de inicio de trabajo y resolución técnica para `ADMIN` sin asignación previa, y validación de `TECHNICIAN` asignado vs 403 Forbidden para no asignado.
- **`IncidentTriageControllerTest`:** Pruebas de autorización con `@WithMockUser` verificando respuestas 200 OK en `/start-work` y `/resolve` para roles `TECHNICIAN` y `ADMIN`, y 403 Forbidden para `OPERATOR`.
- **`IncidentServiceTest`:** Pruebas del método sobrecargado `getActiveIncidents(userEmail)` validando la segregación de avisos para técnicos y retorno completo para operadores y administradores.
- **`IncidentControllerTest`:** Verificación de paso de identidad autenticada al servicio.
