# 009 — Analítica Urbana y Reportes — Plan

_Cómo se implementa lo descrito en `spec.md`. Respeta estrictamente la `constitution/` y los principios de diseño de UrbanPulse._

## Enfoque

La solución amplía la plataforma UrbanPulse dotándola de un subsistema integral de analítica operacional, evaluación estricta de niveles de servicio (SLAs), correlación territorial histórica, exportación de informes oficiales en PDF y asistencia heurística a la asignación departamental, respetando el desacoplamiento en tres capas y la gobernanza del proyecto:

1. **Capa de Negocio y Métricas de SLAs (Backend):**
   - Servicio `AnalyticsService` que consulta `IncidenciaRepository`, `CambiosEstadoRepository` y `TareasRepository` para calcular de forma agregada el Tiempo Medio de Triaje (MTTT) y Tiempo Medio de Resolución (MTTR) a partir de las marcas temporales inmutables de auditoría.
   - Evaluación determinista del cumplimiento de SLA por incidencia contrastando la duración real contra los umbrales pactados en la especificación: `CRITICAL` (3 días / 72 h), `HIGH` (7 días), `MEDIUM` (15 días) y `LOW` (30 días).
2. **Motor de Recomendación Inteligente Autónomo (Backend):**
   - Servicio `IntelligentRecommendationService` que procesa el contenido léxico (título, descripción, categoría y territorio) frente a una matriz ponderada de términos municipales y frecuencias históricas, infiriendo el departamento con un grado de confianza porcentual ($0\%$ a $100\%$) y activando una advertencia si es $< 50\%$.
3. **Capa de Presentación REST y Seguridad RBAC (Backend):**
   - DTOs inmutables bajo `org.bulbaseros.dto` y mapeador `AnalyticsMapper` en `org.bulbaseros.mappers`.
   - Controlador `AnalyticsController` exponiendo `/api/analytics/summary`, `/api/analytics/recommend-department` y `/api/analytics/report-data`.
   - Actualización de `SecurityConfig` autorizando exclusivamente a los roles `ANALYST`, `ADMIN` y `OPERATOR`, y denegando con HTTP 403 a `CITIZEN` y `TECHNICIAN`.
4. **Visualización Reactiva, Gráficos y Exportación PDF (Frontend):**
   - Cliente `analyticsService.js` para consumir los endpoints analíticos.
   - Página principal `AnalyticsPage.jsx` con tarjetas KPI (`AnalyticsKpiCards`), barra de filtros temporales y territoriales (`AnalyticsDateFilter`), gráficos interactivos en SVG (`TerritorialChart`, `SlaComplianceChart`), estado vacío amigable (`EmptyAnalyticsState`) y botón de exportación a PDF (`ExportPdfButton`).
   - Generación de informe PDF estructurado (resumen de KPIs y tabla de incidencias).
   - Integración de la recomendación asistida en `AssignmentModal.jsx` (preselección automática, badge con departamento recomendado y aviso de confianza baja).
   - Enlace "Analítica" en `Navbar.jsx` y ruta protegida en `App.jsx` con `RoleGuard`.
5. **Estrategia Integral de Calidad y Pruebas:**
   - Pruebas unitarias de servicios, DTOs y mappers en backend con Mockito y JUnit 5.
   - Pruebas de integración de controladores REST y seguridad con `MockMvc`.
   - Pruebas de componentes React y utilidades en frontend con Vitest y Testing Library.

---

## Implementación

_Pasos técnicos concretos, en orden. Indica los archivos y módulos afectados._

### 1. DTOs de Analítica y Mapeadores de Dominio (Backend)
- Crear en `Backend/src/main/java/org/bulbaseros/dto/`:
  - `AnalyticsFilterRequest.java`: DTO para encapsular parámetros de filtrado temporal (`startDate`, `endDate`, `preset`) y territorial (`district`, `neighborhood`).
  - `OperationalMetricsResponse.java`: DTO con totales de incidencias, MTTT (horas), MTTR (horas), porcentaje global de cumplimiento de SLA e incidencias fuera de plazo.
  - `TerritorialMetricResponse.java`: DTO con métricas agregadas por distrito o barrio (total, resueltas, cumplimiento de SLA).
  - `DepartmentMetricResponse.java`: DTO con rendimiento por departamento (total asignadas, resueltas, tiempo medio y % SLA).
  - `CategoryMetricResponse.java`: DTO con distribución y tasa de resolución por categoría temática.
  - `DepartmentRecommendationResponse.java`: DTO con departamento sugerido, grado de confianza (0-100%), justificación y bandera de confianza baja.
  - `AssistantPerformanceResponse.java`: DTO con confianza media y métricas de recomendación del motor.
  - `AnalyticsSummaryResponse.java`: DTO consolidado con todos los bloques anteriores y listado detallado de incidencias filtradas para la tabla y exportación.
- Crear mapeador `AnalyticsMapper.java` en `Backend/src/main/java/org/bulbaseros/mappers/` extendiendo `BaseMapper` con transformaciones inmutables y Javadoc.

### 2. Consultas Especializadas en Repositorios (Backend)
- Actualizar `IncidenciaRepository.java` en `org.bulbaseros.repository`:
  - Añadir métodos de consulta con filtros combinados por rango de fecha (`createdAt BETWEEN`), distrito y barrio.
  - Consultas de recuento y agrupación por categoría y demarcación territorial.
- Actualizar `CambiosEstadoRepository.java`:
  - Añadir consultas para recuperar transiciones de estado por incidencia y marcas de tiempo de primera validación (`VALIDATED` / `REJECTED`) y resolución (`RESOLVED`).

### 3. Servicios de Negocio: Recomendación Inteligente y Analítica Operacional (Backend)
- Implementar `IntelligentRecommendationService.java` en `org.bulbaseros.service`:
  - Algoritmo léxico ponderado con normalización probabilística de coincidencia sobre departamentos municipales.
  - Retorno de DTO `DepartmentRecommendationResponse` con estimación porcentual y bandera de advertencia para confianza $< 50\%$.
- Implementar `AnalyticsService.java` en `org.bulbaseros.service`:
  - Cálculo de deltas de tiempo para MTTT y MTTR con prevención de división por cero.
  - Lógica de evaluación de SLA por prioridad (`CRITICAL`: 3 días, `HIGH`: 7 días, `MEDIUM`: 15 días, `LOW`: 30 días).
  - Agrupación dinámica por distrito, barrio, categoría y departamento.
  - Integración de los resultados consolidados en `AnalyticsSummaryResponse`.

### 4. Controladores REST y Seguridad RBAC (Backend)
- Crear `AnalyticsController.java` en `org.bulbaseros.controller`:
  - `GET /api/analytics/summary`: Consulta consolidada de métricas con parámetros de filtro opcionales (`preset`, `district`, `neighborhood`, `year`).
  - `GET /api/analytics/recommend-department`: Sugerencia asistida a partir de título, descripción y categoría.
- Actualizar `SecurityConfig.java`:
  - Añadir regla `.requestMatchers("/api/analytics/**").hasAnyRole("ANALYST", "ADMIN", "OPERATOR")`.

### 5. Servicios API y Utilidades en Cliente Web (Frontend)
- Crear `frontend/src/services/analyticsService.js`:
  - Métodos con Axios (`getAnalyticsSummary`, `getDepartmentRecommendation`).
- Crear utilidades de cálculo y maquetación de informes en `frontend/src/utils/reportUtils.js`:
  - Estructuración de datos y compilación de informe imprimible en PDF con cabecera oficial, resumen ejecutivo de KPIs y tabla detallada.

### 6. Componentes del Panel de Analítica y Gráficos Visuales (Frontend)
- Crear en `frontend/src/components/analytics/`:
  - `AnalyticsKpiCards.jsx` y estilos: Tarjetas métricas (Total, MTTT, MTTR, Tasa de Cumplimiento de SLA).
  - `AnalyticsDateFilter.jsx`: Selectores rápidos (Hoy, 7 días, 30 días, 3 meses, 6 meses, Año, Año específico) y desplegables de Distrito y Barrio.
  - `TerritorialChart.jsx`: Gráfica interactiva visual de barras SVG para distribución territorial.
  - `SlaComplianceChart.jsx`: Gráfica interactiva de cumplimiento de SLA por departamento y prioridad.
  - `EmptyAnalyticsState.jsx`: Mensaje y gráfico informativo ante ausencia de incidencias en el rango.
  - `ExportPdfButton.jsx`: Botón de descarga con feedback de generación y bloqueo reactivo ante listados vacíos.
- Crear vista principal `AnalyticsPage.jsx` y su hoja de estilos `AnalyticsPage.css` en `frontend/src/pages/`.

### 7. Integración de Asistencia Inteligente en el Triaje y Navegación (Frontend)
- Modificar `frontend/src/components/operator/AssignmentModal.jsx`:
  - Invocar `analyticsService.getDepartmentRecommendation` al abrir el modal con los datos de la incidencia.
  - Preseleccionar automáticamente el departamento recomendado en el desplegable.
  - Renderizar insignia informativa persistente con el departamento sugerido originalmente, el grado de confianza y la advertencia si es $< 50\%$.
- Actualizar `frontend/src/components/Navbar.jsx`:
  - Incorporar enlace "Analítica" hacia `/analytics` visible únicamente para roles `ANALYST`, `ADMIN` y `OPERATOR`.
- Actualizar `frontend/src/App.jsx`:
  - Registrar la ruta `/analytics` envuelta en `ProtectedRoute` y `RoleGuard` con los roles autorizados.

---

## Decisiones

_Elecciones de diseño relevantes y su justificación técnica. Referencia oficial: [ADR-08](file:///home/alejandro/Documentos/ETSII/4 curso/Desarrollo Software en Plataformas en la Nube/Proyecto-Urbanpulse/docs/ADR/ADR-08-analitica-agregada-sla-y-recomendacion-asistida.md)._

- **Cómputo analítico en capa de servicio sobre PostgreSQL (ADR-08)** — Se procesan las agregaciones mediante consultas indexadas y cálculo en memoria en Java 21, descartando almacenes OLAP externos (ClickHouse/Elasticsearch) por sobredimensión arquitectónica y sobrecoste de mantenimiento para el volumen actual del sistema.
- **Motor heurístico autónomo local (ADR-08)** — El recomendador de departamentos opera de forma autónoma en el servidor sin dependencias de APIs de LLM externas de pago (OpenAI/Anthropic). Se garantiza latencia $< 50$ ms, funcionamiento sin internet y coste cero.
- **Generación estructurada del informe PDF (ADR-08)** — El backend provee el dataset enriquecido consolidado y el cliente compila directamente el documento PDF, descartando frameworks pesados headless como Puppeteer en el servidor para no degradar el consumo de memoria del backend.
- **Gráficos interactivos en SVG nativo** — Las visualizaciones de distribución y SLAs se implementan mediante componentes SVG nativos reactivos y accesibles, evitando librerías de gráficos sobredimensionadas que aumenten el peso del bundle de Vite.

---

## Riesgos

_Qué puede salir mal o requerir cuidado especial, y cómo se mitiga._

- **Riesgo: Divisiones por cero en periodos sin incidencias o sin transiciones resolutivas.**
  - *Mitigación:* Los métodos de cálculo de `AnalyticsService` validan explícitamente los denominadores antes de promediar, retornando `0.0` o `null` seguro y activando el componente `EmptyAnalyticsState` en la interfaz.
- **Riesgo: Tiempos de respuesta lentos en rangos temporales extensos sobre miles de incidencias.**
  - *Mitigación:* Se aprovechan los índices ya existentes sobre `created_at`, `status` e `incident_id` en PostgreSQL, realizando agregaciones acotadas por rangos de fecha indexados.
- **Riesgo: Pérdida de la sugerencia original si el operador manipula el selector desplegable.**
  - *Mitigación:* El estado reactivo de `AssignmentModal` almacena de forma inmutable la recomendación original obtenida del servicio, manteniendo el chip visual visible aunque el valor del `select` cambie.

---

## Estrategia de tests

- **Backend (Unitarios):**
  - `AnalyticsServiceTest`: Verificación de cálculo de MTTT, MTTR, cumplimiento de SLA para todas las prioridades (3d/7d/15d/30d), incidencias en curso vs resueltas y caso de conjunto vacío (cero divisiones por cero).
  - `IntelligentRecommendationServiceTest`: Pruebas del motor léxico con palabras clave típicas, cálculo de porcentajes de confianza y activación de la bandera de confianza baja ($< 50\%$).
  - `AnalyticsMapperTest` y DTO tests: Validación de serialización, inmutabilidad y conversión de proyecciones.
- **Backend (Integración):**
  - `AnalyticsControllerTest` con `MockMvc` y `@WithMockUser`: Comprobación de respuestas 200 OK con payload consolidado, validación de parámetros de filtro y verificación estricta de seguridad RBAC (200 para `ADMIN`/`ANALYST`/`OPERATOR`, 403 para `CITIZEN`/`TECHNICIAN` y 401 para anónimos).
- **Frontend (Unitarios y Componentes):**
  - `AnalyticsPage.test.jsx`: Renderizado de tarjetas KPI, cambio reactivo de filtros y visualización de gráficos.
  - `EmptyAnalyticsState.test.jsx`: Comprobación del estado vacío y desactivación del botón de exportación PDF ante cero datos.
  - `AssignmentModal.test.jsx`: Verificación de preselección automática del departamento recomendado, persistencia del badge informativo y advertencia visual para confianza $< 50\%$.
  - `analyticsService.test.js`: Pruebas de cliente Axios con endpoints mockeados.
