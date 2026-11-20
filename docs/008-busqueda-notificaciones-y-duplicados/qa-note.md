# QA Note — Feature 008: Búsqueda, Notificaciones y Duplicados

**Estado de Validación:** ✅ VALIDADA CON ÉXITO (`sdd-validate` APROBADO)  
**Fecha de validación:** 2026-11-08  
**Rama de desarrollo:** `feature/009busqueda_notificaciones_y_duplicados`  
**Referencia arquitectónica:** [ADR-08: Motor de Detección de Duplicados y Centro de Notificaciones In-App](file:///c:/Users/Germán/Documents/DesarrolloSoftwareNube/Proyecto-Urbanpulse/docs/ADR/ADR-08-deteccion-duplicados-y-notificaciones.md)  
**Especificación funcional:** [spec.md](file:///c:/Users/Germán/Documents/DesarrolloSoftwareNube/Proyecto-Urbanpulse/features/008-busqueda-notificaciones-y-duplicados/spec.md)  
**Plan técnico:** [plan.md](file:///c:/Users/Germán/Documents/DesarrolloSoftwareNube/Proyecto-Urbanpulse/features/008-busqueda-notificaciones-y-duplicados/plan.md)  
**Desglose de tareas:** [tasks.md](file:///c:/Users/Germán/Documents/DesarrolloSoftwareNube/Proyecto-Urbanpulse/features/008-busqueda-notificaciones-y-duplicados/tasks.md)  

---

## 1. Resumen Global de la Feature 008

La Feature **008 — Búsqueda, Notificaciones y Duplicados** aporta al ecosistema UrbanPulse tres bloques funcionales críticos para la operación urbana inteligente y la comunicación transparente:

1. **Búsqueda y Filtrado Multicriterio (HU19):**  
   Localización ágil de incidencias mediante la combinación libre de filtros (texto insensible a mayúsculas/acentos en título/descripción, categoría, rango de fechas de registro y estado de triaje). Implementado a nivel de persistencia mediante Criteria API (`JpaSpecificationExecutor` y `IncidentSpecifications`). Dispone de control de acceso estricto (RBAC): el selector por estado está reservado al personal interno (`OPERATOR`, `TECHNICIAN`, `ADMIN`, `ANALYST`) y permanece oculto y bloqueado para la ciudadanía (`CITIZEN`).

2. **Detección y Sugerencia de Incidencias Duplicadas (HU20):**  
   Motor de coincidencia multi-vectorial que evalúa simultáneamente:
   - Proximidad espacial mediante fórmula de Haversine ($\le 60\text{ metros}$).
   - Ventana temporal de coexistencia ($\le 48\text{ horas}$).
   - Coincidencia taxonómica (misma categoría de incidencia).
   - Similitud léxica mediante coeficiente de Jaccard sobre texto normalizado ($\ge 0.3$).
   Al detectarse una coincidencia, el sistema emite automáticamente alertas contextuales dirigidas exclusivamente a operadores y técnicos. Estos disponen de la acción para confirmar el duplicado, transicionando la incidencia a estado `REJECTED` con motivo de duplicidad en auditoría, sin penalizar la capacidad del ciudadano para seguir reportando avisos.

3. **Centro de Notificaciones de Cambio de Estado (HU21):**  
   Mecanismo de comunicación in-app bidireccional soportado por la entidad relacional persistente `Notification`. Registra avisos ante cualquier transición en el ciclo de vida de una incidencia (validación, rechazo, asignación, inicio de trabajos, resolución técnica, reapertura, cierre y confirmación de duplicados), notificando tanto al ciudadano que originó el reporte como a todo el personal técnico y operativo interviniente. En frontend se materializa mediante un componente de campana en la barra de navegación con badge de conteo en tiempo real, bandeja cronológica desplegable, marcado individual de lectura y navegación con apertura directa de la ficha modal de la incidencia.

---

## 2. Cuentas y Credenciales de Prueba Preconfiguradas (Base de Datos)

Credenciales sembradas en el entorno PostgreSQL (`database/init/diagramUrbanPulse.sql`):

| Rol | Correo Electrónico | Contraseña *(case-sensitive)* | Capacidades Operativas a Probar |
|:---|:---|:---|:---|
| **ADMIN** | `admin@urbanpulse.es` | `Admin1234` | Supervisión global. Visualiza todos los filtros (incluyendo selector de estado). Recibe alertas de notificaciones y duplicados. Puede confirmar duplicados. |
| **OPERATOR** | `operator@urbanpulse.es` | `Operator1234` | Triaje y supervisión operativa. Accede a filtrado completo. Recibe sugerencias de duplicados con badge `⚠ Posible Duplicado` y botón para confirmar rechazo por duplicidad. Recibe avisos de cambios de estado. |
| **TECHNICIAN** *(Vías)* | `tech.obras1@urbanpulse.es` | `Operator1234` | Técnico asignado. Acceso a filtros completos. Recibe notificaciones de transiciones en incidencias asignadas y alertas de duplicados en su área. |
| **CITIZEN** | `citizen@urbanpulse.es` | `Citizen1234` | Ciudadano. **Segregación RBAC:** Solo puede filtrar por texto, categoría y fechas (filtro de estado bloqueado y oculto). Dispone de campana para ver cambios en sus incidencias reportadas. |

---

## 3. Tabla de Trazabilidad de Requisitos Funcionales (EARS) y Tests

| RF | Requisito Funcional (EARS) | Módulos de Implementación | Tests de Verificación | Estado |
|:---|:---|:---|:---|:---:|
| **RF-1** | Filtrado por texto libre insensible a mayúsculas y acentos en título o descripción. | `IncidentSpecifications.hasSearchText`, `IncidentFilterBar.jsx` | `IncidentSpecificationsTest.shouldBuildPredicateForSearchText`, `IncidentFilterBar.test.jsx` | ✅ PASS |
| **RF-2** | Filtrado multicriterio combinable (categoría, rango fechas, texto) en mapa y tabla de operador. | `IncidentSpecifications.build`, `MapPage.jsx`, `OperatorPanelPage.jsx` | `IncidentSpecificationsTest.shouldCombineMultiplePredicatesConcurrently`, `IncidentFilterBar.test.jsx` | ✅ PASS |
| **RF-3** | Habilitación de selector de estado para roles internos (`OPERATOR`, `TECHNICIAN`, `ADMIN`, `ANALYST`). | `IncidentFilterBar.jsx`, `IncidentSpecifications.java` | `IncidentFilterBar.test.jsx` | ✅ PASS |
| **RF-4** | Ocultación y bloqueo de selector de estado para rol ciudadano (`CITIZEN`). | `IncidentFilterBar.jsx`, `IncidentController.java` | `IncidentFilterBar.test.jsx`, `IncidentControllerTest` | ✅ PASS |
| **RF-5** | Restablecimiento rápido de filtros recuperando la visualización global sin restricciones. | `IncidentFilterBar.jsx` (`handleReset`) | `IncidentFilterBar.test.jsx` | ✅ PASS |
| **RF-6** | Estado visual vacío ante búsquedas sin coincidencias con acción de limpieza rápida. | `MapPage.jsx`, `OperatorPanelPage.jsx` | `OperatorPanelPage.test.jsx`, `IncidentFilterBar.test.jsx` | ✅ PASS |
| **RF-7** | Detección de duplicados según proximidad espacial $\le 60\text{ m}$, ventana $\le 48\text{ h}$, categoría y Jaccard $\ge 0.3$. | `DuplicateDetectionService.java` | `DuplicateDetectionServiceTest` (10 tests dedicados) | ✅ PASS |
| **RF-8** | Notificación de sugerencia de duplicado dirigida exclusivamente a personal operativo y técnico. | `DuplicateDetectionService.java`, `UserRepository.findByRole` | `DuplicateDetectionServiceTest.shouldCreateNotificationsForOperatorsAndTechniciansWhenDuplicateDetected` | ✅ PASS |
| **RF-9** | Confirmación de duplicado por operador/técnico transicionando a `REJECTED` con motivo de duplicidad en auditoría. | `IncidentTriageService.markAsDuplicate`, `IncidentTriageController` | `IncidentTriageServiceTest.shouldMarkIncidentAsDuplicateAndTransitionToRejected`, `IncidentTriageControllerTest` | ✅ PASS |
| **RF-10** | Permiso ininterrumpido para que el ciudadano autor continúe reportando incidencias tras un duplicado. | `IncidentService.createIncident` | `IncidentServiceTest.shouldCreateIncidentSuccessfully`, `CreateIncidentModal.test.jsx` | ✅ PASS |
| **RF-11** | Tratamiento como incidencias independientes si distan $> 60\text{ m}$, superan 48 h o difieren en categoría/texto. | `DuplicateDetectionService.java` | `DuplicateDetectionServiceTest.shouldNotDetectDuplicateWhenDistanceExceeds60Meters` (y variantes) | ✅ PASS |
| **RF-12** | Notificación persistente en transiciones de estado para el autor y técnicos/operadores asignados. | `NotificationService.notifyStatusChange`, `IncidentTriageService` | `NotificationServiceTest`, `IncidentTriageServiceTest` | ✅ PASS |
| **RF-13** | Campana en navbar con badge dinámico de notificaciones no leídas para el usuario autenticado. | `NotificationBell.jsx`, `Navbar.jsx` | `NotificationBell.test.jsx` | ✅ PASS |
| **RF-14** | Despliegue de bandeja cronológica descendente con detalle de mensaje, fecha y estado de lectura. | `NotificationBell.jsx`, `NotificationService.getNotificationsForUser` | `NotificationBell.test.jsx`, `NotificationServiceTest` | ✅ PASS |
| **RF-15** | Clic en notificación abre directamente el modal de detalle de la incidencia vinculada. | `NotificationBell.jsx` (`onIncidentClick`) | `NotificationBell.test.jsx` | ✅ PASS |
| **RF-16** | Marcado individual como leída con actualización inmediata en BD y decremento del badge. | `NotificationBell.jsx`, `NotificationController.markAsRead` | `NotificationBell.test.jsx`, `NotificationControllerTest` | ✅ PASS |
| **RF-17** | Vista informativa vacía cuando la bandeja no cuenta con avisos ("No tienes notificaciones pendientes"). | `NotificationBell.jsx` | `NotificationBell.test.jsx` | ✅ PASS |

---

## 4. Métricas de Cobertura Técnica de Código

### 4.1 Backend (JaCoCo)
*Reporte consolidado tras `mvn clean test`:*

| Métrica JaCoCo | Cobertura Global Proyecto | Módulos Clave de la Feature 008 |
|:---|:---:|:---|
| **Líneas (`Lines`)** | **94.31%** (2.484 / 2.634) | `NotificationController`: **100%** (13/13)<br>`NotificationService`: **100%** (20/20)<br>`NotificationMapper`: **100%** (14/14)<br>`DuplicateDetectionService`: **93.55%** (58/62)<br>`IncidentSpecifications`: **92.59%** (25/27)<br>`IncidentController`: **94.55%** (52/55)<br>`IncidentTriageController`: **100%** (39/39) |
| **Instrucciones (`Instructions`)** | **92.84%** (11.534 / 12.424) | `NotificationService`: **100%** (101/101)<br>`NotificationMapper`: **100%** (48/48)<br>`NotificationController`: **98.11%** (52/53)<br>`DuplicateDetectionService`: **94.47%** (359/380) |
| **Ramas (`Branches`)** | **74.46%** (1.009 / 1.355) | `IncidentSpecifications`: **100%** (18/18)<br>`NotificationMapper`: **100%** (4/4)<br>`DuplicateDetectionService`: **56.90%** (33/58) |
| **Clases Analizadas** | **70 clases** | Todas las clases de la feature integradas con 0 errores de compilación o ejecución. |

- **Total de pruebas en Backend:** **625 tests ejecutados, 0 fallos, 0 errores, 1 omitido** (`@EnabledIfPostgresAvailable`).

### 4.2 Frontend (Vitest & ESLint)
*Reporte consolidado tras `npm run test:coverage`:*

| Métrica Vitest | Cobertura Global Frontend | Componentes Clave de la Feature 008 |
|:---|:---:|:---|
| **Líneas (`Lines`)** | **83.18%** | `components/Filters/IncidentFilterBar.jsx`: **100%**<br>`components/Notifications/NotificationBell.jsx`: **93.10%**<br>`services/notificationService.js`: **100%**<br>`services/triageService.js`: **96.36%** |
| **Sentencias (`Statements`)** | **82.12%** | `NotificationBell.jsx`: **92.06%**<br>`notificationService.js`: **100%**<br>`triageService.js`: **96.36%** |
| **Funciones (`Functions`)** | **76.14%** | `NotificationBell.jsx`: **91.66%**<br>`notificationService.js`: **100%**<br>`triageService.js`: **94.44%** |
| **Ramas (`Branches`)** | **71.52%** | `IncidentFilterBar.jsx`: **77.77%**<br>`NotificationBell.jsx`: **66.66%**<br>`triageService.js`: **100%** |

- **Total de pruebas en Frontend:** **33 suites, 259 tests pasados (100% verde)**.
- **Auditoría de Linter:** **0 errores, 0 advertencias** con `npm run lint`.

---

## 5. Verificación de Requisitos No Funcionales (RNF)

- **Control de Acceso y Privacidad (RBAC):**  
  Las consultas a `/api/notifications` obtienen únicamente las notificaciones del usuario autenticado vía `SecurityContextHolder`. Los endpoints de detección y confirmación de duplicados exigen `hasAnyRole('OPERATOR', 'ADMIN')` o `TECHNICIAN`. En frontend, los ciudadanos no pueden visualizar ni manipular el selector de estado.
- **Rendimiento de Búsqueda:**  
  Las consultas multicriterio resuelven en < 400 ms gracias a índices en base de datos (`idx_incidencias_estado`, `idx_incidencias_categoria`, `idx_incidencias_fecha_creacion`) y construcción optimizada de predicados en Criteria API.
- **Persistencia e Integridad:**  
  Entidad JPA `Notification` vinculada con integridad referencial a `incidencias` y `usuarios`, con auditoría temporal (`created_at`) y bandera booleana `is_read`.
- **Usabilidad y Accesibilidad:**  
  La campana dispone de atributos semánticos (`aria-label="Notificaciones"`, `aria-expanded`), contraste accesible y diseño responsive para dispositivos móviles y de escritorio.

---

## 6. Guía Rápida de Pruebas Manuales

Para verificar manualmente el funcionamiento integrado de la Feature 008, sigue estos sencillos pasos:

### Paso 1: Levantar los servicios locales
1. Iniciar base de datos: `docker compose up -d`
2. Iniciar backend: `cd Backend && mvn spring-boot:run`
3. Iniciar frontend: `cd frontend && npm run dev`
4. Abrir el navegador en `http://localhost:5173`.

### Paso 2: Probar Búsqueda y Filtrado Multicriterio (HU19)
1. **Como Ciudadano:** Inicia sesión con `citizen@urbanpulse.es` / `Citizen1234`.
2. Dirígete a la vista de Mapa (`/`).
3. Comprueba que la barra de filtros muestra campos de texto libre, categoría y fechas, pero **NO muestra el selector de estado**.
4. Escribe un término de búsqueda (ej. `"bache"` o `"farola"`): los marcadores en el mapa se filtrarán inmediatamente.
5. Pulsa el botón *"Limpiar"* o *"Restablecer"*: se restauran todos los marcadores originales.
6. Cierra sesión e inicia como Operador (`operator@urbanpulse.es` / `Operator1234`): accede a `/operator` o al mapa y comprueba que **ahora sí aparece el filtro por estado**.

### Paso 3: Probar Detección y Sugerencia de Duplicados (HU20)
1. Inicia sesión como Ciudadano (`citizen@urbanpulse.es` / `Citizen1234`).
2. Crea una incidencia en una ubicación concreta (ej. Calle Larios, categoría *"Vía Pública"*, título *"Bache profundo en calzada"*).
3. Crea inmediatamente una segunda incidencia en las inmediaciones (a menos de 60 metros), misma categoría y título similar (*"Bache grande en el asfalto"*).
4. Cierra sesión e inicia como Operador (`operator@urbanpulse.es` / `Operator1234`).
5. Abre el Centro de Notificaciones (campana en el navbar): observarás una notificación de tipo *"Posible incidencia duplicada detectada"*.
6. En el panel de operador (`/operator`), la incidencia mostrará el indicador `⚠ Posible Duplicado` y la acción *"Confirmar Duplicado"*.
7. Pulsa *"Confirmar Duplicado"*: la incidencia se transiciona automáticamente a estado `REJECTED` registrando la justificación de duplicidad.

### Paso 4: Probar Notificaciones de Cambio de Estado (HU21)
1. Desde el panel de operador, cambia el estado de una incidencia creada por el ciudadano (por ejemplo, valídala a `VALIDATED` o reasígnala).
2. Cierra sesión y vuelve a ingresar como Ciudadano (`citizen@urbanpulse.es` / `Citizen1234`).
3. Observa la campana en el navbar: mostrará un badge numérico rojo con el conteo de avisos pendientes.
4. Haz clic en la campana para desplegar la bandeja:
   - Verás la notificación con el cambio de estado y la fecha.
   - Haz clic sobre la notificación: se abrirá la ficha de detalle de la incidencia.
   - Pulsa sobre el icono de check o *"Marcar como leída"*: el aviso se marcará como leído y el badge del contador se decrementará inmediatamente.

---

## 7. Decisión de Calidad (Sign-Off)

> **Veredicto Oficial:** **APROBADO PARA PRODUCCIÓN (GO)**  
> La Feature 008 satisface la totalidad de los requisitos funcionales, criterios de aceptación, directrices de arquitectura y estándares de calidad de código del proyecto UrbanPulse.
