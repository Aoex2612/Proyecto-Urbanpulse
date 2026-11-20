# Tarea 6: Servicios API y Módulo de Exportación a PDF en Frontend

**Feature:** `009-analitica-urbana-y-reportes`  
**Estado:** Completada ✅  
**Requisitos funcionales relacionados:** RF-12, RF-13, RF-14  

---

## 1. Resumen de la Tarea

En esta tarea se ha desarrollado la capa de integración de cliente frontend y el módulo de generación y exportación de informes ejecutivos en formato PDF para el panel de analítica urbana de UrbanPulse en React 19, JavaScript ES Modules, jsPDF y Vitest:

1. **Servicio Cliente API (`frontend/src/services/analyticsService.js`):** Cliente HTTP desacoplado basado en Axios (`api.js`) para invocar los endpoints protegidos `/api/analytics/summary` y `/api/analytics/recommend-department`.
2. **Utilidades de Compilación de Informes PDF (`frontend/src/utils/reportUtils.js`):** Motor de maquetación y generación vectorial en PDF (A4 vertical) con cabecera institucional del Ayuntamiento de Málaga, metadatos de auditoría, bloque ejecutivo de KPIs operacionales y tabla detallada de incidencias filtradas mediante `jspdf-autotable`.
3. **Componente de Exportación Accesible (`frontend/src/components/analytics/ExportPdfButton.jsx` y `ExportPdfButton.css`):** Botón interactivo con bloqueo reactivo ante ausencia de incidencias (RF-14), indicador de progreso (spinner y texto "Generando PDF..."), prevención de doble clic y accesibilidad completa conforme a directrices WAI-ARIA.
4. **Barril de Exportación (`frontend/src/components/analytics/index.js`):** Exposición centralizada del componente para su posterior consumo en las vistas del panel de analítica (`AnalyticsPage`).
5. **Pruebas Unitarias Exhaustivas con Vitest:** 32 nuevos tests unitarios cubriendo servicios, utilidades de formateo, maquetación multipágina del PDF, reactividad del botón y estados de error.

---

## 2. Arquitectura y Componentes Desarrollados

### 2.1 Servicio Cliente HTTP (`frontend/src/services/analyticsService.js`)

Centraliza las llamadas HTTP hacia el backend protegidas mediante cabeceras `Authorization: Bearer <token>`:

- **`getAnalyticsSummary(params = {})`**:
  - Endpoint: `GET /api/analytics/summary`
  - Parámetros admitidos: `preset`, `startDate`, `endDate`, `year`, `district`, `neighborhood`.
  - Retorna: Objeto consolidado `AnalyticsSummaryResponse` con métricas operativas, territoriales, departamentales, temáticas, rendimiento de asistencia y el listado de incidencias filtradas.
- **`getDepartmentRecommendation(params = {})`**:
  - Endpoint: `GET /api/analytics/recommend-department`
  - Parámetros admitidos: `incidentId` (modo persistido) o `title`, `description`, `category` (modo tiempo real).
  - Retorna: Objeto `DepartmentRecommendationResponse` con `suggestedDepartmentId`, `suggestedDepartmentName`, `confidencePercentage`, `isLowConfidenceWarning` y `reasoning`.

Ambos métodos cuentan con documentación JSDoc completa en español detallando tipos, descripciones y excepciones.

---

### 2.2 Motor de Generación de Informes PDF (`frontend/src/utils/reportUtils.js`)

Implementa la función principal `generateAnalyticsPdf(summaryData, currentUser, options = {})` apoyada en `jsPDF` y `jspdf-autotable`:

1. **Guarda de Seguridad y Validación de Datos (RF-14):**
   - Comprueba de forma estricta que existan incidencias: `!summaryData || !Array.isArray(summaryData.filteredIncidents) || summaryData.filteredIncidents.length === 0`.
   - Si no hay datos, lanza de forma determinista la excepción `Error('No hay incidencias disponibles para generar el informe PDF')`.
2. **Cabecera Institucional Oficial (RF-13):**
   - Franja decorativa superior en color pizarra oscuro (`#0F172A`).
   - Título oficial: *"UrbanPulse — Informe Ejecutivo de Analítica Urbana"*.
   - Subtítulo municipal: *"Ayuntamiento de Málaga · Plataforma de Gestión Urbana Inteligente"*.
3. **Bloque de Metadatos y Criterios de Filtrado:**
   - Fecha y hora exacta de emisión formateada en español (`formatDate`).
   - Usuario emisor autenticado (`fullName` o `email`) y rol operativo (`currentUser.role`).
   - Resumen legible de los filtros aplicados: Período analizado (ej. *"Últimos 30 días"*, *"Año 2026"* o rango de fechas), Distrito municipal y Barrio.
   - Conteo total de registros incluidos en el informe.
4. **Resumen Ejecutivo de Indicadores Clave (KPIs):**
   - Tabla estructurada con bordes suaves que recopila:
     - **Volumen de Incidencias:** Total, resueltas y pendientes.
     - **Tiempo Medio de Triaje (MTTT):** En horas con un decimal (ej. `3.5 h`).
     - **Tiempo Medio de Resolución (MTTR):** En horas con un decimal (ej. `28.0 h`).
     - **Tasa de Cumplimiento de SLA:** Porcentaje global (ej. `85.5%`), incidencias en plazo y fuera de plazo.
5. **Tabla Detallada de Incidencias Filtradas con `autoTable`:**
   - Columnas: `ID`, `Título`, `Categoría`, `Distrito`, `Barrio`, `Prioridad`, `Estado`, `Fecha`.
   - Cabecera en azul oscuro (`#0F172A`), texto blanco en negrita y filas alternas cebradas (`#F8FAFC`).
   - Ajuste automático de anchos de columna y salto de línea en títulos largos.
6. **Pie de Página Dinámico en Todas las Páginas:**
   - Línea sutil de separación en la base de la página.
   - Leyenda de uso oficial: *"UrbanPulse © 2026 · Ayuntamiento de Málaga · Documento Oficial de Uso Interno"*.
   - Numeración correlativa calculada dinámicamente: *"Página X de Y"*.
7. **Control de Descarga o Retorno:**
   - Si `options.returnDoc === true`, devuelve la instancia `doc` directamente (útil para aserciones en pruebas automatizadas).
   - En caso contrario, ejecuta `doc.save(filename)` con nomenclatura automática `informe-analitica-urbanpulse-YYYY-MM-DD.pdf`.

---

### 2.3 Componente `ExportPdfButton` (`frontend/src/components/analytics/ExportPdfButton.jsx`)

Componente de interfaz para la interacción de usuario con el flujo de exportación:

- **Bloqueo Reactivo (RF-14):**
  - Si `summaryData` no contiene incidencias, el botón se deshabilita automáticamente (`disabled = true`).
  - Muestra el tooltip aclaratorio `title="Se requieren datos para exportar el informe en PDF"`.
  - Cuando existen incidencias y no hay bloqueo externo, el botón se activa con `title="Exportar informe en formato PDF"`.
- **Feedback Visual y Estado de Compilación:**
  - Estado reactivo `isGenerating`.
  - Al iniciar la exportación, el botón se inhabilita para prevenir peticiones concurrentes o dobles clics, mostrando un spinner SVG giratorio con animación CSS y el texto *"Generando PDF..."*.
  - Al finalizar, restablece el estado y notifica mediante el callback opcional `onSuccess(doc)`.
- **Gestión Accesible de Excepciones:**
  - En caso de fallo durante la renderización del PDF, captura el error, reestablece el botón, invoca `onError(err)` y renderiza un contenedor `<span role="alert" className="export-pdf-error">` legible por lectores de pantalla.
- **Accesibilidad ARIA:**
  - Atributos dinámicos `aria-label`, `aria-busy={isGenerating}` y control de foco accesible mediante `:focus-visible`.

---

## 3. Verificación y Cobertura de Pruebas

Se han creado tres suites de pruebas unitarias específicas con Vitest y `@testing-library/react`:

### 3.1 `frontend/src/services/analyticsService.test.js` (7 tests)
- Invocación a `GET /api/analytics/summary` con parámetros vacíos y personalizados.
- Propagación transparente de errores HTTP (403, 500).
- Invocación a `GET /api/analytics/recommend-department` por `incidentId` y por atributos de texto directo.
- Verificación de exportación por defecto y nombrada.

### 3.2 `frontend/src/utils/reportUtils.test.js` (18 tests)
- Diccionario de etiquetas `PRESET_LABELS`.
- Formateo de métricas horarias (`formatMetricHours`) y porcentajes (`formatMetricPercent`) ante valores normales, cero y valores nulos/inválidos (`N/A`, `0.0%`).
- Composición de descripciones de filtros (`formatFilterDescription`).
- Lanzamiento de excepción si `summaryData` es nulo o la lista de incidencias está vacía (RF-14).
- Retorno de instancia `doc` con `returnDoc: true`.
- Comprobación de generación multipágina para volúmenes amplios (65 incidencias).
- Invocación a `doc.save` con nombre personalizado y nombre por defecto con fecha actual.

### 3.3 `frontend/src/components/analytics/ExportPdfButton.test.jsx` (7 tests)
- Renderizado en estado deshabilitado con tooltip aclaratorio ante datos nulos o lista vacía (RF-14).
- Renderizado habilitado cuando existen incidencias.
- Respeto a la propiedad externa `disabled={true}`.
- Disparo de `generateAnalyticsPdf` y callback `onSuccess` tras pulsar el botón.
- Feedback visual de progreso ("Generando PDF...") y atributo `aria-busy="true"`.
- Despliegue de mensaje de error accesible (`role="alert"`) y ejecución de `onError` ante excepciones.

### 3.4 Resultados Globales de Verificación

```bash
# Vitest (100% de suites en verde)
Test Files  23 passed (23)
     Tests  199 passed (199)

# ESLint (0 advertencias, 0 errores)
npm run lint -> 0 errors, 0 warnings
```

---

## 4. Estado de Requisitos Funcionales

| Requisito | Descripción | Estado | Componente Responsable |
| :--- | :--- | :---: | :--- |
| **RF-12** | Botón de exportación habilitado mientras existan incidencias en los filtros. | ✅ Implementado | `ExportPdfButton.jsx` |
| **RF-13** | Generación y descarga directa de informe PDF estructurado con cabecera, KPIs y tabla detallada. | ✅ Implementado | `reportUtils.js` |
| **RF-14** | Bloqueo reactivo del botón y mensaje informativo cuando la lista de incidencias esté vacía. | ✅ Implementado | `ExportPdfButton.jsx` & `reportUtils.js` |
