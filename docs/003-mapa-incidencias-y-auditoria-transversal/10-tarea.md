# Tarea 10: Módulo Utilitario de Geolocalización y Pruebas Unitarias (HU14.4)

**Feature:** `003-mapa-incidencias-y-auditoria-transversal`  
**Historia de Usuario:** HU14.4 — Captura opcional de ubicación actual con geolocalización en alta  
**Estado:** Completada  
**Requisitos funcionales relacionados:** RF-22, RF-26, RF-29, RF-30, RF-31, RF-33  

---

## 1. Resumen de la Tarea

En esta tarea se diseñó e implementó la capa utilitaria de geolocalización en el cliente para el frontend de UrbanPulse (`frontend/src/utils/geolocationUtils.js`), alineada con las directrices técnicas del [ADR-06](../../docs/ADR/ADR-06-geolocalizacion-nativa-navegador.md) y respaldada por una exhaustiva suite de pruebas unitarias (`frontend/src/utils/geolocationUtils.test.js`) en Vitest:

1. **Detección segura de soporte y contexto seguro (`isGeolocationSupported`):**
   - Comprobación de la presencia de `navigator.geolocation` y de `window.isSecureContext` (requerido por las especificaciones W3C para habilitar la geolocalización bajo HTTPS o en localhost).
   - Verificación segura frente a entornos de testing o ejecución en servidor donde `window` o `navigator` pudieran ser indefinidos.
2. **Promesa para la captura de coordenadas (`getCurrentCoordinates`):**
   - Envoltorio de la función asíncrona `navigator.geolocation.getCurrentPosition` en una Promesa nativa de JavaScript.
   - Opciones predeterminadas optimizadas para precisión física y rendimiento: `{ enableHighAccuracy: true, timeout: 10000, maximumAge: 0 }`.
   - Redondeo estricto de latitud y longitud a 6 cifras decimales (`Number(coord.toFixed(6))`), garantizando la precisión métrica y submétrica (~0.1 m) exigida para la fijación de desperfectos en la vía pública.
3. **Mapeo de errores nativos a mensajes claros en español (`getGeolocationErrorMessage` / `GEOLOCATION_ERROR_MESSAGES`):**
   - Mapeo unificado de los códigos estándar W3C (`GeolocationPositionError`):
     - `1` (`PERMISSION_DENIED`): Informa que el permiso fue denegado y sugiere mantener el punto seleccionado en el mapa interactivo (RF-29).
     - `2` (`POSITION_UNAVAILABLE`): Notifica la imposibilidad de fijar la posición y recomienda comprobar la cobertura GPS o de red (RF-31).
     - `3` (`TIMEOUT`): Advierte sobre la superación del tiempo límite de espera de 10 segundos (RF-30).
     - Incompatibilidad de navegador o contexto no seguro (RF-33).
     - Fallo general inesperado ante códigos no estandarizados o respuestas anómalas.
4. **Verificación y calidad:**
   - 25 pruebas unitarias con Vitest cubriendo todos los casos de éxito, opciones configurables, fallos controlados y casos límite.
   - 0 advertencias y 0 errores en ESLint (`npm run lint`).
   - Compilación de producción validada con Vite (`npm run build`).

---

## 2. Subtareas Implementadas

### 2.1 Módulo utilitario y opciones predeterminadas (Subtarea 10.1)
- **`frontend/src/utils/geolocationUtils.js`:**
  - Definición y congelación de `DEFAULT_GEOLOCATION_OPTIONS` con `{ enableHighAccuracy: true, timeout: 10000, maximumAge: 0 }`.
  - Implementación de `isGeolocationSupported()`, evaluando de forma segura `navigator?.geolocation` y `window?.isSecureContext`.
  - Implementación de `getCurrentCoordinates(options)`, permitiendo extender o sobreescribir las opciones predeterminadas y resolviendo un objeto `{ latitude, longitude, accuracy }`.

### 2.2 Formateo a 6 decimales y mapeo de errores en español (Subtarea 10.2)
- **`frontend/src/utils/geolocationUtils.js`:**
  - Creación de `roundCoordinate(value, decimals = 6)` para redondear numéricamente tanto valores numéricos como cadenas numéricas, preservando la coherencia en representaciones cartográficas.
  - Creación de `GEOLOCATION_ERROR_MESSAGES` y la función traductora `getGeolocationErrorMessage(error)`, que admite códigos numéricos directos o instancias de error nativas.
  - Documentación técnica exhaustiva con anotaciones JSDoc (`@fileoverview`, `@constant`, `@param`, `@returns`, `@throws`) en todas las funciones y constantes exportadas.

### 2.3 Suite de pruebas unitarias en Vitest (Subtarea 10.3)
- **`frontend/src/utils/geolocationUtils.test.js`:**
  - Pruebas del catálogo de constantes y exportaciones default.
  - Pruebas de detección de soporte: entornos compatibles, navegadores sin API, entornos con contexto no seguro (`window.isSecureContext === false`) y contextos indefinidos.
  - Pruebas de redondeo a 6 decimales, tratamiento de valores negativos (ej. longitud en Málaga `-4.421457`) y entradas inválidas (`NaN`).
  - Pruebas de traducción de errores para códigos 1, 2, 3 y códigos no esperados.
  - Pruebas de `getCurrentCoordinates` validando invocación a `navigator.geolocation.getCurrentPosition`, resolución con 6 decimales, opciones sobrescritas, descarte por falta de soporte y captura contextual de errores nativos con propagación de `originalError`.

---

## 3. Archivos Modificados y Creados

| Archivo | Tipo de Cambio | Descripción |
|---|---|---|
| `frontend/src/utils/geolocationUtils.js` | Creado | Módulo utilitario de geolocalización con JSDoc exhaustivo, detección segura de compatibilidad, redondeo a 6 decimales y mapeo de errores W3C a español. |
| `frontend/src/utils/geolocationUtils.test.js` | Creado | Suite de 25 pruebas unitarias en Vitest con cobertura integral de detección, opciones, redondeo y códigos de error. |
| `features/003-mapa-incidencias-y-auditoria-transversal/tasks.md` | Modificado | Actualización de Tarea 10 y subtareas 10.1, 10.2 y 10.3 a completadas `[x]`. |
| `docs/003-mapa-incidencias-y-auditoria-transversal/10-tarea.md` | Creado | Documentación técnica integral de la Tarea 10. |

---

## 4. Validación y Resultados de Pruebas

### 4.1 Pruebas Unitarias de Vitest (`geolocationUtils.test.js`)

```bash
npm --prefix frontend test -- src/utils/geolocationUtils.test.js
```

**Resultado:**
```
> frontend@0.0.0 test
> vitest run src/utils/geolocationUtils.test.js

 RUN  v5.0.5 C:/Users/Yesu/Proyecto-Urbanpulse/frontend

 ✓ src/utils/geolocationUtils.test.js (25 tests) 12ms

 Test Files  1 passed (1)
      Tests  25 passed (25)
   Start at  19:47:43
   Duration  1.21s
```

### 4.2 Linter de Frontend (`npm run lint`)

```bash
npm --prefix frontend run lint
```

**Resultado:**
```
> frontend@0.0.0 lint
> eslint .
Exit Code: 0 (0 errores, 0 advertencias)
```

### 4.3 Compilación de Producción (`npm run build`)

```bash
npm --prefix frontend run build
```

**Resultado:**
```
> frontend@0.0.0 build
> vite build

vite v8.3.3 building client environment for production...
transforming...
✓ 150 modules transformed.
rendering chunks...
computing gzip size...
dist/index.html                   0.47 kB │ gzip:   0.30 kB
dist/assets/index-C1qiAo4M.css   54.99 kB │ gzip:  13.54 kB
dist/assets/index-BE5uleTX.js   520.39 kB │ gzip: 164.17 kB
✓ built in 348ms
```

---

## 5. Próximos Pasos

Continuar con la **Tarea 11: Diseño visual y estilos CSS del control de geolocalización (HU14.4)**:
- Añadir estilos en `frontend/src/components/Map/Map.css` para el botón `.btn-current-location` con icono SVG de geoposicionamiento, estados `:hover`, `:focus-visible`, `:disabled` y animación `.is-locating`.
- Diseñar clases para el distintivo de confirmación `.geo-status-badge` en verde esmeralda y la alerta de error en línea `.geo-error-banner` con accesibilidad WCAG 2.1 AA.
