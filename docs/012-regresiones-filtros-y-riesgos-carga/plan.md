# 012 — Corrección de Regresiones de Filtros y Riesgos de Carga — Plan

_Cómo se implementa lo descrito para resolver la Issue #91 (Fase Inmediata). Respeta las directrices de `constitution/`._

## Enfoque

Acotar la resolución inmediata a los elementos críticos que garantizan la fiabilidad de las pruebas de carga y la experiencia de usuario en frontend, desacoplando la paginación de la API para que sirva de contrapunto y optimización en el **Experimento 4**:

1. **Frontend (UX & Concurrencia):**
   - Limpiar o invalidar `searchResults` inmediatamente al cambiar los criterios de filtro.
   - Mostrar estado visual de carga (`loading`) durante el intervalo de debounce y vuelo de la petición en `MapPage` y `OperatorPanelPage`.
   - Ignorar respuestas fuera de orden o abortadas para no mostrar datos obsoletos.
2. **k6 Fiabilidad:**
   - Asegurar que `setup()` en `k6/experimento-3-emergencia.js`, `k6/baseline.js` y `k6/experimento-2-saturacion.js` invoque la función nativa `fail(...)` de k6 si el login no responde HTTP 200 o no retorna token, abortando de raíz el test.
3. **Alineación Documental:**
   - Detallar en `docs/rendimiento/experimento-3-emergencia.md` la separación clara entre la petición técnica de autenticación en `setup` (1 req) y las peticiones de los escenarios de carga (1.021 checks/reqs: 446 lecturas + 575 escrituras).
4. **Paginación Diferida:**
   - Se mantiene la búsqueda actual para permitir medir el rendimiento basal de tablas grandes sin paginación en el Experimento 4, integrando la paginación como palanca de optimización arquitectónica de dicho experimento.

## Implementación

1. **Frontend: Control de estado de búsqueda y debounce**
   - En `frontend/src/pages/MapPage.jsx` y `frontend/src/pages/OperatorPanelPage.jsx`:
     - Al dispararse un cambio en los filtros, marcar estado de carga (`isSearching = true` / limpiar `searchResults` obsoletos si cambian drásticamente los criterios).
     - Descartar callbacks de peticiones que ya no correspondan a la última intención del usuario.
2. **k6 Scripts: Fallo estricto en setup**
   - Importar `fail` de `'k6'` en:
     - `k6/experimento-3-emergencia.js`
     - `k6/baseline.js`
     - `k6/experimento-2-saturacion.js`
   - Reemplazar el `console.warn(...)` pasivo por `fail(`[SETUP] Error: Login fallido con código ${res.status}`)`.
3. **Documentación: Ajuste de cifras**
   - Modificar las tablas y párrafos de `docs/rendimiento/experimento-3-emergencia.md` explicitando: Total de peticiones HTTP: 1.022 (1 autenticación + 1.021 bajo carga: 446 lecturas y 575 escrituras).

## Decisiones

- **Diferir la paginación al Experimento 4:**
  - *Justificación:* La práctica de observabilidad exige evaluar el impacto del volumen de datos (1.000, 10.000, 100.000 registros) y comparar el comportamiento sin paginación vs. con paginación e índices en PostgreSQL. Implementarla globalmente antes de medir eliminaría la línea base de comparación.
- **k6 fail() en setup():**
  - *Justificación:* Evita generar ejecuciones inválidas con falsos positivos o errores 401 que desvirtúen los percentiles de latencia.

## Riesgos

- **Riesgo:** Parpadeo en UI si se limpian los resultados en cada pulsación del usuario.
  - *Mitigación:* Gestionar el indicador visual de carga manteniendo la transición suave o usando el debounce antes de limpiar la visualización.

## Estrategia de tests

- **Frontend:**
  - Ejecutar `npm test` en `frontend/` verificando `MapPage.test.jsx` y `OperatorPanelPage.test.jsx`.
  - Comprobar linting con `npm run lint`.
- **k6:**
  - Validación de sintaxis de los scripts modificados mediante ejecución con `--dry-run` o verificación estática.
