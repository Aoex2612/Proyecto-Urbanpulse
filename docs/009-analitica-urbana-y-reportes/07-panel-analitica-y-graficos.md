# Tarea 7: Componentes del Panel de Analítica y Gráficos Visuales en Frontend

**Feature:** `009-analitica-urbana-y-reportes`  
**Estado:** Completada ✅  
**Requisitos funcionales relacionados:** RF-1 al RF-11, RF-18  

---

## 1. Resumen de la Tarea

En esta tarea se ha implementado el conjunto integral de componentes de interfaz de usuario y la página principal del **Panel de Analítica Urbana y Reportes Ejecutivos** de UrbanPulse en React 19, JavaScript ES Modules, SVG interactivo nativo, diseño UI de alta fidelidad según la filosofía `frontend-design` y Vitest:

1. **Tarjetas de Métricas Ejecutivas (`frontend/src/components/analytics/AnalyticsKpiCards.jsx` y `AnalyticsKpiCards.css`):**
   - Panel de KPIs operacionales con 5 tarjetas ejecutivas:
     * **Volumen Total:** Número consolidado de incidencias en el período con desglose cuantitativo de resueltas vs pendientes.
     * **Tiempo Medio de Triaje (MTTT):** Medido en horas con etiqueta explicativa del ciclo de validación inicial.
     * **Tiempo Medio de Resolución (MTTR):** Medido en horas con etiqueta explicativa del ciclo resolutivo definitivo.
     * **Tasa de Cumplimiento de SLA:** Porcentaje global con desglose de incidencias en plazo vs fuera de plazo e insignias dinámicas de salud (`Cumplimiento óptimo` $\ge 80\%$, `En seguimiento` $50-79\%$, `Atención prioritaria` $< 50\%$).
     * **Desempeño del Asistente Inteligente:** Tasa de adopción por operadores, nivel de confianza promedio y total de recomendaciones emitidas vs aceptadas (RF-18).
   - Resiliencia completa ante valores nulos, no definidos o conjuntos vacíos con valores por defecto y sin divisiones por cero (RF-11).

2. **Panel de Filtros Temporales y Territoriales (`frontend/src/components/analytics/AnalyticsDateFilter.jsx` y `AnalyticsDateFilter.css`):**
   - Controles tipo chips/segmentos para selección rápida de rangos predefinidos: *"Hoy"* (`TODAY`), *"Últimos 7 días"* (`LAST_7_DAYS`), *"Últimos 30 días"* (`LAST_30_DAYS`, por defecto), *"Últimos 3 meses"* (`LAST_3_MONTHS`), *"Últimos 6 meses"* (`LAST_6_MONTHS`), *"Último año"* (`LAST_YEAR`) y *"Año específico"* (`SPECIFIC_YEAR`).
   - Selector numérico desplegable de años cuando el preset activo es `SPECIFIC_YEAR`.
   - Selectores jerárquicos para los 11 distritos oficiales del término municipal de Málaga (*Centro, Este, Ciudad Jardín, Bailén-Miraflores, Palma-Palmilla, Cruz de Humilladero, Carretera de Cádiz, Churriana, Campanillas, Puerto de la Torre, Teatinos-Universidad*) y sus respectivos barrios.
   - Botón interactivo de restablecimiento de filtros a valores por defecto con icono contextual.
   - Notificación reactiva inmediata a través del prop `onFilterChange`.

3. **Gráfico SVG Nativo de Distribución Territorial (`frontend/src/components/analytics/TerritorialChart.jsx` y `TerritorialChart.css`):**
   - Visualización de barras horizontales SVG nativas escaladas proporcionalmente respecto al máximo territorial.
   - Sin dependencias pesadas de terceros, garantizando renderizado ligero, reactivo y nítido.
   - Accesibilidad semántica completa con `role="img"`, `aria-label`, `<title>` y `<desc>`.
   - Interactividad con ratón y foco de teclado mostrando panel tooltip con métricas de desglose (total, resueltas, pendientes y % SLA).
   - Manejo elegante de conjuntos sin registros territoriales.

4. **Gráfico SVG Nativo de Cumplimiento de SLA por Departamento (`frontend/src/components/analytics/SlaComplianceChart.jsx` y `SlaComplianceChart.css`):**
   - Representación porcentual de cumplimiento de acuerdos de nivel de servicio (0% a 100%) por departamento municipal.
   - Codificación cromática de accesibilidad institucional: Verde esmeralda (`#10b981`) para $\ge 80\%$, Ámbar cálido (`#f59e0b`) para $50-79\%$ y Carmesí de alerta (`#ef4444`) para $< 50\%$.
   - Línea guía de referencia en el 80% como indicador de meta institucional del Ayuntamiento de Málaga.
   - Accesibilidad WAI-ARIA y tooltips interactivos con métricas operativas por departamento.

5. **Componente de Estado Vacío Orientador (`frontend/src/components/analytics/EmptyAnalyticsState.jsx` y `EmptyAnalyticsState.css`):**
   - Estado visual informativo y amigable ante la ausencia de incidencias en el rango o territorio seleccionado (RF-11).
   - Ilustración vectorial limpia, mensaje explicativo y botón de acción directa para restablecer filtros.

6. **Constantes del Módulo (`frontend/src/utils/analyticsConstants.js`):**
   - Catálogo inmutable de distritos oficiales de Málaga, diccionario de barrios representativos por demarcación y catálogo de presets temporales.

7. **Página Principal del Panel (`frontend/src/pages/AnalyticsPage.jsx` y `AnalyticsPage.css`):**
   - Conexión reactiva con `analyticsService.getAnalyticsSummary` y `authService.getUser`.
   - Cabecera institucional con marca municipal, distintivo del periodo activo y navegación hacia Inicio y Centro de Triaje.
   - Integración nativa del botón `ExportPdfButton` bloqueado reactivamente cuando no hay datos.
   - Gestión integral de estados: `loading` con spinner accesible, `error` con mensaje y reintento asistido, `empty` con `EmptyAnalyticsState` y `success` con KPIs, cuadrícula gráfica y tabla detallada de incidencias.
   - Tabla paginada de incidencias con badges de prioridad y estado de triaje conformes a las pautas de color de la plataforma.

8. **Suite de Pruebas Unitarias Exhaustivas (Vitest + React Testing Library):**
   - Cobertura total para cada nuevo componente y la integración de la página principal (6 suites dedicadas).

---

## 2. Detalle de Componentes Implementados

### 2.1 Tarjetas de Métricas Ejecutivas (`AnalyticsKpiCards`)

Ubicación: `frontend/src/components/analytics/AnalyticsKpiCards.jsx`

Presenta de forma condensada los indicadores clave de rendimiento operacional:
```jsx
<AnalyticsKpiCards
  operationalMetrics={summaryData.operationalMetrics}
  assistantPerformance={summaryData.assistantPerformance}
/>
```

- **Resiliencia ante conjuntos vacíos (RF-11):** Utiliza `formatMetricHours` y `formatMetricPercent` de `reportUtils.js` evitando valores `NaN`, `Infinity` o divisiones por cero.
- **Categorización de salud de SLA:** Mapea el ratio numérico a estilos diferenciados (`kpi-sla-good`, `kpi-sla-warning`, `kpi-sla-danger`).

---

### 2.2 Filtro Temporal y Territorial (`AnalyticsDateFilter`)

Ubicación: `frontend/src/components/analytics/AnalyticsDateFilter.jsx`

Gestiona las dimensiones temporales y espaciales de la consulta:
- Control de presets mediante botones tipo chip con estado activo (`.is-active`, `aria-pressed="true"`).
- Despliegue condicional del selector de año si `preset === 'SPECIFIC_YEAR'`.
- Menú de distritos de Málaga con actualización reactiva y reseteo automático de barrios si el barrio activo no pertenece al distrito recién seleccionado.

---

### 2.3 Visualizaciones Gráficas SVG Nativas (`TerritorialChart` y `SlaComplianceChart`)

Ubicación: `frontend/src/components/analytics/TerritorialChart.jsx` y `SlaComplianceChart.jsx`

Construidos enteramente con elementos vectoriales estándar (`<svg>`, `<rect>`, `<line>`, `<text>`, `<g>`):
- **Cálculo de escala matemática:**
  * En distribución territorial: `(totalIncidents / maxIncidents) * barAreaWidth`.
  * En cumplimiento SLA: `(slaComplianceRate / 100) * barAreaWidth`.
- **Accesibilidad ARIA:** `<svg role="img" aria-label="..." aria-describedby="...">`, títulos internos y descriptores para lectores de pantalla.
- **Micro-interactividad:** Estados de `hover` y `focus` con tooltip flotante semántico (`role="status"`).

---

### 2.4 Estado Vacío Orientador (`EmptyAnalyticsState`)

Ubicación: `frontend/src/components/analytics/EmptyAnalyticsState.jsx`

Sigue el principio de diseño de orientación al usuario ante conjuntos sin registros:
- Iconografía SVG municipal temática.
- Explicación constructiva del motivo de la ausencia de registros.
- Botón *"Restablecer filtros"* que reinicia la consulta al preset estándar de 30 días.

---

### 2.5 Página Principal (`AnalyticsPage`)

Ubicación: `frontend/src/pages/AnalyticsPage.jsx`

Integra armónicamente todos los módulos:
- **Flujo de datos seguro:** Carga inicial automática con el preset `LAST_30_DAYS` y actualización asíncrona ante cualquier interacción de filtros.
- **Tabla de registros:** Visualiza `filteredIncidents` con paginación de 10 elementos por vista y controles accesibles de avance y retroceso.
- **Botón de exportación:** `ExportPdfButton` integrado en la barra de acciones superior, pasando el DTO consolidado y el usuario activo.

---

## 3. Pruebas Unitarias y Verificación

Se han creado y ejecutado las suites de pruebas con Vitest y React Testing Library:

| Archivo de Prueba | Componente Evaluado | Casos de Prueba Clave |
|---|---|---|
| `AnalyticsKpiCards.test.jsx` | `AnalyticsKpiCards` | Renderizado de las 5 tarjetas, formateo de horas/%, resiliencia ante nulos (RF-11), clases de salud SLA. |
| `AnalyticsDateFilter.test.jsx` | `AnalyticsDateFilter` | Renderizado de presets, selección de 7d/30d, selector de año, distritos de Málaga, reseteo de filtros. |
| `TerritorialChart.test.jsx` | `TerritorialChart` | Estructura SVG accesible, barras proporcionales, tooltip interactivo, estado vacío. |
| `SlaComplianceChart.test.jsx` | `SlaComplianceChart` | Etiquetas de departamentos, umbrales de color (verde/ámbar/rojo), meta 80%, tooltip de hover. |
| `EmptyAnalyticsState.test.jsx` | `EmptyAnalyticsState` | Renderizado de título, mensaje y callback de restablecimiento de filtros. |
| `AnalyticsPage.test.jsx` | `AnalyticsPage` | Carga inicial con API, renderizado completo, estado vacío (RF-11), cambio de filtros, reintento de error. |

### Resultados de Ejecución:
- **Pruebas Vitest:** `29/29 test files pasados`, `223/223 tests unitarios pasados` (100% de éxito).
- **Linter (ESLint):** `0 errors, 0 warnings`.
- **Vite Build (`npm run build`):** Compilación de producción completada con código 0.
