# 012 — Rendimiento del mapa de incidencias — Plan

_¿Cómo se implementa lo descrito en `spec.md`? Respeta estrictamente la `constitution/` y las decisiones registradas en `docs/ADR/`._

## Enfoque

La optimización se aborda en dos frentes independientes y ordenados por impacto:

1. **Frontend puro (sin contrato):** eliminar el coste O(n) por renderizado mediante caché de iconos, memoización, popup ligero, carga diferida por ruta y pausa del sondeo de notificaciones. No requiere ADR porque son optimizaciones internas que no cambian la arquitectura ni el contrato de lectura.
2. **Agrupación de marcadores:** reducir el número de nodos del DOM con `react-leaflet-cluster`, decisión formalizada en el **ADR-11**, que referencia y mantiene vigente el **ADR-04** (Leaflet + OpenStreetMap).

El orden recomendado es aplicar primero el frente 1 (ganancia inmediata y medible) y después el frente 2, para poder atribuir cada mejora a su causa. La reducción de payload no forma parte de esta spec: se resuelve en la Spec 013 y en el ADR-12.

## Implementación

### 1. Caché de iconos de marcador

- `frontend/src/utils/leafletIcons.js`:
  - Sustituir la creación de un `L.divIcon` por llamada por una caché a nivel de módulo (`Map`) indexada por estado y por la variante de selección.
  - Mantener una función de creación pura interna para poder invalidar o reconstruir la caché en pruebas.
  - Conservar `setupLeafletIcons()` sin efectos ocultos adicionales; documentar con JSDoc que la instancia devuelta es compartida y no debe mutarse.
- `frontend/src/components/Map/mapConstants.js`:
  - `getIncidentMarkerIcon(status)` y la variante seleccionada deben devolver siempre la misma referencia para la misma clave.
  - No construir plantillas SVG ni objetos `L.divIcon` dentro del `.map()` de marcadores.

Criterio de verificación: dos llamadas consecutivas con el mismo estado devuelven la misma instancia (`===`).

### 2. Memoización del filtrado y del componente de mapa

- `frontend/src/components/Map/IncidentsMap.jsx`:
  - Calcular `validIncidents` dentro de `useMemo`, con dependencia exclusiva de `incidents`.
  - Extraer el filtro de coordenadas válidas a una función pura testeable.
- `frontend/src/pages/MapPage.jsx`:
  - Envolver los handlers que recibe el mapa (`onSelectIncident`, `onLocationSelected`, `onFocusConsumed`, etc.) en `useCallback` con dependencias estables.
  - Evitar crear objetos o arrays nuevos en cada render para las props del mapa.
- `frontend/src/components/Map/IncidentsMap.jsx`:
  - Exportar el componente envuelto en `React.memo` con comparación por props relevantes.
  - Verificar con React DevTools Profiler que un cambio de toast, foco o notificación no vuelve a renderizar el mapa.

### 3. Popup ligero con delegación del detalle

- `frontend/src/components/Map/IncidentsMap.jsx`:
  - Reducir el contenido de `<Popup>` a estado, título, fecha de creación y acción «Ver detalle».
  - Eliminar la descripción completa y el resto del contenido pesado del popup.
  - Al pulsar «Ver detalle», cerrar el popup y abrir `IncidentDetailModal` con la incidencia seleccionada, reutilizando el componente existente.
- `frontend/src/components/Map/Map.css`:
  - Ajustar únicamente los estilos del popup ligero; no se divide el fichero en esta entrega para no mezclar refactor visual con rendimiento.

### 4. Carga diferida por ruta

- `frontend/src/App.jsx`:
  - Convertir las importaciones de páginas a `React.lazy` con `Suspense`, manteniendo intactos `ProtectedRoute` y `RoleGuard`.
  - Prioridad de división: `AnalyticsPage` (arrastra `jspdf`/`jspdf-autotable`), `OperatorPanelPage`, `MapPage`, `MyIncidentsPage`, `HomePage`, `LoginPage` y `RegisterPage`.
  - Añadir un fallback de carga accesible (texto y `role="status"`), sin bloquear la navegación.
- Verificar en `dist/assets` que el generador de PDF no se descarga en la ruta del mapa.

### 5. Pausa del sondeo de notificaciones

- `frontend/src/components/Notifications/NotificationBell.jsx`:
  - Comprobar `document.hidden` antes de cada consulta periódica.
  - Escuchar `visibilitychange`: al ocultarse la pestaña se cancela el intervalo; al volverse visible se reanuda y se refresca una vez.
  - Limpiar el listener y el intervalo en el desmontaje.
  - Envolver el componente en `React.memo` y estabilizar el handler que recibe del navbar.

### 6. Agrupación de marcadores (ADR-11)

- Añadir `react-leaflet-cluster@^4.1.3` (arrastra `leaflet.markercluster@^1.5.3`) en `frontend/package.json`. La instalación se ejecuta en la fase de implementación, no en la Fase 0.
- `frontend/src/components/Map/IncidentsMap.jsx`:
  - Envolver los marcadores en `MarkerClusterGroup` cuando el número de incidencias representables supere 500.
  - Activar `chunkedLoading`, agrupación por proximidad y desagregación al nivel de detalle individual.
  - Mantener el marcador individual para agrupaciones de una sola incidencia.
- `frontend/src/components/Map/mapConstants.js` y `Map.css`:
  - Iconos de agrupación con contador y severidad predominante, sin depender solo del color.
- Calibrar `maxClusterRadius` y `disableClusteringAtZoom` con la densidad urbana de Málaga y registrarlo en la línea base.

## Decisiones

- **Caché de iconos a nivel de módulo** — Los iconos son inmutables y dependen solo del estado. Se descarta cachear por componente porque multiplicaría instancias y no resolvería el re-render.
- **`React.memo` + `useCallback` antes de extraer estado a un contexto** — Es la intervención de menor riesgo y suficiente para aislar el mapa; un reducer/contexto global se pospone hasta que la medición demuestre que sigue habiendo acoplamiento.
- **Popup ligero delegando en `IncidentDetailModal`** — Evita duplicar la ficha y reduce el DOM por marcador; no se crea un componente de detalle nuevo.
- **`React.lazy` por ruta, no por componente** — Mantiene los límites de carga alineados con la navegación y evita suspenses anidados innecesarios.
- **Pausa del sondeo con `visibilitychange`** — Se prefiere a comprobar solo `document.hidden` en cada tick porque permite cancelar el intervalo y no gastar temporizadores mientras la pestaña está oculta.
- **`react-leaflet-cluster` como primera opción** — Decisión registrada en el **ADR-11**; `supercluster` queda documentado como plan B si no se alcanzan los umbrales.

## Línea base (Antes / Objetivo)

Medición local realizada el 2026-11-26 con `npm run build`, `npm test` y `npm run test:coverage` en `frontend/`. Los artefactos (`dist/`, `coverage/`) están ignorados por Git. El detalle del protocolo y las métricas de navegador están en `docs/rendimiento/experimento-4-baseline-frontend.md`.

| Métrica | Antes (medido) | Objetivo | Fuente |
| :--- | ---: | ---: | :--- |
| JS de entrada principal | 1.063,46 kB (329,77 kB gzip) | ≤ 600 kB y sin `jspdf` en el chunk inicial | `npm run build` |
| CSS de entrada | 125,15 kB (23,81 kB gzip) | Sin aumento > 5 % | `npm run build` |
| `jspdf` en chunk inicial | Sí (122 referencias en `index-*.js`) | No (chunk bajo demanda) | `npm run build` |
| Chunks dinámicos detectados | `html2canvas` 199,48 kB, `index.es` 151,39 kB, `purify.es` 29,07 kB | Mantenerlos fuera de la ruta del mapa | `npm run build` |
| Tests frontend | 37 archivos / 303 tests en verde | 100 % en verde y nuevos tests para las ramas añadidas | `npm test` |
| Cobertura de sentencias | 81,68 % | Sin regresión > 1 p.p. | `npm run test:coverage` |
| Cobertura de ramas | 71,91 % | Sin regresión > 1 p.p. | `npm run test:coverage` |
| Cobertura de funciones | 76,70 % | Sin regresión > 1 p.p. | `npm run test:coverage` |
| Cobertura de líneas | 82,86 % | Sin regresión > 1 p.p. | `npm run test:coverage` |
| Cobertura de `IncidentsMap.jsx` | 0 % (sin test directo) | ≥ 70 % | `npm run test:coverage` |
| Nodos del DOM en `/map` con 5.000 incidencias | Pendiente de medición en navegador | ≤ 1.000 con el mapa alejado | DevTools |
| TTI / LCP de `/map` | Pendiente de medición en navegador | TTI ≤ 3 s en equipo de referencia | DevTools |
| FPS en pan/zoom | Pendiente de medición en navegador | 60 fps sin tareas > 100 ms | DevTools |
| Peticiones de notificaciones con pestaña oculta (10 min) | Pendiente de medición en navegador | 0 peticiones | DevTools → Network |
| Payload que recibe `/map` con 4.500 activas | 4.771.646 bytes (~4,55 MiB) y 9.851 ms | ≤ 1 MB y p95 ≤ 1.000 ms (Spec 013) | `curl.exe -w` |

**Nota sobre el dataset:** la base local contiene 5.000 incidencias totales y 4.500 activas (el seed reserva un 10 % a `CLOSED`/`REJECTED`). Cualquier cifra de navegador debe declarar si se midió con 4.500 activas o si se amplió el conjunto a 5.000; no se debe etiquetar como «5.000 activas» una medición con 4.500.

## Riesgos

- **Riesgo: incompatibilidad de `react-leaflet-cluster` con React 19 o `react-leaflet` 5.** Mitigación: los peers declarados coinciden; si una actualización rompe, se aplica el plan B documentado en el ADR-11 (`supercluster`).
- **Riesgo: la caché de iconos devuelve una instancia mutada por Leaflet.** Mitigación: no mutar los iconos después de crearlos y cubrir la identidad con un test unitario.
- **Riesgo: `React.memo` no evita el re-render por props nuevas.** Mitigación: memoizar handlers y objetos en `MapPage`; verificar con el Profiler, no solo por inspección.
- **Riesgo: la carga diferida rompe pruebas que importan páginas de forma síncrona.** Mitigación: adaptar los tests a `Suspense` y mantener los mocks actuales de `../components/Map`.
- **Riesgo: el popup ligero elimina información que el usuario esperaba ver.** Mitigación: la ficha completa sigue disponible en un clic mediante `IncidentDetailModal`; se conserva el estado y la fecha en el popup.
- **Riesgo: el sondeo se detiene y no se reanuda.** Mitigación: probar `visibilitychange` con temporizadores falsos y verificar que al volver a visible se refresca una vez.

## Estrategia de tests

- **Unitarios de utilidades:** identidad de iconos por estado y por variante de selección; filtro de coordenadas válidas.
- **Componentes:** popup ligero (solo estado, título, fecha y acción), apertura de `IncidentDetailModal`, `NotificationBell` con `document.hidden` y `visibilitychange` usando `vi.useFakeTimers()`.
- **Integración de rutas:** `App` con `React.lazy` y `Suspense`; verificar que el fallback es accesible y que las rutas protegidas siguen funcionando.
- **Agrupación:** prueba de `IncidentsMap` con un conjunto pequeño y el entorno de Leaflet simulado; verificar que se renderiza el grupo y que se desagrega al acercarse.
- **Rendimiento:** repetir el protocolo de `docs/rendimiento/experimento-4-baseline-frontend.md` antes y después, y registrar la comparación en este plan.
- **Cobertura:** mantener la línea base de 81,68 % de sentencias y elevar `IncidentsMap.jsx` por encima del 70 %.

## Referencias

- `features/012-mapa-rendimiento/spec.md`
- `docs/ADR/ADR-04-cartografia-interactiva-leaflet-openstreetmap.md`
- `docs/ADR/ADR-11-agrupacion-de-marcadores-en-el-mapa.md`
- `docs/rendimiento/experimento-4-baseline-frontend.md`
- `docs/issue-refactor-frontend.md` (hallazgos C1, C2, C5 y C6)
