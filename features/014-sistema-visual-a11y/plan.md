# 014 — Sistema visual, navegación responsive y accesibilidad — Plan

_¿Cómo se implementa lo descrito en `spec.md`? Respeta estrictamente la `constitution/` y no altera contratos de API ni el backend._

## Enfoque

La intervención se organiza en cuatro pasadas incrementales y revisables por separado:

1. **Metadatos y limpieza de plantilla:** idioma, título, descripción y eliminación de estilos muertos.
2. **Sistema de tokens y modo oscuro:** una capa semántica en `index.css` y sustitución progresiva de valores literales, fichero a fichero.
3. **Foco, navegación y semántica:** `:focus-visible` global, `aria-current`, menú móvil, `<fieldset>`/`role="search"` y modales nativos.
4. **Movimiento y renderizado:** `prefers-reduced-motion` y `content-visibility` en secciones bajo el pliegue.

No se rediseña la identidad visual ni se sustituyen los emojis por un set propio en esta entrega. No se requiere ADR nuevo: son cambios de presentación y accesibilidad que no alteran arquitectura, persistencia ni contratos. Se mantiene vigente el ADR-04 para la cartografía.

## Implementación

### 1. Idioma, metadatos y limpieza de la plantilla

- `frontend/index.html`:
  - `lang="es"`.
  - `<title>UrbanPulse — Gestión de incidencias urbanas</title>`.
  - `<meta name="description" content="...">` descriptiva.
- `frontend/src/index.css`:
  - Eliminar `#social`, `.button-icon`, `code`, `.counter` y cualquier selector sin uso.
  - Quitar `text-align: center`, `max-width: 1540px` y `border-inline` de `#root`; cada vista gestiona su propio ancho.
  - Revisar `--accent` violeta: sustituirlo por el token de acento real y actualizar el marcador temporal de selección del mapa si lo usa.
- `frontend/src/App.css`:
  - Eliminar el fichero (no se importa en ningún punto; verificado antes de borrar que ningún test lo referencia).

### 2. Tokens semánticos y modo oscuro

- `frontend/src/index.css`:
  - Definir tokens en `:root`: `--surface`, `--surface-muted`, `--surface-raised`, `--text-primary`, `--text-secondary`, `--text-inverse`, `--border-subtle`, `--border-strong`, `--focus-ring`, `--accent-*` por estado y severidad, `--shadow-*`.
  - Definir el bloque `@media (prefers-color-scheme: dark)` con los mismos tokens invertidos y `color-scheme: light dark`.
  - No duplicar la paleta en cada fichero; los componentes consumen `var(--token)`.
- Sustitución por fichero, con revisión visual después de cada uno:
  - `frontend/src/components/Navbar.css`
  - `frontend/src/pages/MyIncidentsPage.css`
  - `frontend/src/pages/OperatorPanelPage.css`
  - `frontend/src/components/Filters/IncidentFilterBar.css`
  - `frontend/src/components/Notifications/NotificationBell.css`
  - `frontend/src/components/Comments/IncidentCommentsSection.css`
  - `frontend/src/components/Map/Map.css` (unificar el bloque oscuro duplicado)
- Verificar contraste 4,5:1 en texto normal y 3:1 en texto grande/componentes, en estados normal, hover, foco, activo y deshabilitado.

### 3. Foco visible y teclado

- `frontend/src/index.css`:
  - Regla global `:focus-visible { outline: 2px solid var(--focus-ring); outline-offset: 2px; }`.
  - No eliminar el foco sin sustituto visible.
- Eliminar los `outline: none` de los 14 ficheros detectados (18 apariciones). Prioridad inicial:
  - `frontend/src/pages/Auth.css` (`.form-input:focus`).
  - `frontend/src/pages/MyIncidentsPage.css` (`.status-filter-select:focus`).
  - `frontend/src/pages/OperatorPanelPage.css` (`.status-select:focus`).
  - Resto: `IncidentCommentsSection.css`, `IncidentFilterBar.css`, `Map.css`, `MapDateFilter.css`, `NotificationBell.css`, `PriorityAssistantModal.css`, `ReasonModal.css`, `ResolveIncidentModal.css`, `AnalyticsDateFilter.css`, `SlaComplianceChart.css` y `TerritorialChart.css`.
- Revisar que los controles personalizados (selects, botones de icono, enlaces del navbar) conservan el anillo de foco.

### 4. Navegación responsive

- `frontend/src/components/Navbar.jsx`:
  - Añadir `aria-current="page"` al enlace de la sección activa.
  - Añadir un botón de menú con `aria-expanded` y `aria-controls` para el bloque de enlaces.
  - Exponer el estado expandido/contraído a tecnologías de asistencia.
  - Mantener `aria-label="Navegación principal"` en el contenedor.
- `frontend/src/components/Navbar.css`:
  - Breakpoint único compartido (propuesta: 768 px; pendiente de aprobación).
  - Menú colapsable por debajo del breakpoint, sin desbordamiento horizontal a 320 px.
  - Distinguir la sección activa por al menos dos medios visuales (color + subrayado o borde), no solo por color.
- Verificar que la campana de notificaciones y «Cerrar sesión» siguen accesibles dentro del menú colapsado.

### 5. Modales nativos `<dialog>`

- Migración incremental, empezando por los más usados desde el mapa:
  1. `frontend/src/components/Map/IncidentDetailModal.jsx`
  2. `frontend/src/components/Map/CreateIncidentModal.jsx`
  3. `frontend/src/components/Triage/AssignmentModal.jsx`
  4. `frontend/src/components/Triage/ReasonModal.jsx`
  5. `frontend/src/components/Triage/ResolveIncidentModal.jsx`
  6. `frontend/src/components/Triage/PriorityAssistantModal.jsx`
  7. Modal embebido en `frontend/src/pages/OperatorPanelPage.jsx`
- Patrón:
  - Usar `<dialog>` con `showModal()` y `close()`.
  - Eliminar el foco atrapado manual, el listener de `keydown` para Escape y el `role="dialog"`/`aria-modal` duplicados.
  - Escuchar `cancel` para cerrar y devolver el foco al elemento que abrió el modal.
  - Mantener el bloqueo de fondo que aporta el top-layer nativo.
- Mantener la API de props y el comportamiento observable de cada modal para no romper los tests existentes.

### 6. Semántica de filtros

- `frontend/src/components/Filters/IncidentFilterBar.jsx`:
  - Sustituir el `div` con `aria-label` por `<fieldset>` con `<legend>` visible o visualmente oculto, o por `role="search"`/`role="group"` con nombre accesible.
  - Eliminar los `aria-label` redundantes de los controles que ya tienen `<label htmlFor>`.
  - Conservar `role="status"` del estado «sin resultados».
- Verificar el árbol de accesibilidad: cada control debe anunciarse una sola vez, con nombre, rol y valor.

### 7. Movimiento reducido y renderizado

- `frontend/src/index.css` y ficheros con animaciones:
  - Añadir `@media (prefers-reduced-motion: reduce)` para desactivar `animation: spin`, transiciones no esenciales y desplazamientos.
  - Mantener el estado de carga visible sin animación.
- `frontend/src/pages/HomePage.css` y `frontend/src/pages/AnalyticsPage.css`:
  - Aplicar `content-visibility: auto` y `contain-intrinsic-size` a las secciones bajo el pliegue.
  - Verificar que el contenido se muestra al hacer scroll y al imprimir.

## Decisiones

- **Tokens semánticos en `index.css`** — Una única fuente de verdad evita duplicar la paleta y permite completar el modo oscuro con un solo bloque. Se descarta crear un fichero de tema separado en esta entrega para no fragmentar la carga inicial.
- **`<dialog>` nativo** — Aporta semántica, top-layer, Escape y foco atrapado sin código propio. Se descarta mantener los overlays manuales y se evita duplicar `role="dialog"`/`aria-modal`.
- **Migración modal a modal** — Reduce el riesgo de regresión y permite revisar cada uno; no se hace un cambio masivo en un único commit.
- **Breakpoint único** — Se propone 768 px para navbar y vistas; se mantienen los breakpoints existentes de 640 px solo donde ya estén justificados (pendiente de aprobación en la spec).
- **No sustituir emojis por iconos propios** — Se pospone; solo se exige que los decorativos queden ocultos a tecnologías de asistencia.
- **Sin ADR nuevo** — No hay decisión arquitectónica: se usan estándares web y el stack existente.

## Línea base (Antes / Objetivo)

Inspección estática y medición local del 2026-11-26. No se pudo ejecutar una auditoría automática de accesibilidad porque requeriría instalar herramientas nuevas, prohibido en la Fase 0.

| Métrica | Antes (medido) | Objetivo | Fuente |
| :--- | ---: | ---: | :--- |
| Idioma del documento | `lang="en"` | `lang="es"` | `frontend/index.html:2` |
| Título del documento | `frontend` | Título descriptivo + `meta description` | `frontend/index.html:8` |
| `outline: none` sin sustituto | 18 apariciones en 14 ficheros CSS | 0 | `grep` en `src/**/*.css` |
| `:focus-visible` global | No existe | Regla global de 2 px y contraste ≥ 3:1 | `grep` en `src/**/*.css` |
| `prefers-reduced-motion` | 0 apariciones | Cobertura de animaciones y transiciones | `grep` en `src/**/*.css` |
| `prefers-color-scheme` | 11 de 27 ficheros CSS; sin cobertura en `Navbar`, `MyIncidentsPage`, `OperatorPanelPage`, `IncidentFilterBar`, `NotificationBell`, `IncidentCommentsSection`, `Auth` y los modales | Todos los ficheros con superficies y texto | `grep` en `src/**/*.css` |
| Media queries en `Navbar.css` | 0 | Menú colapsable < 768 px | `grep` en `Navbar.css` |
| `aria-current="page"` | 1 aparición en `AnalyticsPage`; 0 en `Navbar` | Enlace activo en todas las rutas | `grep` en `src/**/*.jsx` |
| Modales con `div` + `role="dialog"` | 7 | `<dialog>` nativo en todos | Inspección de código |
| `aria-label` en `div` sin rol (filtros) | 1 | `<fieldset>`/`role="search"` con nombre accesible | `IncidentFilterBar.jsx:89` |
| Desbordamiento horizontal a 320 px | Pendiente de medición en navegador | 0 | DevTools |
| Auditoría WCAG automática | Pendiente (sin herramienta instalada) | Lighthouse ≥ 95 o axe sin incidencias críticas/graves | Lighthouse / axe |
| Tests frontend | 37 archivos / 303 tests en verde | 100 % en verde + tests de accesibilidad | `npm test` |
| `npm run lint` | 0 errores y 0 avisos | Sin errores ni avisos | `npm run lint` |
| Cobertura de sentencias | 81,68 % | Sin regresión > 1 p.p. | `npm run test:coverage` |
| CSS de entrada | 125,15 kB (23,81 kB gzip) | Sin aumento > 10 % | `npm run build` |

## Riesgos

- **Riesgo: regresión visual al sustituir cientos de valores literales por tokens.** Mitigación: migración fichero a fichero con capturas de referencia y revisión en modo claro y oscuro.
- **Riesgo: el modo oscuro herede controles nativos ilegibles.** Mitigación: definir `color-scheme` y probar inputs, selects, scrollbars y botones del navegador.
- **Riesgo: `<dialog>` cambie el comportamiento de foco o el cierre en tests.** Mitigación: migrar primero los dos modales del mapa y adaptar los tests con `showModal`/`close` simulados en jsdom.
- **Riesgo: el menú móvil rompa la navegación existente.** Mitigación: mantener los enlaces y el `aria-label`, probar a 320, 390 y 768 px.
- **Riesgo: `content-visibility` retrase la impresión o el scroll.** Mitigación: verificar impresión y búsqueda del navegador; limitarlo a secciones bajo el pliegue.
- **Riesgo: la auditoría automática no esté disponible en CI.** Mitigación: documentar la herramienta y el umbral, y complementar con revisión manual de teclado y árbol de accesibilidad.

## Estrategia de tests

- **Componentes:** `Navbar` (aria-current, aria-expanded, menú móvil), `IncidentFilterBar` (fieldset/legend, sin doble anuncio), modales migrados (Escape, foco atrapado, retorno de foco).
- **Accesibilidad:** pruebas con `@testing-library/jest-dom` para roles y nombres accesibles; revisión manual del árbol de accesibilidad.
- **Visual:** capturas de referencia a 1440, 768 y 390 px en modo claro y oscuro; comprobar ausencia de desbordamiento a 320 px.
- **Regresión:** mantener verdes los 303 tests actuales y añadir los nuevos casos de accesibilidad.
- **Auditoría:** Lighthouse o axe sobre las cinco rutas principales; registrar la puntuación antes/después.

## Referencias

- `features/014-sistema-visual-a11y/spec.md`
- `docs/ADR/ADR-04-cartografia-interactiva-leaflet-openstreetmap.md`
- `docs/issue-refactor-frontend.md` (hallazgos C7, C8 y área 4)
- `docs/rendimiento/experimento-4-baseline-frontend.md`
