# ADR-11: Agrupación de marcadores del mapa con react-leaflet-cluster

* Estado: propuesto
* Fecha: 2026-11-26
* Decisiones relacionadas: `docs/ADR/ADR-04-cartografia-interactiva-leaflet-openstreetmap.md` (cartografía interactiva con Leaflet y OpenStreetMap), ADR-12 (contrato de lectura de listados), Spec 012 (rendimiento del mapa de incidencias)

## Contexto

El mapa de incidencias representa un marcador y un popup del DOM por cada incidencia activa. Con el volumen objetivo de 5.000 incidencias, esto mantiene decenas de miles de nodos y provoca un deterioro severo del desplazamiento y del zoom. La auditoría `docs/issue-refactor-frontend.md` (hallazgos C1 y C2) demuestra que el coste dominante es la recreación de iconos en cada renderizado, seguido del número de marcadores y popups montados. La recreación de iconos se corrige con independencia de esta decisión (Spec 012), pero no reduce el DOM.

El `ADR-04-cartografia-interactiva-leaflet-openstreetmap.md` fijó Leaflet, `react-leaflet` 5 y teselas de OpenStreetMap como base cartográfica, con React 19 como framework cliente. La nueva decisión debe ser compatible con ese stack, preferiblemente declarativa, y no puede alterar el contrato de lectura del backend, que se aborda en el ADR-12 y en la Spec 013.

## Opciones consideradas

* **Opción 1: `react-leaflet-cluster` `^4.1.3` con `leaflet.markercluster` `^1.5.3`.** Envoltorio declarativo sobre `leaflet.markercluster`. Declara compatibilidad exacta con React 19, `react-leaflet` 5, `@react-leaflet/core` 3 y Leaflet 1.9.
* **Opción 2: `leaflet.markercluster` `^1.5.3` en modo imperativo.** API original de Leaflet gestionada manualmente mediante un componente auxiliar y `useMap`.
* **Opción 3: `supercluster` `^9.1.0` con renderizado manual.** Cálculo de agrupaciones desacoplado del DOM de Leaflet y pintado con `CircleMarker`/`divIcon` propios.
* **Opción 4: mantener el mapa sin agrupación.** Limitarse a cachear iconos, memoizar y reducir el popup.

## Decisión tomada

Optamos por la **Opción 1: `react-leaflet-cluster` `^4.1.3` con `leaflet.markercluster` `^1.5.3`**, envolviendo los marcadores en un grupo de agrupación con carga troceada, agrupación por proximidad y desagregación al nivel de detalle individual. `supercluster` `^9.1.0` queda documentado como alternativa de plan B.

La configuración inicial parte de estos valores, ajustables tras la medición de la línea base:

* Activación de la agrupación cuando el número de incidencias representables supere 500.
* `chunkedLoading` activado para no bloquear el hilo principal al procesar miles de marcadores.
* Radio de agrupación y nivel de desagregación calibrados con la densidad urbana de Málaga, sin fijarlos en este ADR.
* Iconos de agrupación con contador y color de severidad predominante.
* Si el perfilado con 5.000 incidencias no alcanza los umbrales de la Spec 012, se evaluará la Opción 3 (`supercluster`) como plan B, manteniendo el mismo comportamiento de interfaz.

## Razón / Por qué se tomó la decisión

* **Compatibilidad exacta con el stack vigente:** los peers declarados por `react-leaflet-cluster` 4.1.3 coinciden con React 19, `react-leaflet` 5 y Leaflet 1.9, por lo que no obliga a degradar ni a sustituir la base fijada en el ADR-04.
* **Integración declarativa:** permite seguir usando componentes de React dentro del árbol del mapa y reduce el código de ciclo de vida frente a la API imperativa de `leaflet.markercluster`.
* **Menor coste de mantenimiento y de revisión:** el comportamiento de agrupación queda concentrado en un envoltorio con una API conocida, en lugar de repartirse entre hooks, eventos y limpieza manual de capas.
* **Carga troceada:** `chunkedLoading` evita que el procesamiento inicial de miles de marcadores bloquee la interacción, que es el objetivo principal de la Spec 012.
* **Sin impacto en backend:** la agrupación es una decisión de presentación; no requiere endpoint ni cambio de contrato.
* **La caché de iconos no es una alternativa:** es un requisito previo e independiente. Sin ella, cada renderizado seguiría recreando los iconos de todos los marcadores, estén agrupados o no.

## Consecuencias

* **Positivo:** reducción drástica de los nodos del DOM en zoom bajo, mejora del desplazamiento y del zoom, y conservación de la navegación individual al acercarse.
* **Negativo:** se añade una dependencia de terceros y peso al paquete del mapa; los iconos de agrupación requieren estilos y revisión visual propios; el DOM de las agrupaciones sigue existiendo y debe medirse.
* **Negativo:** `react-leaflet-cluster` debe mantener la compatibilidad con React 19 y `react-leaflet` 5; una actualización incompatible obligaría a la Opción 2 o a la Opción 3.
* **Neutral / trade-off DOM frente a payload:** la agrupación reduce drásticamente los nodos del DOM, pero **no reduce el payload ni la latencia de red**. Esa mejora depende del endpoint ligero de mapa y de la paginación del ADR-12 y de la Spec 013. Medir solo el DOM ocultaría el coste de los 4,55 MiB que siguen viajando.
* **Neutral:** las pruebas del mapa deberán simular el entorno de Leaflet y verificar la agrupación con un conjunto pequeño de incidencias.
* **Riesgo:** si el rendimiento no alcanza el umbral con 5.000 incidencias, se sustituirá por `supercluster` sin cambiar el comportamiento observable definido en la Spec 012.
* **Seguridad y privacidad:** sin impacto directo; la agrupación no altera los datos recibidos.
* **Accesibilidad:** las agrupaciones deben exponer un nombre comprensible y no depender solo del color; se validará junto con la Spec 014.
