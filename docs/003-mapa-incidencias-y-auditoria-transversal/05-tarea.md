# Tarea 5: Componente de Mapa Cartográfico con Leaflet y Modal de Creación

**Feature:** `003-mapa-incidencias-y-auditoria-transversal`  
**Estado:** Completada  
**Requisitos funcionales relacionados:** RF-1, RF-2, RF-4, RF-5, RF-6, RF-7, RF-8, RF-9, RF-11, RF-12  

---

## 1. Resumen de la Tarea

En esta tarea se desarrollaron los componentes centrales de la experiencia geoespacial e interactiva de UrbanPulse en el cliente web:

1. **Componente de Mapa Cartográfico (`IncidentsMap.jsx` y `mapConstants.js`):**
   - Implementado mediante `react-leaflet` (`MapContainer`, `TileLayer`, `Marker`, `Popup`).
   - Centrado por defecto en el núcleo urbano de Málaga (`[36.7213, -4.4214]`, zoom 13) utilizando capas de teselas de OpenStreetMap.
   - Posicionamiento dinámico de marcadores para las incidencias activas consumidas por props.
   - Cada marcador incluye un `Popup` nativo con los datos básicos de la incidencia: título, descripción, categoría predefinida, badge del estado operativo y botón de acción "Ver detalle completo" que invoca el callback `onSelectIncident(incident)`.
   - Asignación de iconos mediante `defaultIcon` y `createColoredMarkerIcon` desde `leafletIcons.js`, permitiendo distinguir visualmente estados de las incidencias (ámbar para `REPORTED`, azul para `IN_PROGRESS`, verde para `RESOLVED`, etc.).
2. **Captura Asistida de Coordenadas (`useMapEvents`):**
   - Subcomponente interno `MapEventsHandler` que escucha los clics en el lienzo del mapa de Leaflet.
   - Activado **únicamente** cuando la prop `isSelectingLocation` es verdadera (`true`). Al hacer clic sobre el mapa, captura `e.latlng.lat` y `e.latlng.lng` y ejecuta el callback `onLocationSelected({ latitude, longitude })`.
   - Cuando el modo de selección está desactivado, los clics sobre el lienzo cartográfico siguen el comportamiento nativo de Leaflet (cierre de popups abiertos sin alterar el estado ni crear marcadores).
   - Indicador flotante en la parte superior del mapa informando al usuario cuando el modo de selección se encuentra activo.
3. **Modal de Creación Asistida (`CreateIncidentModal.jsx`):**
   - Modal accesible (`role="dialog"`, `aria-modal="true"`) con formulario de alta conteniendo:
     - Título (campo de texto obligatorio con validación de longitud).
     - Categoría (desplegable obligatorio con catálogo cerrado: `Alumbrado`, `Vías Públicas`, `Limpieza`, `Parques y Jardines`, `Mobiliario Urbano`).
     - Descripción (área de texto obligatoria con validación de contenido mínimo).
     - Coordenadas capturadas (latitud y longitud en inputs de solo lectura `readOnly` con formato decimal).
   - Validación visual preventiva en el cliente antes de la emisión HTTP.
   - Botón de cancelación y cruz de cierre que invocan `handleDismiss` (`onClose` / `onCancel`), descartando las coordenadas y limpiando el estado sin dejar marcadores temporales residuales.
   - Persistencia a través de `incidentService.createIncident` (o callback `onSubmit`), reseteando el formulario y notificando mediante `onCreated(newIncident)` para incorporar de inmediato el nuevo marcador al mapa.
4. **Diseño Visual y Estilos (`Map.css`):**
   - Dimensiones responsivas con `min-height: 520px` y `height: 100%`.
   - Cambio dinámico de cursor a `crosshair` durante el modo de selección.
   - Diseño refinado para tarjetas de popup, badges de estados con transparencias, rejilla de coordenadas y modales modernos compatibles con modo claro y oscuro (`@media (prefers-color-scheme: dark)`).
5. **Verificación y Calidad:**
   - 0 errores y 0 advertencias en ESLint (`npm run lint`).
   - Compilación exitosa en Vite (`npm run build`).
   - 100% de tests unitarios y de integración de backend pasando satisfactoriamente (166 tests en `mvn clean test`).

---

## 2. Subtareas Implementadas

### 2.1 Componente Cartográfico `IncidentsMap.jsx` y Constantes (Subtarea 5.1)
- **`frontend/src/components/Map/mapConstants.js`:**
  - Definición centralizada de constantes `DEFAULT_MAP_CENTER` (`[36.7213, -4.4214]`), `DEFAULT_MAP_ZOOM` (`13`) y catálogo `INCIDENT_CATEGORIES`.
  - Funciones utilitarias `getStatusLabel` (traducción de estados a etiquetas en español) y `getIncidentMarkerIcon` (asignación de `createColoredMarkerIcon` o `defaultIcon` según estado).
- **`frontend/src/components/Map/IncidentsMap.jsx`:**
  - Configuración del mapa con `MapContainer` y `TileLayer` de OpenStreetMap.
  - Filtrado preventivo de incidencias asegurando coordenadas numéricas válidas antes de instanciar marcadores.
  - Renderizado de marcadores con popups nativos estructurados con título, categoría, descripción y botón interactivo "Ver detalle completo".
  - Renderizado condicional de marcador temporal para `selectedLocation` con icono distintivo.
  - JSDoc completo en todos los componentes y funciones.

### 2.2 Escucha de Clics en el Lienzo del Mapa (Subtarea 5.2)
- Subcomponente interno `MapEventsHandler` integrado dentro del contexto de `MapContainer` usando el hook `useMapEvents` de `react-leaflet`.
- Condicionado estrictamente a `isSelectingLocation === true`: fuera de este modo, los clics en el mapa no emiten ningún evento de creación y permiten la navegación y cierre de popups estándar.
- Cambio de cursor en el mapa mediante la clase `.map-selection-active` y banner flotante animado informando del modo activo.

### 2.3 Modal de Creación de Incidencia (Subtarea 5.3 y 5.4)
- **`frontend/src/components/Map/CreateIncidentModal.jsx`:**
  - Formulario con campos de título, descripción, selector desplegable predefinido y coordenadas geográficas fijadas en modo solo lectura.
  - Validación en frontend comprobando campos obligatorios, longitudes mínimas y límites de coordenadas geográficas.
  - Manejo de ciclo de vida con autofocus automático en el campo de título al abrirse y cierre mediante la tecla `Escape`.
  - Integración asíncrona con `incidentService.createIncident` con control de estados de carga (`isSubmitting`) y banners visuales ante errores de servidor.
  - Limpieza rigurosa de estado tras confirmación o descarte sin dejar marcadores huérfanos.

---

## 3. Archivos Modificados y Creados

| Archivo | Tipo de Cambio | Descripción |
|---------|----------------|-------------|
| `frontend/src/components/Map/Map.css` | Creado | Hoja de estilos para el mapa Leaflet, popups nativos, badges y modal de creación con soporte claro/oscuro. |
| `frontend/src/components/Map/mapConstants.js` | Creado | Constantes cartográficas, catálogo de categorías y formateadores de estado e iconos. |
| `frontend/src/components/Map/IncidentsMap.jsx` | Creado | Componente cartográfico interactivo con OpenStreetMap, marcadores y escucha condicional de coordenadas. |
| `frontend/src/components/Map/CreateIncidentModal.jsx` | Creado | Componente modal de alta de incidencias geolocalizadas con validación en frontend y persistencia vía API. |
| `frontend/src/components/Map/index.js` | Creado | Archivo barril con exportaciones limpias del módulo cartográfico. |
| `features/003-mapa-incidencias-y-auditoria-transversal/tasks.md` | Modificado | Marcada la Tarea 5 y subtareas 5.1 a 5.4 como completadas `[x]`. |
| `docs/003-mapa-incidencias-y-auditoria-transversal/05-tarea.md` | Creado | Documentación técnica detallada de la Tarea 5. |

---

## 4. Validación y Resultados de Pruebas

### 4.1 Linter de Frontend (`npm run lint`)

```bash
npm --prefix frontend run lint
```

**Resultado:**
```
> frontend@0.0.0 lint
> eslint .

Exit Code: 0 (0 errores, 0 advertencias)
```

### 4.2 Compilación de Producción de Frontend (`npm run build`)

```bash
npm --prefix frontend run build
```

**Resultado:**
```
> frontend@0.0.0 build
> vite build

vite v8.3.3 building client environment for production...
transforming...
✓ 91 modules transformed.
rendering chunks...
computing gzip size...
dist/index.html                   0.47 kB │ gzip:   0.30 kB
dist/assets/index-BCxKg8OO.css   27.50 kB │ gzip:   9.07 kB
dist/assets/index-C_7UoZs9.js   328.37 kB │ gzip: 105.49 kB
✓ built in 282ms
```

### 4.3 Pruebas de Backend (`mvn clean test`)

```bash
cd Backend && mvn clean test
```

**Resultado:**
```
[INFO] Tests run: 166, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time: 24.405 s
```

---

## 5. Siguiente Paso

- **Tarea 6:** Modal de Detalle de Incidencia y Componente Modular `Timeline` de Auditoría (`features/003-mapa-incidencias-y-auditoria-transversal/tasks.md`).
