# Auditoría Técnica del Frontend — Base para Issue #60

> **Alcance:** Análisis estático de solo lectura sobre `frontend/` + verificación del contrato real en `Backend/`. No se ha creado, modificado ni eliminado ningún fichero. No se ejecutaron comandos Git ni de instalación.  
> **Guías consultadas:** `AGENTS.md`, `constitution/tech-stack.md`, `docs/ADR/ADR-04-cartografia-interactiva-leaflet-openstreetmap.md`, skills `modern-web-guidance` (*defer-rendering-heavy-content*, *dialog*, *accessibility*) y `frontend-design`. Ninguna guía ha generado cambios de código en este turno; se citan como criterio de las recomendaciones.

---

## 1. Resumen Ejecutivo

El frontend funciona bien con decenas de incidencias, pero escala linealmente en tres dimensiones a la vez (**payload**, **DOM** y **re-render**), y las tres convergen en la misma vista: `/map`.

| ID | Hallazgo | Evidencia | Impacto con ~5.000 incidencias |
| :--- | :--- | :--- | :--- |
| **C1** | Se crea un `L.divIcon` nuevo por marcador y por render | `frontend/src/utils/leafletIcons.js:55-73` invocado desde `frontend/src/components/Map/mapConstants.js:141-153`, llamado dentro del `.map()` en `frontend/src/components/Map/IncidentsMap.jsx:185` | `react-leaflet` compara por identidad (`node_modules/react-leaflet/lib/Marker.js: props.icon !== prevProps.icon → marker.setIcon()`), así que cada re-render destruye y recrea los 5.000 nodos de marcador. **Es el coste dominante, con diferencia.** |
| **C2** | Un `<Marker>` + `<Popup>` completo por incidencia, sin clustering | `IncidentsMap.jsx:182-220`, `preferCanvas={false}` en `:161` | ~5.000 marcadores DOM + 5.000 popups montados (con título, descripción y botón) → del orden de 40k–60k nodos. Pan/zoom con *jank* severo. |
| **C3** | El mapa carga todas las incidencias desde un endpoint sin paginar | `frontend/src/pages/MapPage.jsx:126` → `frontend/src/services/incidentService.js` → `Backend/.../controller/v2/IncidentControllerV2.java:66-69` (`List<IncidentResponseV2>`) y `Backend/.../service/IncidentService.java:755-761` (`findByStatusNotIn` sin límite) | Payload estimado 3–8 MB por visita al mapa (el DTO `IncidentResponseV2` incluye `description`, `citizenEmail`, `urbanAssets[]`, `urbanContext[]`, `activeAssignments[]`). Además expone `citizenEmail` a la vista ciudadana. |
| **C4** | El panel de operador renderiza una `<tr>` por incidencia, sin paginar ni virtualizar | `frontend/src/pages/OperatorPanelPage.jsx:686`, array completo desde `:108`, sin `slice`/`currentPage` (verificado: 0 coincidencias) | ~5.000 filas × 8 celdas con botones y modales. Cada cambio de estado hace `prev.map` sobre el array completo (`:237-423`). |
| **C5** | Cero `React.memo` en todo el proyecto | `grep "React.memo\|memo("` → `0` resultados en `frontend/src/**/*.jsx` | Cualquier cambio de estado en `MapPage` (toast, hover, foco) o el polling de notificaciones re-renderiza el árbol completo. |
| **C6** | Todas las páginas se importan *eager*, incluido `jspdf` | `frontend/src/App.jsx:10-16`; `jspdf` + `jspdf-autotable` en `frontend/src/utils/reportUtils.js:8-9` | El mapa (vista crítica) paga el coste de parseo del generador de PDF en el bundle inicial. |
| **C7** | Accesibilidad: idioma, foco y semántica | `frontend/index.html:2` (`lang="en"`), `frontend/src/pages/Auth.css:72-73`, `frontend/src/pages/MyIncidentsPage.css:160-161`, `frontend/src/pages/OperatorPanelPage.css:309-310` (`outline: none` sin sustituto), `frontend/src/components/Filters/IncidentFilterBar.jsx:89` (`aria-label` en `div` sin rol) | Fallos WCAG 2.1 A/AA: **3.1.1** (idioma), **2.4.7** (foco visible), **4.1.2**. |
| **C8** | Menú sin adaptación móvil | `frontend/src/components/Navbar.jsx:60-117` + `frontend/src/components/Navbar.css` (98 líneas, 0 media queries) | A 360–480 px, 5 enlaces + campana + *"Cerrar sesión"* desbordan sin hamburguesa ni colapso. |

> **Conclusión:** El orden de ataque correcto no es *"meter clustering primero"*. Es **`C1` → `C5` → `C2`** (frontend puro, sin tocar API, ganancia inmediata y medible), y en paralelo abrir el frente **`C3`/`C4`**, que exige contrato backend nuevo y, por las reglas del proyecto (`AGENTS.md`, flujo SDD, `ADR-04`), spec aprobada + ADR antes de implementar.

---

## 2. Análisis Detallado por Área

### Área 1 — Leaflet y Clustering

#### 1.1 El coste real no es *"5.000 marcadores"*, es *"5.000 iconos nuevos cada render"*
`frontend/src/components/Map/IncidentsMap.jsx:182-191`:

```jsx
{validIncidents.map((incident) => {
  const markerIcon = getIncidentMarkerIcon(incident.status);   // ← :185
  return <Marker icon="{markerIcon}" key="{incident.id}" lng]} position="{[lat,">
```

`getIncidentMarkerIcon` (`mapConstants.js:141-153`) delega en `createColoredMarkerIcon` (`leafletIcons.js:55-73`), que construye un template SVG string y un `L.divIcon` nuevo en cada llamada. Verificado en `node_modules/react-leaflet/lib/Marker.js`:

```javascript
if (props.icon != null && props.icon !== prevProps.icon) {
    marker.setIcon(props.icon);
}
```

Comparación por referencia ⇒ siempre distinto ⇒ `setIcon()` en los 5.000 marcadores en cada render. `setIcon` en Leaflet destruye el `<img>`/`<div>` del marcador y crea otro, más `_initIcon` y recálculo de posición. Esto explica por qué la vista se degrada también sin mover el mapa: basta un toast, un hover de popup o el ciclo de la campana de notificaciones (`NotificationBell.jsx:63`, `setInterval` dentro del `Navbar`, montado en todas las páginas).

* **Corrección de bajo riesgo y altísimo impacto:** Cachear los iconos por estado en un `Map` módulo-level (son 5 estados + 1 selección). Es un cambio de ~10 líneas en `leafletIcons.js`/`mapConstants.js`, sin dependencias nuevas, sin ADR (no cambia arquitectura, es una optimización interna), y con test unitario trivial:
  ```javascript
  expect(getIncidentMarkerIcon('RESOLVED')).toBe(getIncidentMarkerIcon('RESOLVED'));
  ```

#### 1.2 Filtrado en render
`IncidentsMap.jsx:124-131` recalcula `validIncidents` con `incidents.filter(...)` en el cuerpo del componente, sin `useMemo`. Con 5.000 elementos y renders frecuentes es trabajo $O(n)$ repetido; se elimina con `useMemo([incidents])`.

#### 1.3 Popups montados siempre
Cada `<Popup>` (`:192-219`) contiene un `.incident-popup-card` completo con `<h3>`, descripción íntegra y `<button>`. Leaflet monta el DOM del popup al abrirlo, pero React mantiene el árbol de la capa. Con 5.000 popups, el contenido de descripción (texto libre, potencialmente largo) infla memoria.
* **Recomendación:** Popup ligero (*título + estado + fecha*) y *"Ver detalle"* abre `IncidentDetailModal` (`IncidentDetailModal.jsx`), que ya existe y ya se usa desde `:212`. Esto reduce el DOM por marcador drásticamente y, de paso, unifica la UX.

#### 1.4 Compatibilidad de librerías de clustering (verificada contra el registro npm)

| Paquete | Versión | Peers declarados | Veredicto con el stack actual |
| :--- | :--- | :--- | :--- |
| `react-leaflet-cluster` | `4.1.3` | `react ^19.0.0`, `react-dom ^19.0.0`, `react-leaflet ^5.0.0`, `@react-leaflet/core ^3.0.0`, `leaflet ^1.9.0`; depende de `leaflet.markercluster ^1.5.3` | ✅ Encaje exacto con `react-leaflet` 5.0 + React 19.2 (`frontend/package.json`) |
| `leaflet.markercluster` | `1.5.3` | `leaflet ^1.3.1` | ✅ Compatible, pero es API imperativa: obliga a gestionar el grupo a mano contra el ciclo de vida declarativo de `react-leaflet` |
| `supercluster` | `9.1.0` | — (sin peers React) | ✅ Alternativa *"sin DOM de cluster"*: se calculan clusters y se pintan con `CircleMarker`/`divIcon` propios. Más código, control total y mejor techo de rendimiento |
| `@tanstack/react-virtual` | `3.14.26` | `react ^16–^19` | ✅ Para el eje 2 (tabla del panel) |

* **Recomendación:** `react-leaflet-cluster` como primera opción (integración declarativa, peers exactos, cero fricción con `ADR-04`). `supercluster` queda como plan B si se quiere evitar el DOM de `leaflet.markercluster`. Ambas son decisión de arquitectura ⇒ requieren ADR nuevo que referencie/superseda parcialmente `ADR-04`, y `spec.md` aprobada antes de tocar código (regla explícita de `AGENTS.md`).

#### 1.5 Lo que el clustering NO arregla
Clustering reduce DOM, no payload. Con `C3` sin resolver, el navegador sigue recibiendo y parseando varios MB de JSON en la primera pintura. `C1+C2` mejoran el frame rate; `C3` mejora el TTI. Son frentes independientes y hay que planificarlos por separado.

---

### Área 2 — Paginación y Carga Eficiente de Listas

#### 2.1 El backend no ofrece paginación donde se necesita

| Endpoint | Firma real | Paginación |
| :--- | :--- | :--- |
| `GET /api/v2/incidents` | `IncidentControllerV2.java:66-69` → `List<IncidentResponseV2>` | ❌ Ninguna |
| `GET /api/v2/incidents/search` | `IncidentControllerV2.java:197-220` → `List` plano | ❌ Ninguna, sin `limit` |
| `GET /api/v2/incidents/my-incidents` | `IncidentControllerV2.java:71-82` → `PageResponse` con `page`/`size`/`status` | ✅ Ya paginado |
| `GET /api/incidents/my-incidents` (legacy) | `IncidentController` fuerza `size` 1–50 | ✅ Ya paginado |

`IncidentService.java:755-761` (`getActiveIncidentsV2`) hace `findByStatusNotIn(CLOSED, REJECTED)` sin `Pageable` y `toDtoList`. `IncidentMapperV2` recibe `TareasRepository`, lo que sugiere resolución de assignments por incidencia dentro del mapeo — con 5.000 filas esto es un candidato claro a **N+1**. A verificar con `spring.jpa.show-sql` o un test de integración antes de dimensionar, pero el patrón lo justifica.

#### 2.2 El frontend ya tiene el patrón correcto… en la página equivocada
`frontend/src/pages/MyIncidentsPage.jsx` implementa paginación servidor con `PAGE_SIZE = 10` e indicador `aria-live` (`:428`). Es el modelo a replicar. En cambio:
* `MapPage.jsx:126` y `OperatorPanelPage.jsx:108` llaman a `getAllIncidents()` sin parámetros.
* `searchIncidents` en ambas páginas (`MapPage.jsx:183`, `OperatorPanelPage.jsx:181`) tiene debounce de 300 ms correcto, pero sin `limit` ni paginación: una búsqueda por categoría puede devolver el catálogo entero.
* `OperatorPanelPage.jsx:686` pinta `displayedIncidents.map(...)` completo. `displayedIncidents` (`:441-495`) filtra en cliente sobre 5.000 registros, incluyendo `new Date(...)` por incidencia y por render dentro del filtro de fechas (`:483`, `:488`) — parseo de 5.000 fechas por pasada.

#### 2.3 Contrato propuesto (a validar en spec)
Para no romper `MapPage` ni los tests existentes, la vía de menor riesgo es aditiva:
1. **Endpoint ligero para el mapa:** `GET /api/v2/incidents/map` devolviendo un DTO reducido (`id`, `latitude`, `longitude`, `status`, `priority`, `category`, `createdAt`, `title` corto) — sin `description`, sin `citizenEmail`, sin `urbanAssets`, sin `urbanContext`. Reduce payload de ~1 KB a ~150 B por incidencia (≈ 750 KB para 5.000) y elimina la fuga de `citizenEmail`. Es también la respuesta natural a *"¿qué necesita el mapa realmente?"*.
2. **Paginación en `/incidents` y `/search`:** Con `page`/`size` opcionales y retrocompatibles (sin params ⇒ comportamiento actual), para no romper consumidores ni tests mientras se migra.
3. **Panel de operador:** Paginar en servidor reutilizando `MyIncidentsPage` como referencia; el filtrado multicriterio debe pasar a query params (ya existe `toSearchParams`, `OperatorPanelPage.jsx:35`).

---

### Área 3 — Re-render y Memoria en React

#### 3.1 Estado concentrado en un componente
`MapPage.jsx:59-101` declara 11 `useState` en el mismo componente: datos, loading, refresh, error, filtro fecha, filtro múltiple, resultados de búsqueda, departamentos, modo selección, coordenadas seleccionadas, modal de alta, foco, incidencia seleccionada y toast. Cada uno de esos `setState` re-renderiza `IncidentsMap` y, por `C1`, reconstruye los 5.000 iconos.

**Mitigaciones (de menor a mayor esfuerzo):**
1. Cache de iconos (Área 1.1) — elimina el coste $O(n)$ aunque el re-render ocurra. Hacer primero.
2. `React.memo` en `IncidentsMap` + `useCallback` en los handlers que recibe (`onSelectIncident`, `onLocationSelected`, `onFocusConsumed`). Sin memoizar callbacks, `memo` no sirve.
3. Extraer el estado efímero (toast, foco, selección) a un componente contenedor o a un reducer con contexto, para que no viva en el mismo nivel que `incidents`.

#### 3.2 Polling de notificaciones dentro del navbar global
`frontend/src/components/Notifications/NotificationBell.jsx:63` crea un `setInterval` y se monta en `Navbar` (`Navbar.jsx:107`), que está en todas las páginas. Cada tick actualiza `notifications`/`unreadCount` → re-render del navbar. En `/map`, eso es un re-render periódico del árbol completo. Con `C1` resuelto el impacto cae al mínimo; aun así, conviene:
* Pausar el polling con `document.visibilityState` (no gastar cuota con la pestaña oculta).
* Envolver `NotificationBell` en `React.memo` y estabilizar el handler.
* O mover la campana fuera del árbol del mapa si el diseño lo permite.

#### 3.3 Code splitting ausente
`App.jsx:10-16` importa `LoginPage`, `RegisterPage`, `HomePage`, `OperatorPanelPage`, `MapPage`, `MyIncidentsPage`, `AnalyticsPage` *eager*. `AnalyticsPage` arrastra `analyticsService` y, en cuanto se pulsa exportar, `reportUtils` → `jspdf` + `jspdf-autotable` (`reportUtils.js:8-9`). `React.lazy` + `Suspense` por ruta (con `ProtectedRoute` intacto) saca ese peso del bundle inicial. Impacto directo en LCP del mapa, que es la pantalla crítica.

#### 3.4 Micro-antipatrones detectados
* `transition: all` en 6 puntos de `OperatorPanelPage.css` (`:64`, `:81`, `:370`, `:392`, `:422`, `:545`) y varios en `MyIncidentsPage.css` — fuerza al motor a observar todas las propiedades; usar propiedades explícitas.
* `getActiveAssignments(incident)` (`OperatorPanelPage.jsx:435-438`) se llama dentro del `.map()` de filas y también dentro del `useMemo` de filtrado (`:467-471`): dos recorridos distintos con la misma lógica duplicada.
* `IncidentFilterBar.jsx:60-66` recrea el objeto `filters` completo en cada tecla y notifica al padre; el padre reprograma el debounce (correcto), pero no hay `useCallback` en `onFilterChange` en ninguna de las dos páginas.
* `leafletIcons.js:35-41` ejecuta `setupLeafletIcons()` como efecto colateral del import: acopla el módulo a Leaflet global. Aceptable, pero conviene hacerlo explícito en `main.jsx` para claridad y para testear sin efectos ocultos.

#### 3.5 Impacto en tests (Vitest)
* `frontend/src/pages/MapPage.test.jsx:23-32` mockea `../components/Map` completo ⇒ añadir clustering no rompe esos tests.
* `frontend/src/pages/OperatorPanelPage.test.jsx:42-43, 171-172` mockea `getAllIncidents`/`searchIncidents` devolviendo arrays ⇒ cambiar a respuestas paginadas (`{content, totalElements}`) romperá estos tests y exige un adaptador en `incidentService` que normalice ambas formas (recomendado: que el servicio devuelva siempre la misma forma al consumidor).
* `frontend/src/services/incidentService.test.js:32-92` valida la forma de llamada de `searchIncidents`/`getMyIncidents`: cualquier cambio de query params debe actualizar estas aserciones.

---

### Área 4 — Auditoría Visual, CSS, Menú y Accesibilidad

#### 4.1 Global / plantilla Vite sin limpiar

| Hallazgo | Ubicación | Efecto |
| :--- | :--- | :--- |
| `lang="en"` con UI en español | `frontend/index.html:2` | WCAG 3.1.1 (A) — lectores de pantalla pronuncian en inglés |
| `<title>frontend</title>` | `frontend/index.html:8` | Sin marca; mal en pestañas, historial y SEO |
| `#root { text-align:center; max-width:1540px; border-inline }` | `frontend/src/index.css:57-67` | Herencia de plantilla; obliga a cada página a re-alinear y rompe tablas/listas a la izquierda |
| `#social .button-icon`, `code`, `.counter` | `frontend/src/index.css:48-50, :98-104` | Restos del template; `#social` no existe en la app |
| `App.css` (158 líneas) | `frontend/src/App.css` | No se importa en ningún sitio (verificado) — código muerto |
| `--accent: #aa3bff` (violeta) | `frontend/src/index.css:7` | Choca con la paleta real de la app (`#0284c7`, `#0369a1`, `#0f766e`…) y se usa en el marcador de selección (`IncidentsMap.jsx:232`) |

#### 4.2 Sistema de color inexistente (hardcodes)
Conteo de hex por fichero (medido): `Map.css` 140, `AnalyticsPage.css` 126, `IncidentCommentsSection.css` 63, `AssignmentModal.css` 58, `AnalyticsKpiCards.css` 56, `AnalyticsDateFilter.css` 49, `IncidentAssetCard.css` 39, `PriorityAssistantModal.css` 38, `StatusTimeline.css` 35, `MyIncidentsPage.css` (~500 líneas), `OperatorPanelPage.css` (595 líneas). Solo 4 ficheros tienen `prefers-color-scheme`: `index.css`, `AnalyticsPage.css:458`, `HomePage.css:121`, `MapPage.css:407` (+ `Map.css:770` y `:1168`, duplicado).

* **Consecuencia práctica:** `Navbar.css`, `MyIncidentsPage.css`, `OperatorPanelPage.css`, `IncidentFilterBar.css`, `IncidentCommentsSection.css` y `NotificationBell.css` no tienen modo oscuro. Con `color-scheme: light dark` en `:root` (`index.css:20`), el sistema operativo en oscuro hace que los controles nativos (inputs, selects, scrollbars) se pinten oscuros sobre fondos `#ffffff` hardcodeados → combinaciones ilegibles. Es un fallo real, no estético.
* **Propuesta:** Una capa de tokens semánticos en `index.css` (`--surface`, `--surface-muted`, `--surface-raised`, `--text-primary`, `--text-secondary`, `--border-subtle`, `--focus-ring`, `--accent-*` para los 5 estados) y una pasada por fichero sustituyendo hex por `var(...)`. Es mecánico, revisable y desbloquea el modo oscuro completo con un único bloque.

#### 4.3 Foco visible (WCAG 2.4.7)
Solo `AnalyticsPage.css:101-103, 200-202, 447-449` implementa `:focus-visible`. En el resto:

```css
/* frontend/src/pages/Auth.css:72-73 */
.form-input:focus { outline: none; }

/* frontend/src/pages/MyIncidentsPage.css:160-161 */
.status-filter-select:focus { outline: none; }

/* frontend/src/pages/OperatorPanelPage.css:309-310 */
.status-select:focus { outline: none; }
```

Sin sustituto ⇒ el usuario de teclado pierde el rastro del foco. La corrección correcta es una regla global `:focus-visible { outline: 2px solid var(--focus-ring); outline-offset: 2px; }` y eliminar los `outline: none`. Nunca usar `:focus { outline: none }` sin acción alternativa.

#### 4.4 Menú y navegación
`Navbar.jsx:60-117`: 5 enlaces (*Inicio*, *Mapa*, *Mis Incidencias*, *Panel de Operador*, *Analítica*) + `NotificationBell` + botón *"Cerrar sesión"*, todo en un `div.navbar-links` sin colapso. `Navbar.css` (98 líneas, 21 hex, 6 var) no tiene ninguna media query. No hay botón hamburguesa, ni `role="navigation"` explícito (sí `aria-label="Navegación principal"` en `:47`, correcto), ni `aria-current="page"` en el enlace activo (`:63`, `:70`, `:77`, `:87`, `:99` usan solo la clase `active`). En móvil el layout desborda; además el estado activo es solo visual (color), lo que falla también **1.4.1** (color como único medio).
* **Correcciones:** `aria-current="page"` en el link activo, menú colapsable con `<details>`/botón + `aria-expanded`, y breakpoint único (~768 px) compartido con el resto de páginas.

#### 4.5 Modales: `div` + `role="dialog"` en lugar de `<dialog>`
Todos los overlays son manuales:
* `frontend/src/components/Triage/AssignmentModal.jsx:347-348`
* `frontend/src/components/Triage/ReasonModal.jsx:178-179`
* `frontend/src/components/Triage/ResolveIncidentModal.jsx:144-145`
* `frontend/src/components/Triage/PriorityAssistantModal.jsx:187-188`
* `frontend/src/components/Map/IncidentDetailModal.jsx:205-206`
* `frontend/src/components/Map/CreateIncidentModal.jsx:521` (+ focus trap manual con `window.addEventListener('keydown')` en `:249`)
* `frontend/src/pages/OperatorPanelPage.jsx:993-994`

Cada uno reimplementa a mano foco atrapado, `Esc` y bloqueo de fondo. El estándar actual es `<dialog>` nativo con `showModal()`: aporta semántica, *top-layer*, `Esc`, foco atrapado e `inert` del fondo sin código propio, y elimina la necesidad de `role="dialog"`/`aria-modal` (duplicarlos es contraproducente). Migración incremental modal a modal, empezando por `IncidentDetailModal` y `CreateIncidentModal` (los más usados desde el mapa).

#### 4.6 Semántica de filtros
`IncidentFilterBar.jsx:89` pone `aria-label="Filtros de incidencias"` en un `div` sin `role` ⇒ no se anuncia. Debería ser `<fieldset>` con `<legend>` o `role="group"`/`role="search"`. Además `:146`, `:184`, `:204` tienen `aria-label` redundante con el `<label htmlFor>` correspondiente (doble anuncio). `:228-230` sí usa `role="status"` para *"sin resultados"* — correcto.

#### 4.7 Rendimiento visual y responsive
* `MapPage.css` tiene 1 sola media query para 405 líneas; `IncidentFilterBar.css` 1 para 101. Dos barras de filtros apiladas (`MapDateFilter` + `IncidentFilterBar`) pueden ocupar más de la mitad del viewport en móvil.
* No hay `content-visibility: auto` en listas/tablas largas ni virtualización: `OperatorPanelPage.css` solo tiene `overflow-x: auto` (`:219-220`).
* `@media (prefers-reduced-motion)` no existe en ningún fichero (0 coincidencias) pese a haber `animation: spin` (`MyIncidentsPage.css:207`) y decenas de `transition`. WCAG 2.3.3 / buena práctica.
* `Map.css` (1.080 líneas) mezcla estilos de popup, marcadores, controles y banners: candidato a dividirse por responsabilidad al tocar el popup ligero.

#### 4.8 Iconografía con emojis
Uso de emojis como iconos: `IncidentsMap.jsx:141` (con `aria-hidden`, correcto), `OperatorPanelPage.jsx:698` (`⚠ Posible Duplicado`), `:718` (`⚡ Asistente`). Los emojis varían por plataforma y no heredan color. Al introducir tokens de color conviene sustituirlos por SVG inline o un set mínimo propio. No es bloqueante.

---

## 3. Deuda Técnica y Riesgos

### 3.1 Riesgos de contrato backend/API

| Riesgo | Detalle | Mitigación |
| :--- | :--- | :--- |
| **R1 — Cambio de contrato rompe consumidores** | `/api/v2/incidents` devuelve `List`; pasar a `Page` rompe `MapPage`, `OperatorPanelPage` y sus tests (`MapPage.test.jsx`, `OperatorPanelPage.test.jsx`) | Endpoint nuevo y aditivo para el mapa + `page`/`size` opcionales. Nunca cambiar la forma por defecto en la misma release |
| **R2 — Fuga de dato personal** | `IncidentResponseV2.citizenEmail` (`IncidentResponseV2.java:43`) viaja en la respuesta que consume el mapa ciudadano | El DTO ligero de mapa lo elimina por diseño. Mientras no exista, es un hallazgo a reportar como seguridad, no solo rendimiento |
| **R3 — N+1 en el mapeo masivo** | `IncidentMapperV2` depende de `TareasRepository`; `getActiveIncidentsV2` mapea 5.000 entidades | Verificar con SQL logging; si se confirma, `@EntityGraph`/fetch join o proyección directa a DTO en la query |
| **R4 — `/search` sin límite** | `IncidentControllerV2.java:197-220` devuelve lista completa | Añadir `page`/`size` con default conservador (p. ej. 50) y `totalElements` |
| **R5 — Tests de integración con PostgreSQL** | Los `@EnabledIfPostgresAvailable` se omiten si la BD no está arriba | Al tocar repositorios, levantar `docker compose up -d` y verificar que los tests se ejecutan, no se omiten |

### 3.2 Riesgos de proceso (bloqueantes reales)
* `AGENTS.md` exige `spec.md` aprobada antes de implementar cualquier feature. Issue #60 no es una excepción: el propio issue mezcla 4 frentes con impactos distintos. Debe trocearse en al menos 3 specs (rendimiento del mapa, contrato de listados, sistema visual/accesibilidad) o en una spec con alcance explícitamente acotado y un *"fuera de alcance"*.
* `ADR-04` está aceptado para Leaflet/OSM. Introducir `react-leaflet-cluster` (o `supercluster`) y un endpoint de mapa son decisiones significativas ⇒ ADR nuevo que lo referencie. El `plan.md` debe enlazarlo.
* Convención de commits/ramas (español, sin `ñ`/tildes, `feature/...` desde `develop`) aplica a todo el trabajo derivado.

### 3.3 Riesgos de test/cobertura
* Cobertura actual del frontend: `npm run test:coverage` (v8) — `vite.config.js:11-15` excluye solo `node_modules` y `setupTests.js`, así que CSS y componentes se cuentan.
* Añadir clustering y memoización puede bajar la cobertura si no se añaden tests de las nuevas ramas. Planificar tests junto a cada tarea, no al final.
* `frontend/src/App.css` no importado: si se borra, verificar que ningún test hace `import './App.css'` (no es el caso).

### 3.4 Riesgos de regresión visual
Sustituir hex por tokens en 1.500+ declaraciones es una operación de alto volumen. Debe hacerse fichero a fichero con verificación visual, no en un único commit masivo, o se perderán matices (sombras, degradados, estados hover).

---

## 4. Roadmap por Fases (Tareas Atómicas, Ordenadas por Impacto)

> **Orden = ratio impacto / riesgo.** Las fases 1 y 2 no tocan backend ni contrato y son mergeables de inmediato. Las fases 3+ requieren spec/ADR.

### Fase 0 — Desbloqueo de proceso (0 código)

| ID | Tarea | Entregable |
| :--- | :--- | :--- |
| **0.1** | Trocear Issue #60 en 3 specs (`NNN-mapa-rendimiento`, `NNN-listados-paginados`, `NNN-sistema-visual-a11y`) | 3 `spec.md` en `features/` con estado propuesta |
| **0.2** | ADR de clustering (`react-leaflet-cluster` vs `supercluster`) referenciando `ADR-04` | `docs/ADR/ADR-NN.md` |
| **0.3** | ADR de contrato de listados (endpoint ligero de mapa + paginación aditiva) | `docs/ADR/ADR-NN.md` |
| **0.4** | Baseline medible: tiempo de carga de `/map` y `/operator-panel` con dataset sembrado de 5.000 incidencias | Nota en el `plan.md` con cifras antes/después |

### Fase 1 — Frontend puro, sin backend, máximo impacto

| ID | Tarea | Ficheros | Criterio de aceptación | Esf. |
| :--- | :--- | :--- | :--- | :--- |
| **1.1** | Cachear iconos por estado (`Map` módulo-level) | `frontend/src/utils/leafletIcons.js`, `frontend/src/components/Map/mapConstants.js` | `getIncidentMarkerIcon(s)` devuelve la misma instancia en llamadas sucesivas; test unitario | S |
| **1.2** | `useMemo` para `validIncidents` | `frontend/src/components/Map/IncidentsMap.jsx:124-131` | Sin recálculo si `incidents` no cambia | S |
| **1.3** | `React.memo` en `IncidentsMap` + `useCallback` en handlers de `MapPage` | `IncidentsMap.jsx`, `MapPage.jsx:536` | Un `setToastMessage` no re-renderiza el mapa (verificable con React DevTools Profiler) | S |
| **1.4** | Popup ligero (título, estado, fecha, *"Ver detalle"*) | `frontend/src/components/Map/IncidentsMap.jsx:192-219`, `frontend/src/components/Map/Map.css` | DOM por popup reducido; el detalle sigue accesible vía `IncidentDetailModal` | M |
| **1.5** | `React.lazy` + `Suspense` por ruta | `frontend/src/App.jsx:10-16` | `jspdf` fuera del chunk inicial (verificar en `dist/assets`) | S |
| **1.6** | Pausar polling de notificaciones con pestaña oculta | `frontend/src/components/Notifications/NotificationBell.jsx:55-70` | Sin peticiones con `document.hidden === true` | S |

> **Resultado esperado de la Fase 1:** Desaparición del *jank* en pan/zoom y del coste $O(n)$ por re-render. Es la fase con mejor retorno y la más fácil de justificar.

### Fase 2 — Clustering

| ID | Tarea | Criterio |
| :--- | :--- | :--- |
| **2.1** | Instalar `react-leaflet-cluster` (tras ADR 0.2) y envolver los marcadores en `MarkerClusterGroup` con `chunkedLoading` | 5.000 marcadores agrupados; zoom/pan fluido |
| **2.2** | Iconos de cluster con conteo y color por severidad | Legible a zoom bajo |
| **2.3** | `maxClusterRadius` / `disableClusteringAtZoom` afinados para densidad urbana de Málaga | Detalle individual visible al acercarse |
| **2.4** | Tests: el mock de `../components/Map` en `MapPage.test.jsx` sigue funcionando; añadir test de `IncidentsMap` con 3 incidencias verificando que se renderiza el grupo | Sin regresiones |

### Fase 3 — Contrato backend y listados *(requiere spec 0.1 + ADR 0.3)*

| ID | Tarea | Criterio |
| :--- | :--- | :--- |
| **3.1** | `GET /api/v2/incidents/map` con DTO reducido (sin `description`, sin `citizenEmail`, sin arrays) | Payload ≈ 150 B/incidencia; test de que `citizenEmail` no aparece |
| **3.2** | `MapPage` consume el endpoint de mapa; adaptador de compatibilidad en `incidentService` | `MapPage.test.jsx` actualizado o intacto vía adaptador |
| **3.3** | `page`/`size` opcionales en `/incidents` y `/search` (retrocompatible) | Sin params ⇒ respuesta idéntica a hoy |
| **3.4** | Paginar tabla del panel de operador (patrón `MyIncidentsPage`, `PAGE_SIZE = 10`) + `aria-live` en el indicador | 5.000 registros navegables sin *jank* |
| **3.5** | Verificar/eliminar N+1 en `getActiveIncidentsV2` (`IncidentService.java:755-761`) | SQL logging muestra 1–2 consultas, no 5.000 |
| **3.6** | Virtualizar filas si el panel debe mostrar >50 por página (`@tanstack/react-virtual`, peer React 19 ✅) | Scroll estable a 60 fps |

### Fase 4 — Sistema visual y accesibilidad (`label: accessibility`)

| ID | Tarea | Criterio |
| :--- | :--- | :--- |
| **4.1** | `index.html`: `lang="es"`, `<title>UrbanPulse — Gestión de incidencias urbanas</title>`, `meta description` | WCAG 3.1.1 resuelto |
| **4.2** | Limpiar plantilla Vite de `index.css` (`#social`, `code`, `.counter`, `text-align:center`, `--accent` violeta) | Sin regresiones visuales en las 5 rutas |
| **4.3** | Eliminar `frontend/src/App.css` (no importado) | Build sin referencias |
| **4.4** | Tokens semánticos + modo oscuro completo (`Navbar`, `MyIncidentsPage`, `OperatorPanelPage`, `IncidentFilterBar`, `NotificationBell`, `IncidentCommentsSection`) | Sin texto ilegible en `prefers-color-scheme: dark` |
| **4.5** | `:focus-visible` global + eliminar los 3 `outline: none` | Navegación completa por teclado |
| **4.6** | `Navbar` responsive: hamburguesa + `aria-expanded` + `aria-current="page"` | 360 px sin desbordes |
| **4.7** | Migrar modales a `<dialog>` nativo (empezando por `IncidentDetailModal` y `CreateIncidentModal`) | `Esc`, foco atrapado y fondo inerte sin código manual |
| **4.8** | `IncidentFilterBar` → `<fieldset>`/`role="search"`; quitar `aria-label` redundantes | Anuncio correcto en lector de pantalla |
| **4.9** | `prefers-reduced-motion` para `spin` y transiciones | Sin animación con la preferencia activa |
| **4.10** | `content-visibility: auto` en secciones bajo el pliegue de `AnalyticsPage`/`HomePage` | Mejora de render inicial sin cambio visual |

### Fase 5 — Cierre y verificación
* `cd frontend && npm run lint` y `npm test` (con `docker compose up -d` para que los tests de integración del backend se ejecuten, no se omitan).
* `npm run test:coverage` y comparar con la línea base de Fase 0.4.
* Repetir la medición de 0.4 y documentar el antes/después en el `plan.md`.
* PR a `develop` desde `feature/...`, con plantilla `.github/PULL_REQUEST_TEMPLATE.md`, mensajes en español sin `ñ`/tildes.

---

## Anexo — Mapa de Ficheros Relevantes

```text
frontend/
├── index.html                                    ← lang, title, meta
├── vite.config.js                                ← test/coverage (v8)
├── src/
│   ├── main.jsx                                  ← StrictMode, imports de Leaflet CSS
│   ├── App.jsx                                   ← rutas eager (C6)
│   ├── App.css                                   ← NO importado (muerto)
│   ├── index.css                                 ← tokens + restos de plantilla
│   ├── pages/
│   │   ├── MapPage.jsx                           ← 11 useState, getAllIncidents (:126)
│   │   ├── MapPage.css                           ← 405 líneas, 1 media query
│   │   ├── OperatorPanelPage.jsx                 ← getAllIncidents (:108), tabla (:686)
│   │   ├── OperatorPanelPage.css                 ← 595 líneas, sin dark mode
│   │   ├── MyIncidentsPage.jsx                   ← ✅ patrón de paginación correcto
│   │   └── AnalyticsPage.jsx                     ← arrastra jspdf
│   ├── components/
│   │   ├── Map/IncidentsMap.jsx                  ← 1 Marker+Popup por incidencia
│   │   ├── Map/mapConstants.js                   ← icono nuevo por llamada
│   │   ├── Map/Map.css                           ← 1.080 líneas, dark mode duplicado
│   │   ├── Navbar.jsx / Navbar.css               ← sin responsive ni aria-current
│   │   ├── Filters/IncidentFilterBar.jsx         ← aria-label sin rol, sin debounce propio
│   │   └── Notifications/NotificationBell.jsx    ← polling global
│   ├── services/incidentService.js               ← getAllIncidents / searchIncidents
│   └── utils/
│       ├── leafletIcons.js                       ← createColoredMarkerIcon (C1)
│       └── reportUtils.js                        ← jspdf + autotable
└── Backend/.../controller/v2/IncidentControllerV2.java   ← paginación solo en /my-incidents
```

---

> **Siguiente paso recomendado:** Aprobar el troceo de la **Fase 0 (0.1–0.4)** y autorizar la **Fase 1** como implementación inmediata, ya que no depende de decisiones de arquitectura ni de cambios de contrato. Con tu OK, arranco por **1.1** (cache de iconos), que es el cambio más pequeño con la mayor ganancia medida.