# Documento Técnico y Guía de Validación de Calidad (QA Note)
## Feature 009: Analítica Urbana y Reportes

**Fecha:** 8 de noviembre de 2026  
**Rama:** `feature/feature-009-analítica-urbana-y-reportes`  
**Estado:** Implementación Completa — Lista para Validación Formal (`sdd-validate`)  
**Cobertura Funcional:** Historias de Usuario HU22, HU23, HU24 y HU25 (RF-1 al RF-21)  

---

## 1. Resumen Ejecutivo de la Feature

La Feature 009, **Analítica Urbana y Reportes**, dota a los roles analíticos y operativos del Ayuntamiento de Málaga (`ANALYST`, `ADMIN`, `OPERATOR`) de una plataforma unificada para la supervisión de acuerdos de nivel de servicio (SLAs), correlación territorial y estacional de incidencias, exportación oficial de informes ejecutivos en formato PDF y asistencia inteligente probabilística en el triaje y asignación multidepartamental.

### 1.1 Historias de Usuario Cubiertas
- **HU22 (Métricas Operativas y SLAs / H1):** Supervisión de indicadores MTTT (Mean Time to Triage) y MTTR (Mean Time to Resolve), evaluación de matrices deterministas de SLA según severidad (`CRITICAL` a 3 días, `HIGH` a 7 días, `MEDIUM` a 15 días y `LOW` a 30 días), desglose por departamento y categoría temática.
- **HU23 (Análisis Territorial Agregado y Correlación Histórica / H2):** Desglose dinámico por los 11 distritos oficiales de Málaga y sus barrios, filtrado temporal multiescala (`TODAY`, `LAST_7_DAYS`, `LAST_30_DAYS`, `LAST_3_MONTHS`, `LAST_6_MONTHS`, `LAST_YEAR`, `SPECIFIC_YEAR`) y visualizaciones gráficas SVG interactivas y nativas.
- **HU24 (Exportación de Informes en PDF / H3):** Generación cliente en PDF estructurado según las directrices gráficas institucionales del consistorio malagueño (cabecera oficial, resumen ejecutivo de KPIs y desglose tabular de incidencias filtradas).
- **HU25 (Asistencia Inteligente y Recomendaciones con Grado de Confianza / H4):** Motor heurístico ponderado con cálculo porcentual de certeza (0% a 100%), preselección asistida en modal de asignación, insignia persistente de recomendación, distintivo visual de baja confianza (< 50%) y métricas de desempeño del recomendador en el panel general.

---

## 2. Matriz de Requisitos Funcionales (EARS) vs Implementación

| Identificador | Requisito Funcional (EARS) | Componentes Clave Involucrados | Evidencia de Verificación |
| :--- | :--- | :--- | :--- |
| **RF-1** | Cálculo y presentación del MTTT (Tiempo Medio de Triaje). | `AnalyticsService.java`, `IncidenciaRepository.java`, `AnalyticsKpiCards.jsx` | `AnalyticsServiceTest#mtttCalculation`, `AnalyticsKpiCards.test.jsx` |
| **RF-2** | Cálculo y presentación del MTTR (Tiempo Medio de Resolución). | `AnalyticsService.java`, `CambiosEstadoRepository.java`, `AnalyticsKpiCards.jsx` | `AnalyticsServiceTest#mttrCalculation`, `AnalyticsKpiCards.test.jsx` |
| **RF-3** | Matriz de umbrales máximos de SLA por prioridad (3d, 7d, 15d, 30d). | `AnalyticsService.java`, `Incidencia.java`, `Prioridad.java` | `AnalyticsServiceTest#slaThresholdsEvaluation` (todas las prioridades) |
| **RF-4** | Detección de superación de SLA en incidencias resueltas y en curso. | `AnalyticsService.java` | `AnalyticsServiceTest#inProgressAndResolvedSlaBreach` |
| **RF-5** | Cálculo porcentual de la Tasa de Cumplimiento de SLA. | `AnalyticsService.java`, `OperationalMetricsResponse.java` | `AnalyticsServiceTest#slaComplianceRate` |
| **RF-6** | Desglose cuantitativo por departamento municipal y categoría. | `AnalyticsService.java`, `DepartmentMetricResponse.java`, `CategoryMetricResponse.java` | `AnalyticsRepositoriesTest`, `AnalyticsServiceTest` |
| **RF-7** | Recálculo dinámico por nivel territorial (Distrito o Barrio). | `IncidenciaRepository.java`, `AnalyticsService.java`, `TerritorialChart.jsx` | `AnalyticsRepositoriesTest#filterByDistrict`, `TerritorialChart.test.jsx` |
| **RF-8** | Presets temporales preconfigurados y año específico. | `AnalyticsTimePreset.java`, `AnalyticsDateFilter.jsx`, `AnalyticsController.java` | `DtoAnalyticsValidationTest`, `AnalyticsDateFilter.test.jsx` |
| **RF-9** | Actualización reactiva de métricas ante cambios de filtro. | `AnalyticsPage.jsx`, `analyticsService.js` | `AnalyticsPage.test.jsx#reactiveFilterUpdate` |
| **RF-10** | Gráficos visuales interactivos SVG nativos (Territorio y SLAs). | `TerritorialChart.jsx`, `SlaComplianceChart.jsx` | `TerritorialChart.test.jsx`, `SlaComplianceChart.test.jsx` |
| **RF-11** | Manejo de conjuntos vacíos sin divisiones por cero (estado vacío orientador). | `AnalyticsService.java`, `EmptyAnalyticsState.jsx` | `AnalyticsServiceTest#zeroIncidentsResilience`, `EmptyAnalyticsState.test.jsx` |
| **RF-12** | Habilitación condicional del botón de exportación PDF mientras haya datos. | `ExportPdfButton.jsx`, `AnalyticsPage.jsx` | `ExportPdfButton.test.jsx#buttonEnabledState` |
| **RF-13** | Descarga de documento PDF estructurado oficial con KPIs y tabla. | `reportUtils.js`, `ExportPdfButton.jsx`, `jspdf`, `jspdf-autotable` | `reportUtils.test.js` (18 tests), `ExportPdfButton.test.jsx` |
| **RF-14** | Bloqueo reactivo (`disabled`) del botón de exportación PDF ante listado vacío. | `ExportPdfButton.jsx` | `ExportPdfButton.test.jsx#disabledOnEmpty` |
| **RF-15** | Preselección de departamento sugerido e insignia con grado de confianza. | `AssignmentModal.jsx`, `IntelligentRecommendationService.java` | `AssignmentModal.test.jsx#preselection`, `IntelligentRecommendationServiceTest` |
| **RF-16** | Persistencia visible de la insignia tras cambiar o desmarcar la selección. | `AssignmentModal.jsx` | `AssignmentModal.test.jsx#persistenceOnDeselect` |
| **RF-17** | Advertencia explícita visible cuando la confianza es inferior al 50%. | `AssignmentModal.jsx`, `IntelligentRecommendationService.java` | `AssignmentModal.test.jsx#lowConfidenceWarning` |
| **RF-18** | Métrica de confianza promedio y tasa de adopción del asistente inteligente. | `IntelligentRecommendationService.java`, `AnalyticsKpiCards.jsx` | `IntelligentRecommendationServiceTest`, `AnalyticsKpiCards.test.jsx` |
| **RF-19** | Enlace visible "Analítica" en `Navbar` y `HomePage` para `ANALYST`, `ADMIN`, `OPERATOR`. | `Navbar.jsx`, `HomePage.jsx` | `Navbar.test.jsx`, `HomePage.test.jsx` |
| **RF-20** | Protección de la ruta `/analytics` mediante guardas de seguridad y RBAC. | `App.jsx`, `ProtectedRoute.jsx`, `RoleGuard.jsx` | `App.test.jsx#analyticsRouteAllowedRoles` |
| **RF-21** | Denegación de acceso (HTTP 403 / Redirección) para `CITIZEN` y `TECHNICIAN`. | `SecurityConfig.java`, `RoleGuard.jsx`, `App.jsx` | `AnalyticsControllerTest#securityTests`, `SecurityConfigTest`, `App.test.jsx` |

---

## 3. Arquitectura del Sistema

```
                                  [ Navegador Cliente (React 19) ]
                                                  │
                 ┌────────────────────────────────┴────────────────────────────────┐
                 ▼                                                                 ▼
      [ Barra de Navegación ]                                            [ Modal de Asignación ]
      - Navbar.jsx (RF-19)                                               - AssignmentModal.jsx (RF-15 a RF-17)
      - HomePage.jsx (Header & Cards)                                      * Insignia persistente
                                                                           * Advertencia confianza < 50%
                                                                           * Preselección automática
                 │                                                                 │
                 ▼                                                                 ▼
      [ Ruta Protegida RBAC ]                                            [ analyticsService.js ]
      - /analytics (App.jsx) (RF-20, RF-21)                              - getDepartmentRecommendation()
      - ProtectedRoute + RoleGuard                                                 │
                 │                                                                 │
                 ▼                                                                 │
      [ Panel AnalyticsPage.jsx ]                                                  │
      ├─ AnalyticsKpiCards (MTTT, MTTR, SLAs, Asistente)                           │
      ├─ AnalyticsDateFilter (Presets, Distritos, Barrios)                         │
      ├─ TerritorialChart (SVG Nativo por Distritos)                               │
      ├─ SlaComplianceChart (SVG Nativo por Departamentos)                         │
      ├─ ExportPdfButton / reportUtils.js (PDF Oficial)                            │
      └─ Tabla paginada de incidencias                                             │
                 │                                                                 │
                 └────────────────────────────────┬────────────────────────────────┘
                                                  │ Axios (JWT Auth)
                                                  ▼
                               [ Spring Boot 3 Backend (Java 21) ]
                                                  │
                 ┌────────────────────────────────┴────────────────────────────────┐
                 ▼                                                                 ▼
      [ SecurityConfig.java ]                                            [ AnalyticsController.java ]
      - Perímetro /api/analytics/**                                      - GET /api/analytics/summary
      - Roles: ANALYST, ADMIN, OPERATOR                                  - GET /api/analytics/recommend-department
      - 403 Forbidden para CITIZEN/TECH                                  - Bean Validation en DTOs
                 │                                                                 │
                 ▼                                                                 ▼
      [ AnalyticsService.java ]                                          [ IntelligentRecommendationService.java ]
      - MTTT / MTTR en horas                                             - Motor heurístico ponderado
      - Matriz determinista SLA (3d, 7d, 15d, 30d)                       - Certeza porcentual (0% - 100%)
      - Agregaciones espaciales y temáticas                              - Bandera isLowConfidenceWarning (< 50%)
      - Blindaje matemático sin divisiones por cero                      - Tasa de adopción y confianza media
                 │                                                                 │
                 └────────────────────────────────┬────────────────────────────────┘
                                                  │ Spring Data JPA
                                                  ▼
                                   [ Repositorios PostgreSQL / H2 ]
                                   - IncidenciaRepository
                                   - CambiosEstadoRepository
                                   - TareasRepository
```

---

## 4. Evidencias de Ejecución de Pruebas Automatizadas

### 4.1 Backend (Spring Boot 3 + JUnit 5 + Mockito + MockMvc + JaCoCo)
Comando ejecutado:
```bash
export JAVA_HOME=/home/alejandro/.jdks/corretto-21.0.12.1
export PATH=$JAVA_HOME/bin:$PATH
cd Backend && mvn clean test
```
**Resultado:**
- **Tests ejecutados:** 577 tests
- **Fallos:** 0
- **Errores:** 0
- **Ignorados / Omitidos:** 1 (preexistente de infraestructura)
- **Estado del build:** `BUILD SUCCESS` (Tiempo total: ~21-30 segundos)
- **JaCoCo Code Coverage:** Reporte generado satisfactoriamente en `target/site/jacoco/index.html`.

### 4.2 Frontend (React 19 + Vitest + React Testing Library)
Comando ejecutado:
```bash
cd frontend && npm test -- --run
```
**Resultado:**
- **Archivos de prueba:** 30 suites pasadas (100%)
- **Tests individuales:** 243 tests pasados (100%)
- **Fallos:** 0
- **Suites principales de la Feature 009:**
  * `src/components/Triage/AssignmentModal.test.jsx`: 10 tests (preselección, persistencia de sugerencia, alerta de confianza baja < 50%, no duplicidad).
  * `src/components/Navbar.test.jsx`: 7 tests (enlace "Analítica" exclusivo para `ANALYST`/`ADMIN`/`OPERATOR`, oculto para `CITIZEN`/`TECHNICIAN`, logout).
  * `src/pages/HomePage.test.jsx`: 7 tests (enlaces contextuales, analítica urbana y logout).
  * `src/App.test.jsx`: 11 tests (guardas RBAC sobre `/analytics` y `/operator-panel`, 403/redirección con `accessDenied`).
  * `src/pages/AnalyticsPage.test.jsx`: 6 tests (carga, filtros reactivos, paginación, KPIs).
  * `src/components/analytics/AnalyticsKpiCards.test.jsx`: 4 tests (MTTT, MTTR, SLAs, asistente).
  * `src/components/analytics/AnalyticsDateFilter.test.jsx`: 6 tests (cambio de presets y distritos).
  * `src/components/analytics/TerritorialChart.test.jsx`: 3 tests (barras SVG y tooltips).
  * `src/components/analytics/SlaComplianceChart.test.jsx`: 3 tests (semáforo de cumplimiento).
  * `src/components/analytics/EmptyAnalyticsState.test.jsx`: 2 tests (estado sin datos).
  * `src/components/analytics/ExportPdfButton.test.jsx`: 7 tests (generación PDF y bloqueo reactivo).
  * `src/utils/reportUtils.test.js`: 18 tests (formato institucional, tablas y filtros).
  * `src/services/analyticsService.test.js`: 7 tests (contratos Axios y parámetros de consulta).

### 4.3 Verificación de Calidad de Código (ESLint)
Comando ejecutado:
```bash
cd frontend && npm run lint
```
**Resultado:**
- `0 errors, 0 warnings` (Código 100% conforme a las reglas del linter y React 19).

---

## 5. Guía de Validación Paso a Paso para la Fase de Aceptación (`sdd-validate`)

### Paso 1: Verificación de Navegación y Control de Acceso RBAC (RF-19, RF-20, RF-21)
1. **Iniciar sesión como Ciudadano (`CITIZEN`):**
   - Comprobar que en la cabecera superior y en la barra de navegación no figura ningún enlace a *"Analítica"*.
   - Intentar forzar la navegación manual a `http://localhost:5173/analytics`.
   - **Resultado esperado:** Redirección automática a la raíz (`/`) desplegando el banner de advertencia: *"Acceso Restringido por Control RBAC"*.
2. **Iniciar sesión como Técnico (`TECHNICIAN`):**
   - Comprobar que se muestra *"Mis Asignaciones"* pero NO *"Analítica"*.
   - Intentar forzar la navegación manual a `/analytics`.
   - **Resultado esperado:** Redirección a `/` con aviso de acceso denegado.
3. **Iniciar sesión como Operador, Analista o Administrador (`OPERATOR`, `ANALYST`, `ADMIN`):**
   - Comprobar que aparece visible el enlace *"Analítica"* tanto en el `Navbar` como en las tarjetas destacadas del portal `HomePage`.
   - Hacer clic en *"Analítica"*.
   - **Resultado esperado:** Acceso exitoso a la ruta `/analytics` cargando el panel analítico.

### Paso 2: Validación del Panel de Analítica, KPIs y Gráficos (RF-1 al RF-11)
1. En `/analytics`, comprobar la visualización de las 5 tarjetas de KPI: Volumen Total, Tiempo Medio de Triaje (MTTT), Tiempo Medio de Resolución (MTTR), Tasa de Cumplimiento de SLA y Desempeño del Asistente.
2. Comprobar los dos gráficos visuales SVG interactivos:
   - Gráfico horizontal de distribución por Distrito municipal.
   - Gráfico de barras de Cumplimiento de SLA por Departamento con línea meta institucional del 80%.
3. Cambiar el preset temporal a *"Últimos 7 días"* o seleccionar un Distrito específico (ej. *"Centro"*).
   - **Resultado esperado:** Las métricas y gráficos se recalculan reactivamente sin recarga de página.
4. Aplicar un filtro que no tenga incidencias registradas (ej. un barrio sin reportes o año sin datos).
   - **Resultado esperado:** Se despliega el componente visual informativo `EmptyAnalyticsState`, los KPIs permanecen en 0 sin errores en consola (cero divisiones por cero) y el botón de exportación a PDF se inhabilita.

### Paso 3: Validación de la Exportación de Informes en PDF (RF-12, RF-13, RF-14)
1. Con un filtro activo que contenga incidencias, pulsar sobre el botón *"Exportar Informe PDF"*.
2. **Resultado esperado:**
   - Indicador visual temporal en el botón (*"Generando PDF..."*).
   - Descarga directa de un archivo PDF con nombre descriptivo (`informe-analitica-urbana-*.pdf`).
   - El documento incluye cabecera municipal oficial, bloque de KPIs y tabla paginada de incidencias filtradas.

### Paso 4: Validación de la Asistencia Inteligente en Asignación (RF-15, RF-16, RF-17)
1. Iniciar sesión como `OPERATOR` o `ADMIN` y acceder al panel de triaje (`/operator-panel`).
2. Abrir el modal de asignación sobre una incidencia validada (ej. con título o categoría de *"Baches en calzada"*).
3. **Resultado esperado (RF-15):**
   - Se muestra la insignia verde: *"Sugerencia Inteligente: Vías Públicas ([X]%)"*.
   - El departamento recomendado queda automáticamente preseleccionado y sus técnicos se cargan bajo demanda.
4. Desmarcar manualmente el departamento sugerido o marcar un departamento adicional (RF-16):
   - **Resultado esperado:** La insignia con la recomendación original permanece visible e inmutable para referencia del operador.
5. Probar con una incidencia de descripción muy escueta o ambigua (confianza < 50%) (RF-17):
   - **Resultado esperado:** Se muestra la alerta visual explícita de advertencia: *"Atención: Nivel de confianza bajo (< 50%). Requiere confirmación rigurosa del operador."*

---

## 6. Conclusión y Dictamen

La implementación de la Feature 009 satisface íntegramente la especificación de requisitos funcionales (RF-1 a RF-21), arquitecturales, no funcionales y de seguridad RBAC. Las suites automatizadas de backend y frontend garantizan 100% de éxito con cero regresiones en el sistema. Se recomienda proceder a la fase de validación formal (`sdd-validate`).
