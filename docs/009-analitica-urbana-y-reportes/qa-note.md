# Documento Técnico y Guía de Validación de Calidad (QA Note)
## Feature 009: Analítica Urbana y Reportes

**Fecha:** 8 de noviembre de 2026  
**Rama:** `feature/feature-009-analítica-urbana-y-reportes`  
**Estado:** **Validada y Aprobada al 100% (`sdd-validate`)** ✅  
**Cobertura Funcional:** Historias de Usuario HU22, HU23, HU24 y HU25 (RF-1 al RF-21 completados y verificados)  

---

## 1. Resumen Ejecutivo y Veredicto de Validación

La Feature 009, **Analítica Urbana y Reportes**, dota a los perfiles analíticos y operativos del Ayuntamiento de Málaga (`ANALYST`, `ADMIN`, `OPERATOR`) de una plataforma centralizada para la supervisión de acuerdos de nivel de servicio (SLAs), correlación territorial y estacional de incidencias, exportación oficial de informes ejecutivos en formato PDF y asistencia inteligente probabilística en el triaje y asignación multidepartamental.

### Veredicto de Calidad: ÉXITO ROTUNDO (APROBADO AL 100%)
- **Requisitos Funcionales:** 21 / 21 cubiertos con pruebas dedicadas (100% verde).
- **Backend (Spring Boot 3 + Java 21):** 577 tests ejecutados, 0 fallos, 0 errores, 1 skipped. Cobertura JaCoCo: **93% instrucciones / 75% ramas** en 64 clases.
- **Frontend (React 19 + Vitest):** 30 suites y 243 tests ejecutados, 0 fallos. Cobertura Vitest: **84.03% líneas / 83.09% sentencias / 72.63% ramas** (módulos de analítica por encima del 92%).
- **Calidad de Código:** ESLint `0 errors, 0 warnings` y Vite build exitoso.

---

## 2. Tabla de Trazabilidad de Requisitos Funcionales (EARS) y Cobertura de Tests

| Identificador | Requisito Funcional (EARS) | Componentes Clave Involucrados | Tests Asociados | Estado |
| :--- | :--- | :--- | :--- | :---: |
| **RF-1** | Cálculo y presentación del MTTT (Tiempo Medio de Triaje en horas). | `AnalyticsService.java`, `IncidenciaRepository.java`, `AnalyticsKpiCards.jsx` | `AnalyticsServiceTest` (5 tests), `AnalyticsKpiCards.test.jsx`, `AnalyticsPage.test.jsx` | **PASADO** ✅ |
| **RF-2** | Cálculo y presentación del MTTR (Tiempo Medio de Resolución en horas). | `AnalyticsService.java`, `CambiosEstadoRepository.java`, `AnalyticsKpiCards.jsx` | `AnalyticsServiceTest` (5 tests), `AnalyticsKpiCards.test.jsx`, `AnalyticsPage.test.jsx` | **PASADO** ✅ |
| **RF-3** | Matriz de umbrales máximos de SLA por prioridad (`CRITICAL` 3d/72h, `HIGH` 7d/168h, `MEDIUM` 15d/360h, `LOW` 30d/720h). | `AnalyticsService.java`, `Incidencia.java`, `PrioridadIncidencia.java` | `AnalyticsServiceTest` (5 tests), `AnalyticsRepositoriesTest` | **PASADO** ✅ |
| **RF-4** | Detección de superación de SLA en incidencias resueltas y en curso contra fecha actual. | `AnalyticsService.java` | `AnalyticsServiceTest` (5 tests) | **PASADO** ✅ |
| **RF-5** | Cálculo porcentual de la Tasa de Cumplimiento de SLA (segura sin divisiones por cero). | `AnalyticsService.java`, `OperationalMetricsResponse.java` | `AnalyticsServiceTest`, `AnalyticsKpiCards.test.jsx`, `SlaComplianceChart.test.jsx` | **PASADO** ✅ |
| **RF-6** | Desglose cuantitativo por departamento municipal y categoría. | `AnalyticsService.java`, `DepartmentMetricResponse.java`, `CategoryMetricResponse.java` | `AnalyticsRepositoriesTest`, `AnalyticsServiceTest`, `SlaComplianceChart.test.jsx` | **PASADO** ✅ |
| **RF-7** | Recálculo dinámico por nivel territorial (Distrito o Barrio). | `IncidenciaRepository.java`, `AnalyticsService.java`, `TerritorialChart.jsx` | `AnalyticsRepositoriesTest`, `AnalyticsServiceTest`, `TerritorialChart.test.jsx` | **PASADO** ✅ |
| **RF-8** | Presets temporales preconfigurados (7 presets) y año específico. | `AnalyticsTimePreset.java`, `AnalyticsDateFilter.jsx`, `AnalyticsController.java` | `AnalyticsServiceTest` (10 tests), `DtoAnalyticsValidationTest`, `AnalyticsDateFilter.test.jsx` | **PASADO** ✅ |
| **RF-9** | Actualización reactiva de métricas ante cambios de filtro. | `AnalyticsPage.jsx`, `analyticsService.js` | `AnalyticsPage.test.jsx` | **PASADO** ✅ |
| **RF-10** | Gráficos visuales interactivos SVG nativos (Territorio y SLAs). | `TerritorialChart.jsx`, `SlaComplianceChart.jsx` | `TerritorialChart.test.jsx` (6 tests), `SlaComplianceChart.test.jsx` (6 tests) | **PASADO** ✅ |
| **RF-11** | Manejo de conjuntos vacíos sin divisiones por cero (estado visual informativo). | `AnalyticsService.java`, `EmptyAnalyticsState.jsx` | `AnalyticsServiceTest` (2 tests), `EmptyAnalyticsState.test.jsx`, `AnalyticsPage.test.jsx` | **PASADO** ✅ |
| **RF-12** | Habilitación condicional del botón de exportación PDF mientras haya datos. | `ExportPdfButton.jsx`, `AnalyticsPage.jsx` | `ExportPdfButton.test.jsx` | **PASADO** ✅ |
| **RF-13** | Descarga de documento PDF estructurado oficial con cabecera, KPIs y tabla cebrada. | `reportUtils.js`, `ExportPdfButton.jsx`, `jspdf`, `jspdf-autotable` | `reportUtils.test.js` (18 tests), `ExportPdfButton.test.jsx` | **PASADO** ✅ |
| **RF-14** | Bloqueo reactivo (`disabled`) del botón de exportación PDF ante listado vacío. | `ExportPdfButton.jsx` | `ExportPdfButton.test.jsx`, `reportUtils.test.js` | **PASADO** ✅ |
| **RF-15** | Preselección de departamento sugerido e insignia con grado de confianza (0%-100%). | `AssignmentModal.jsx`, `IntelligentRecommendationService.java` | `IntelligentRecommendationServiceTest` (19 tests), `AssignmentModal.test.jsx` | **PASADO** ✅ |
| **RF-16** | Persistencia visible de la insignia tras cambiar o desmarcar la selección. | `AssignmentModal.jsx` | `AssignmentModal.test.jsx` | **PASADO** ✅ |
| **RF-17** | Advertencia explícita visible cuando la confianza es inferior al 50%. | `AssignmentModal.jsx`, `IntelligentRecommendationService.java` | `IntelligentRecommendationServiceTest`, `AssignmentModal.test.jsx` | **PASADO** ✅ |
| **RF-18** | Métrica de confianza promedio y tasa de adopción del asistente inteligente. | `IntelligentRecommendationService.java`, `AnalyticsKpiCards.jsx` | `IntelligentRecommendationServiceTest`, `AnalyticsServiceTest`, `AnalyticsKpiCards.test.jsx` | **PASADO** ✅ |
| **RF-19** | Enlace visible "Analítica" en `Navbar` y `HomePage` para `ANALYST`, `ADMIN`, `OPERATOR`. | `Navbar.jsx`, `HomePage.jsx` | `Navbar.test.jsx` (7 tests), `HomePage.test.jsx` (7 tests) | **PASADO** ✅ |
| **RF-20** | Protección de la ruta `/analytics` mediante guardas de seguridad y RBAC. | `App.jsx`, `ProtectedRoute.jsx`, `RoleGuard.jsx` | `App.test.jsx` (11 tests), `SecurityConfigTest`, `AnalyticsControllerTest` | **PASADO** ✅ |
| **RF-21** | Denegación de acceso (HTTP 403 / Redirección) para `CITIZEN` y `TECHNICIAN`. | `SecurityConfig.java`, `RoleGuard.jsx`, `App.jsx` | `AnalyticsControllerTest` (15 tests), `SecurityConfigTest`, `App.test.jsx` | **PASADO** ✅ |

---

## 3. Métricas de Cobertura Técnica de Código

### 3.1 Backend (JaCoCo)

| Paquete | % Cobertura Instrucciones | % Ramas Condicionales | Líneas Analizadas | Clases |
| :--- | :---: | :---: | :---: | :---: |
| **`org.bulbaseros.service`** | 92% | 74% | 575 | 143 |
| **`org.bulbaseros.controller`** | 99% | 76% | 51 | 38 |
| **`org.bulbaseros.mappers`** | 97% | 90% | 114 | 38 |
| **`org.bulbaseros.dto`** | 96% | 73% | 166 | 151 |
| **`org.bulbaseros.security`** | 93% | 72% | 79 | 31 |
| **`org.bulbaseros.entity`** | 91% | 64% | 63 | 38 |
| **`org.bulbaseros.config`** | 100% | 100% | - | - |
| **TOTAL PROYECTO BACKEND** | **93%** (10.595 / 11.317) | **75%** (929 / 1.229) | **1.091** | **64 clases** |

### 3.2 Frontend (Vitest Coverage v8)

| Módulo / Archivo | % Líneas | % Ramas | % Funciones | % Sentencias |
| :--- | :---: | :---: | :---: | :---: |
| **Componentes `src/components/analytics/`** | 92.99% | 83.91% | 88.23% | 92.21% |
| **Servicio `src/services/analyticsService.js`** | 100.00% | 100.00% | 100.00% | 100.00% |
| **Utilidades `src/utils/reportUtils.js`** | 99.09% | 92.77% | 100.00% | 99.10% |
| **Componente `src/components/Navbar.jsx`** | 96.15% | 85.71% | 100.00% | 96.15% |
| **Componente `src/components/Triage/AssignmentModal.jsx`** | 95.91% | 73.24% | 88.67% | 93.90% |
| **Página `src/pages/AnalyticsPage.jsx`** | 85.50% | 70.58% | 76.47% | 83.56% |
| **TOTAL PROYECTO FRONTEND** | **84.03%** | **72.63%** | **77.01%** | **83.09%** |

---

## 4. Instrucciones Simples para Pruebas Manuales de Aceptación

Cualquier persona puede verificar la funcionalidad en menos de 5 minutos siguiendo estos pasos directos:

### Paso 1: Control de Acceso RBAC y Navegación (RF-19, RF-20, RF-21)
1. Iniciar sesión como Ciudadano (`vecino@urbanpulse.es` / pass de prueba):
   - Comprobar que en la barra superior **NO** aparece el enlace *"Analítica"*.
   - Si se escribe manualmente en el navegador `http://localhost:5173/analytics`, el sistema redirige inmediatamente a la página de inicio con la advertencia de acceso denegado (HTTP 403).
2. Cerrar sesión e iniciar como Operador (`operador.triaje@urbanpulse.es`) o Administrador:
   - Comprobar que aparece visible el enlace *"Analítica"* en la barra de navegación superior.
   - Pulsar sobre *"Analítica"*: accede directamente a `/analytics`.

### Paso 2: Panel de Analítica, Métricas de SLA y Gráficos SVG (RF-1 al RF-11)
1. En `/analytics`, observar las 5 tarjetas de KPI: Volumen Total, MTTT (horas de triaje), MTTR (horas de resolución), Cumplimiento de SLA (%) y Desempeño del Asistente.
2. Observar los dos gráficos interactivos:
   - Distribución por distrito de Málaga (barras horizontales con tooltips interactivos).
   - Cumplimiento de SLA por departamento (con semáforo de colores y línea de meta del 80%).
3. Probar los filtros rápidos: hacer clic en *"Últimos 7 días"* o filtrar por el distrito *"Centro"*:
   - Las métricas, gráficos y la tabla inferior se actualizan de forma reactiva en tiempo real.
4. Seleccionar un filtro sin incidencias (ej. un barrio sin reportes):
   - Aparece la pantalla amigable *"Sin incidencias registradas en este periodo"*, las métricas numéricas marcan `0.0` sin errores matemáticos y el botón de PDF se desactiva.

### Paso 3: Exportación del Informe en PDF (RF-12, RF-13, RF-14)
1. Con incidencias en pantalla, hacer clic en *"Exportar Informe PDF"*.
2. El botón muestra el spinner *"Generando PDF..."* y se descarga un archivo PDF oficial estructurado con cabecera del Ayuntamiento de Málaga, bloque de KPIs ejecutivos y tabla de incidencias filtradas con formato cebrado y paginación.

### Paso 4: Asistencia Inteligente en el Modal de Triaje (RF-15, RF-16, RF-17, RF-18)
1. Ir al *"Panel de Operador"* (`/operator-panel`) y abrir el modal de asignación de una incidencia (ej. un bache en calzada).
2. Verificar que se muestra la tarjeta verde *"Sugerencia Inteligente: Vías Públicas ([X]%)"* y que dicho departamento aparece preseleccionado automáticamente.
3. Desmarcar la opción o seleccionar otro departamento: la tarjeta con la recomendación original permanece visible.
4. Si la descripción es escueta (ej. "Roto"), verificar que la confianza es menor al 50% y se muestra el distintivo de advertencia: *"Atención: Nivel de confianza bajo (< 50%). Requiere confirmación rigurosa del operador."*
