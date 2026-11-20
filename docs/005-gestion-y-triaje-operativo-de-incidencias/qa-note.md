# QA Note — Feature 005: Gestión y Triaje Operativo de Incidencias

**Estado de Validación:** ✅ VALIDADA CON ÉXITO (`sdd-validate` APROBADO)  
**Fecha de validación:** 2026-11-06  
**Rama de desarrollo:** `feature/Feature-005-Gestion-y-Triaje-Operativo-de-Incidencias`  
**Referencia arquitectónica:** [ADR-07: Gestión del Ciclo de Vida y Asignación Multidepartamental](file:///c:/Users/felix/Desktop/Desarrollo_en_la_nube/Proyecto-Urbanpulse/docs/ADR/ADR-07-gestion-ciclo-vida-asignacion-multidepartamental.md)  
**Especificación funcional:** [spec.md](file:///c:/Users/felix/Desktop/Desarrollo_en_la_nube/Proyecto-Urbanpulse/features/005-gestion-y-triaje-operativo-de-incidencias/spec.md)  
**Plan técnico:** [plan.md](file:///c:/Users/felix/Desktop/Desarrollo_en_la_nube/Proyecto-Urbanpulse/features/005-gestion-y-triaje-operativo-de-incidencias/plan.md)  
**Desglose de tareas:** [tasks.md](file:///c:/Users/felix/Desktop/Desarrollo_en_la_nube/Proyecto-Urbanpulse/features/005-gestion-y-triaje-operativo-de-incidencias/tasks.md)  

---

## 1. Resumen Global de la Feature 005

La Feature **005 — Gestión y Triaje Operativo de Incidencias** implementa el flujo operacional completo de supervisión municipal, triaje, asignación departamental multidisciplinar, intervención técnica y trazabilidad auditable en UrbanPulse.

Cubre de forma exhaustiva las siguientes capacidades:
- **HU07 (Validación o Rechazo de Reportes):** Operadores y administradores evalúan reportes iniciales (`REPORTED`), validándolos formalmente (`VALIDATED`) o rechazándolos con motivo justificado (`REJECTED`, mínimo 5 caracteres) e inactivación de asignaciones. Permite además la reevaluación rectificativa de expedientes rechazados.
- **HU08 (Asignación Multidepartamental y Técnica):** Coordinación multidisciplinar permitiendo asignar simultáneamente múltiples áreas municipales (e.g., Alumbrado, Parques, Obras) y técnicos especialistas adscritos a dichas áreas, con reversión automática a `VALIDATED` si no restan departamentos activos.
- **HU09 (Intervención Técnica y Resolución):** Técnicos asignados o administradores inician la actuación de campo (`IN_PROGRESS`) y declaran la resolución (`RESOLVED`) aportando memoria descriptiva obligatoria (mínimo 10 caracteres) para prioridades medias, altas y críticas.
- **HU10 (Reapertura o Cierre Definitivo):** Supervisores cierran formalmente el expediente (`CLOSED`) con inactivación automática de asignaciones o lo reabren (`REOPENED`) con motivo obligatorio si las actuaciones resultaron insuficientes.
- **HU11 (Muro de Comentarios y Notas Técnicas):** Historial colaborativo de intervenciones con segregación estricta RBAC entre notas técnicas internas (`is_internal = true`, restringidas a personal municipal) y avisos públicos para la ciudadanía (`is_internal = false`), sellado temporal en ediciones (`updated_at`) y bloqueo en expedientes terminales.
- **HU12 (Priorización Asistida y Manual):** Motor determinista puro de recomendación de prioridad basado en corte de vía y nivel de riesgo para la seguridad, con justificación textual explicativa automática y confirmación por el operador.
- **Permisos Universales para ADMIN y Vista Segregada para TECHNICIAN (RF-25):** El rol `ADMIN` ostenta facultades operativas universales sin requerir autoasignación previa, mientras que los usuarios `TECHNICIAN` disponen de un panel especializado enfocado exclusivamente en sus asignaciones activas (`is_active = true`).

---

## 2. Cuentas y Contraseñas de Prueba Preconfiguradas (Base de Datos)

Credenciales sembradas de forma predeterminada en PostgreSQL (`database/init/diagramUrbanPulse.sql`):

| Rol | Correo Electrónico | Contraseña *(case-sensitive)* | Departamento / Área | Capacidades Operativas a Probar |
|:---|:---|:---|:---|:---|
| **ADMIN** | `admin@urbanpulse.es` | `Admin1234` | Supervisión Global | **Permisos universales:** Visualiza todas las incidencias. Puede iniciar trabajo (`start-work`), resolver (`resolve`), validar, rechazar, reevaluar, asignar, cerrar y reabrir de manera directa sin requerir asignación previa como técnico. |
| **TECHNICIAN** *(Técnico Vías)* | `tech.obras1@urbanpulse.es` | `Operator1234` | Infraestructuras y Obras Públicas (Dpto 1) | **Vista segregada:** Accede a *"Mis Asignaciones"*. Únicamente ve las incidencias asignadas a su identificador con `is_active = true`. Puede iniciar trabajo (`IN_PROGRESS`), resolver (`RESOLVED`) con memoria técnica y comentar. |
| **TECHNICIAN** *(Técnico Alumbrado)* | `tech.obras2@urbanpulse.es` | `Operator1234` | Infraestructuras y Obras Públicas (Dpto 1) | Idem técnico de vías, adscrito a instalaciones de alumbrado y electricidad urbana. |
| **TECHNICIAN** *(Técnica Residuos)* | `tech.limpieza1@urbanpulse.es` | `Operator1234` | Medio Ambiente y Limpieza (Dpto 2) | Idem, adscrita a limpieza y gestión de residuos. |
| **TECHNICIAN** *(Técnico Tráfico)* | `tech.movilidad1@urbanpulse.es` | `Operator1234` | Movilidad y Tráfico (Dpto 3) | Idem, adscrito a señalización y red semafórica. |
| **OPERATOR** | `operator@urbanpulse.es` | `Operator1234` | Infraestructuras y Obras Públicas (Dpto 1) | **Gestor de triaje:** Validar reportes, rechazar con motivo (&ge; 5 car.), calcular prioridad asistida con modal, asignar departamentos y técnicos, reabrir y cerrar expedientes. |
| **CITIZEN** | `citizen@urbanpulse.es` | `Citizen1234` | Ciudadanía | Consulta cartográfica pública y portal `/my-incidents`. **Segregación:** Visualiza exclusivamente avisos públicos (`is_internal = false`), sin acceso a notas técnicas internas. |

---

## 3. Tabla de Trazabilidad de Requisitos Funcionales (EARS) y Tests

| RF | Requisito Funcional (EARS) | Componentes de Implementación | Tests de Verificación | Estado |
|:---|:---|:---|:---|:---:|
| **RF-1** | Cálculo determinista de prioridad asistida según matriz (vía y seguridad) con justificación textual explicativa. | `PriorityService`, `PriorityCalculationRequest`, `PriorityCalculationResponse` | `PriorityServiceTest` (9 tests), `PriorityAssistantModal.test.jsx` | ✅ PASS |
| **RF-2** | Fijación y actualización de prioridad operativa definitiva (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`) y `priority_reason`. | `IncidentTriageService.updatePriority`, `UpdatePriorityRequest`, `Incidencia` | `IncidentTriageServiceTest`, `IncidentTriageControllerTest`, `PriorityAssistantModal.test.jsx` | ✅ PASS |
| **RF-3** | Restricción RBAC: Denegación HTTP 403 en cálculo o fijación de prioridad a usuarios sin rol `OPERATOR` ni `ADMIN`. | `IncidentTriageController`, `SecurityConfig` (`@PreAuthorize`) | `IncidentTriageControllerTest` | ✅ PASS |
| **RF-4** | Validación formal de reportes (`REPORTED` &rarr; `VALIDATED`) con registro inmutable en `CambiosEstado`. | `IncidentTriageService.validateIncident`, `CambiosEstadoRepository` | `IncidentTriageServiceTest`, `OperatorPanelPage.test.jsx` | ✅ PASS |
| **RF-5** | Rechazo de reporte con motivo &ge; 5 caracteres, transición a `REJECTED`, inactivación de asignaciones y auditoría. | `IncidentTriageService.rejectIncident`, `AssignmentService.deactivateAllActiveAssignments` | `IncidentTriageServiceTest`, `AssignmentServiceTest`, `ReasonModal.test.jsx`, `OperatorPanelPage.test.jsx` | ✅ PASS |
| **RF-6** | Bloqueo por validación al intentar rechazar sin motivo o con menos de 5 caracteres útiles. | `RejectIncidentRequest` (`@Size(min=5)`), `IncidentTriageService` | `IncidentTriageServiceTest`, `DtoValidationTest`, `ReasonModal.test.jsx` | ✅ PASS |
| **RF-7** | Reevaluación de incidencia rechazada devolviéndola a `REPORTED` o `VALIDATED` con motivo justificado. | `IncidentTriageService.reevaluateIncident`, `ReevaluateIncidentRequest` | `IncidentTriageServiceTest`, `ReasonModal.test.jsx`, `OperatorPanelPage.test.jsx` | ✅ PASS |
| **RF-8** | Asignación multidepartamental en incidencias `VALIDATED` o `REOPENED` transicionando a `ASSIGNED`. | `AssignmentService.assignDepartmentsAndTechnicians`, `TareasRepository` | `AssignmentServiceTest`, `AssignmentModal.test.jsx`, `OperatorPanelPage.test.jsx` | ✅ PASS |
| **RF-9** | Asignación de técnicos adscritos estrictamente a departamentos asignados a la incidencia. | `AssignmentService`, `UserRepository.findByRoleAndDepartmentId` | `AssignmentServiceTest`, `AssignmentModal.test.jsx` | ✅ PASS |
| **RF-10** | Denegación atómica si un técnico no pertenece a ninguno de los departamentos asignados activos. | `AssignmentService.validateTechniciansBelongToActiveDepartments` | `AssignmentServiceTest`, `AssignmentModal.test.jsx` | ✅ PASS |
| **RF-11** | Desactivación lógica de asignaciones departamentales y técnicas (`is_active = false`). | `AssignmentService.deactivateAssignments`, `Tareas` | `AssignmentServiceTest`, `TareasRepositoryTest`, `AssignmentModal.test.jsx` | ✅ PASS |
| **RF-12** | Reversión automática de estado de `ASSIGNED` a `VALIDATED` si no restan departamentos activos tras desasignar. | `AssignmentService.checkAndRevertStatusIfNoActiveDepartments` | `AssignmentServiceTest`, `OperatorPanelPage.test.jsx` | ✅ PASS |
| **RF-13** | Inicio de trabajos técnicos (`ASSIGNED`/`REOPENED` &rarr; `IN_PROGRESS`) por técnico asignado activo o `ADMIN`. | `IncidentTriageService.startIntervention`, `StartWorkButton`, `OperatorPanelPage` | `IncidentTriageServiceTest`, `IncidentTriageControllerTest`, `OperatorPanelPage.test.jsx` | ✅ PASS |
| **RF-14** | Denegación de inicio de trabajos a usuarios no técnicos o técnicos sin asignación activa (salvo `ADMIN`). | `IncidentTriageService`, `TareasRepository.existsByIncidentIdAndTechnicianIdAndIsActiveTrue` | `IncidentTriageServiceTest`, `IncidentTriageControllerTest` | ✅ PASS |
| **RF-15** | Reporte de resolución técnica (`RESOLVED`) con memoria &ge; 10 caracteres por técnico asignado o `ADMIN`. | `IncidentTriageService.resolveIncident`, `ResolveIncidentModal`, `OperatorPanelPage` | `IncidentTriageServiceTest`, `ResolveIncidentModal.test.jsx`, `OperatorPanelPage.test.jsx` | ✅ PASS |
| **RF-16** | Bloqueo de resolución en prioridades medias, altas y críticas si la memoria técnica tiene &lt; 10 caracteres. | `IncidentTriageService`, `ResolveIncidentModal` | `IncidentTriageServiceTest`, `ResolveIncidentModal.test.jsx` | ✅ PASS |
| **RF-17** | Cierre administrativo definitivo (`CLOSED`) con inactivación automática de asignaciones y registro auditable. | `IncidentTriageService.closeIncident`, `AssignmentService.deactivateAllActiveAssignments` | `IncidentTriageServiceTest`, `AssignmentServiceTest`, `OperatorPanelPage.test.jsx` | ✅ PASS |
| **RF-18** | Reapertura operativa (`RESOLVED` &rarr; `REOPENED`) con motivo obligatorio &ge; 5 caracteres. | `IncidentTriageService.reopenIncident`, `ReopenIncidentRequest` | `IncidentTriageServiceTest`, `ReasonModal.test.jsx`, `OperatorPanelPage.test.jsx` | ✅ PASS |
| **RF-19** | Denegación de cierre o reapertura a usuarios con roles no autorizados (`TECHNICIAN`, `CITIZEN`). | `IncidentTriageController` (`@PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")`) | `IncidentTriageControllerTest` | ✅ PASS |
| **RF-20** | Publicación de notas técnicas internas (`is_internal = true`) y comentarios públicos (`is_internal = false`). | `CommentService.createComment`, `ComentariosIncidenciasRepository` | `CommentServiceTest`, `IncidentCommentsSection.test.jsx` | ✅ PASS |
| **RF-21** | Segregación estricta RBAC: los ciudadanos nunca reciben comentarios marcados como internos (`is_internal = true`). | `CommentService.getCommentsForIncident`, `IncidentTriageController` | `CommentServiceTest`, `IncidentTriageControllerTest`, `IncidentCommentsSection.test.jsx` | ✅ PASS |
| **RF-22** | Edición de comentarios por su autor original o `ADMIN`, sellando `updated_at` y preservando el autor. | `CommentService.updateComment`, `ComentariosIncidencias.updatedAt` | `CommentServiceTest`, `ComentariosIncidenciasRepositoryTest`, `IncidentCommentsSection.test.jsx` | ✅ PASS |
| **RF-23** | Denegación de edición de comentarios a terceros no autores sin rol `ADMIN`. | `CommentService.validateCommentEditAuthorization` | `CommentServiceTest`, `IncidentCommentsSection.test.jsx` | ✅ PASS |
| **RF-24** | Bloqueo estricto de adición o edición de comentarios en incidencias en estado terminal (`CLOSED`, `REJECTED`). | `CommentService.assertIncidentNotTerminal` | `CommentServiceTest`, `IncidentCommentsSection.test.jsx` | ✅ PASS |
| **RF-25** | Segregación de incidencias en `GET /api/incidents`: los técnicos solo visualizan incidencias con asignación activa. | `IncidentService.getActiveIncidents(userEmail)`, `IncidentController` | `IncidentServiceTest`, `IncidentControllerTest`, `OperatorPanelPage.test.jsx` | ✅ PASS |

---

## 4. Métricas de Cobertura Técnica de Código

### 4.1 Cobertura de Backend (JaCoCo)
*Reporte extraído de `Backend/target/site/jacoco/index.html`:*

| Métrica JaCoCo | Cobertura Global | Cubierto / Total | Módulos Clave de la Feature 005 |
|:---|:---:|:---:|:---|
| **Líneas (`Lines`)** | **95%** | 1.669 / 1.756 | `org.bulbaseros.controller`: **100%**<br>`org.bulbaseros.mappers`: **100%**<br>`org.bulbaseros.service`: **95%** |
| **Instrucciones (`Instructions`)** | **93%** | 6.912 / 7.403 | `IncidentTriageController`: **98%**<br>`Mappers`: **100%**<br>`Priority & Triage Services`: **91%** |
| **Ramas (`Branches`)** | **76%** | 606 / 790 | `org.bulbaseros.mappers`: **95%**<br>`org.bulbaseros.service`: **76%** |
| **Métodos (`Methods`)** | **91%** | 373 / 410 | Controladores y mappers: **100%** |
| **Clases Analizadas** | **55** | 54 al 100% | 0 errores |

- **Total de pruebas en Backend:** **471 tests ejecutados, 0 fallos, 0 errores (100% verde)**.

### 4.2 Cobertura de Frontend (Vitest)
*Reporte extraído de `frontend/coverage/index.html`:*

| Métrica Vitest | Cobertura Global Frontend | Componentes Clave de la Feature 005 |
|:---|:---:|:---|
| **Líneas (`Lines`)** | **80.26%** (1037 / 1292) | `components/Triage`: **93.9%**<br>`components/Comments`: **87.91%**<br>`components/Timeline`: **100%**<br>`pages/OperatorPanelPage`: **80.64%** |
| **Sentencias (`Statements`)** | **78.96%** (1070 / 1355) | `components/Triage`: **91.19%**<br>`components/Comments`: **87.5%**<br>`utils`: **90.32%** |
| **Funciones (`Functions`)** | **73.08%** (239 / 327) | `components/Comments`: **94.73%**<br>`components/Triage`: **90.36%**<br>`utils`: **94.11%** |
| **Ramas (`Branches`)** | **68.24%** (791 / 1159) | `components/Comments`: **81.25%**<br>`components/Triage`: **75.49%**<br>`pages`: **71.84%** |

- **Total de pruebas en Frontend:** **167 tests pasados en 20 suites (100% verde)**.
- **ESLint:** 0 errores, 0 advertencias.

---

## 5. Matriz de Endpoints y Seguridad RBAC

| Método | Endpoint | Roles Autorizados | Estado Precondición | Descripción Funcional |
|:---|:---|:---|:---|:---|
| `POST` | `/api/incidents/{id}/priority/calculate` | `OPERATOR`, `ADMIN` | Cualquiera | Calcula prioridad asistida sugerida según vía y seguridad. |
| `PUT` | `/api/incidents/{id}/priority` | `OPERATOR`, `ADMIN` | No terminal | Fija la prioridad definitiva y su justificación explicativa. |
| `POST` | `/api/incidents/{id}/validate` | `OPERATOR`, `ADMIN` | `REPORTED` | Valida el reporte para entrar al circuito operativo formal. |
| `POST` | `/api/incidents/{id}/reject` | `OPERATOR`, `ADMIN` | No terminal | Rechaza el reporte con motivo &ge; 5 caracteres e inactiva tareas. |
| `POST` | `/api/incidents/{id}/reevaluate` | `OPERATOR`, `ADMIN` | `REJECTED` | Reactiva una incidencia rechazada a `REPORTED` o `VALIDATED`. |
| `POST` | `/api/incidents/{id}/assignments` | `OPERATOR`, `ADMIN` | `VALIDATED`, `REOPENED`, `ASSIGNED` | Asigna departamentos y técnicos adscritos (transiciona a `ASSIGNED`). |
| `POST` | `/api/incidents/{id}/assignments/deactivate` | `OPERATOR`, `ADMIN` | `ASSIGNED` | Desactiva lógicamente asignaciones existentes. |
| `POST` | `/api/incidents/{id}/start-work` | `TECHNICIAN`, `ADMIN` | `ASSIGNED`, `REOPENED` | Inicia intervención sobre el terreno (pasa a `IN_PROGRESS`). |
| `POST` | `/api/incidents/{id}/resolve` | `TECHNICIAN`, `ADMIN` | `IN_PROGRESS` | Resuelve con memoria técnica obligatoria &ge; 10 caracteres. |
| `POST` | `/api/incidents/{id}/close` | `OPERATOR`, `ADMIN` | `RESOLVED` | Cierra formalmente el expediente e inactiva asignaciones. |
| `POST` | `/api/incidents/{id}/reopen` | `OPERATOR`, `ADMIN` | `RESOLVED` | Reabre con motivo obligatorio &ge; 5 caracteres. |
| `GET` | `/api/incidents/{id}/comments` | `AUTHENTICATED` | Cualquiera | Ciudadanos reciben solo públicos; personal municipal recibe todos. |
| `POST` | `/api/incidents/{id}/comments` | `OPERATOR`, `TECHNICIAN`, `ADMIN` | No terminal | Publica nota interna (`is_internal=true`) o aviso público (`is_internal=false`). |
| `PUT` | `/api/incidents/{id}/comments/{commentId}` | `AUTHENTICATED` | No terminal | Edición permitida al autor original o al rol `ADMIN`. |
| `GET` | `/api/departments` | `AUTHENTICATED` | — | Catálogo ordenado alfabéticamente de áreas municipales. |
| `GET` | `/api/departments/{id}/technicians` | `OPERATOR`, `ADMIN` | — | Técnicos adscritos exclusivamente al departamento indicado. |

---

## 6. Guía Paso a Paso para la Verificación Manual

### Prerrequisitos de Ejecución
1. **Base de Datos:** Levantar el contenedor PostgreSQL con datos semilla:
   ```bash
   docker compose up -d
   ```
2. **Backend:** Iniciar el servidor Spring Boot (puerto 8080):
   ```bash
   cd Backend && mvn spring-boot:run
   ```
3. **Frontend:** Iniciar el cliente React + Vite (puerto 5173):
   ```bash
   cd frontend && npm run dev
   ```
   Abrir navegador en `http://localhost:5173`.

---

### Paso 1: Prueba de Permisos Universales de ADMIN
1. Iniciar sesión con:
   - **Email:** `admin@urbanpulse.es`
   - **Contraseña:** `Admin1234`
2. Ir al **Panel de Operaciones** (`/operator-panel`).
3. Localizar una incidencia en estado `ASIGNADA` o `REABIERTA`.
4. Comprobar que aparece el botón verde **"Iniciar Trabajo"**: pulsarlo.
   - La incidencia cambia inmediatamente a `EN INTERVENCIÓN`.
5. Comprobar que aparece el botón verde **"Resolver"**: pulsarlo.
   - Introducir menos de 10 caracteres (ej. "Ok"): verificar que el botón de confirmación se mantiene bloqueado.
   - Introducir una memoria descriptiva válida (ej. "Reparación completada y comprobada in situ.").
   - Confirmar: la incidencia pasa a `RESUELTA`.
6. **Resultado esperado:** El administrador puede ejecutar todo el ciclo técnico sin estar autoasignado artificialmente.

---

### Paso 2: Prueba de Vista Segregada para Técnicos de Campo
1. Con la sesión de `admin@urbanpulse.es` o de `operator@urbanpulse.es`, asignar una incidencia a:
   - **Departamento:** *Infraestructuras y Obras Públicas*.
   - **Técnico:** *Alvaro Gomez (Tecnico Vias)* (`tech.obras1@urbanpulse.es`).
2. Cerrar sesión e iniciar sesión como técnico:
   - **Email:** `tech.obras1@urbanpulse.es`
   - **Contraseña:** `Operator1234`
3. En la barra superior, pulsar en **"Mis Asignaciones"**.
4. **Verificaciones visuales y de seguridad:**
   - El título es **"Mis Asignaciones Técnicas"** y el badge indica *"Técnico de Campo"*.
   - **Única y exclusivamente** aparecen las incidencias donde figura asignado Álvaro Gómez (`is_active = true`).
   - Están ocultos los botones de triaje de operador (Validar, Rechazar, Reevaluar, Asignar Departamentos, Cerrar, Reabrir).
   - En las incidencias asignadas puede pulsar **"Iniciar Trabajo"** y **"Resolver"**.

---

### Paso 3: Prueba de Priorización Asistida y Asignación Multidepartamental
1. Iniciar sesión como operador:
   - **Email:** `operator@urbanpulse.es`
   - **Contraseña:** `Operator1234`
2. Ir a `/operator-panel` y localizar una incidencia en `REPORTADA`.
3. Pulsar el botón **`⚡ Asistente`**:
   - Marcar *"Corte de Calle o Calzada"* y nivel de seguridad *"Alto"*.
   - Verificar que el asistente calcula y sugiere `CRÍTICA` con la justificación redactada. Confirmar.
4. Pulsar **"Validar"**: pasa a `VALIDADA`.
5. Pulsar **"Asignar Departamentos/Técnicos"**:
   - Seleccionar *Infraestructuras y Obras Públicas* y *Medio Ambiente y Limpieza*.
   - Comprobar que en el selector de técnicos solo aparecen los técnicos de esas áreas.
   - Guardar: el estado pasa a `ASIGNADA` y se muestran los badges con los nombres de departamentos y técnicos.

---

### Paso 4: Prueba del Muro de Comentarios y Segregación de Visibilidad
1. Sobre cualquier incidencia activa, pulsar el botón **`💬 Comentarios`**.
2. Publicar una **Nota Interna** (`is_internal = true`) indicando: *"Se precisa andamio especial para mañana"*.
3. Publicar un **Aviso Público** (`is_internal = false`) indicando: *"Personal municipal trabajando en la zona"*.
4. Cerrar sesión e iniciar sesión como ciudadano:
   - **Email:** `citizen@urbanpulse.es`
   - **Contraseña:** `Citizen1234`
5. Abrir la incidencia en `/my-incidents` o en el mapa:
   - **Comprobar que el Aviso Público es visible.**
   - **Comprobar que la Nota Interna NO aparece ni se envía en la respuesta JSON.**

---

### Paso 5: Prueba de Reapertura, Cierre y Reevaluación
1. Iniciar sesión nuevamente como `operator@urbanpulse.es` o `admin@urbanpulse.es`.
2. En una incidencia `RESUELTA`, pulsar **"Reabrir"**:
   - Introducir motivo &ge; 5 caracteres (*"Problema recurrente reportado por vecinos"*): pasa a `REABIERTA`.
3. Resolverla de nuevo y pulsar **"Cerrar"**:
   - Pasa a `CERRADA`. Sus asignaciones se desactivan lógicamente y el muro de comentarios bloquea nuevos comentarios.
4. En una incidencia en `REPORTADA`, pulsar **"Rechazar"**:
   - Introducir motivo &ge; 5 caracteres (*"Aviso duplicado con incidencia #12"*): pasa a `RECHAZADA`.
5. Sobre la incidencia rechazada, pulsar **"Reevaluar"**:
   - Seleccionar destino `VALIDADA` e indicar motivo (*"Revisión posterior confirma competencia municipal"*): se reactiva en `VALIDADA`.

---

## 7. Conclusión y Veredicto Final

La **Feature 005: Gestión y Triaje Operativo de Incidencias** satisface la totalidad de los requisitos funcionales (RF-1 al RF-25), los requisitos no funcionales de seguridad, trazabilidad inmutable y tiempos de respuesta, así como las directrices de diseño y arquitectura de `constitution/` y `ADR-07`.

**Veredicto Final:** ✅ **APROBADO PARA PRODUCCIÓN / MERGE EN DEVELOP**.
