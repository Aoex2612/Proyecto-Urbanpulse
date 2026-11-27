# Experimento 4: Línea base de rendimiento del frontend (Issue #60)

**Estado:** línea base de API v2, bundle y cobertura completada; métricas de navegador pendientes de ejecución.

**Fecha de definición:** 2026-11-26

**Tareas relacionadas:** Fase 0.4 de `docs/issue-refactor-frontend.md`, Specs 012, 013 y 014, ADR-11 y ADR-12.

## 1. Objetivo

Disponer de una línea base reproducible del rendimiento de las dos vistas críticas de UrbanPulse con un conjunto de 5.000 incidencias activas:

* `/map` (vista cartográfica).
* `/operator-panel` (tabla de triaje).

La línea base debe medir el estado **antes** de aplicar las optimizaciones de las Specs 012 y 013, y repetirse **después** con el mismo entorno, dataset y método para poder comparar cifras. El `plan.md` de cada spec debe citar este documento y registrar en él sus resultados antes/después.

## 2. Entorno de referencia

| Elemento | Valor |
| :--- | :--- |
| Backend | Spring Boot, Java 21, perfil de desarrollo local |
| Base de datos | PostgreSQL en Docker con 5.000 incidencias totales (4.500 activas) |
| Frontend | Compilación de producción servida con `npm run build` y `npm run preview` |
| Navegador | Chrome estable, ventana de incógnito, caché deshabilitada |
| Viewport de escritorio | 1440 × 900 |
| Viewport móvil | 390 × 844 |
| Limitación de CPU | 4× en las trazas de rendimiento |
| Repeticiones | 3 cargas en frío por vista y viewport; se registra la mediana |
| Autenticación | Usuario sembrado con roles `ADMIN` u `OPERATOR` para acceder a ambas rutas |

**Nota:** la línea base existente de `docs/rendimiento/experimento-1-baseline.md` se ejecutó en Linux/CachyOS contra `/api/v1`. La medición de API de esta ejecución se repitió el 2026-11-26 contra `/api/v2` en el equipo local de trabajo (Windows), tal como recoge la sección 6.1.

## 3. Dataset

El dataset se genera con `database/init/diagramUrbanPulse.sql`, que crea 5.000 incidencias totales mediante `generate_series`. Antes de medir se verifica que:

* El número de incidencias activas (excluyendo `CLOSED` y `REJECTED`) coincide con el valor que se va a declarar en la medición. El script actual deja aproximadamente 4.500 activas de las 5.000 totales, porque reserva un 10 % a estados terminales.
* Si el objetivo es medir exactamente 5.000 activas, se amplía el conjunto antes de la medición y se registra el nuevo total; no se debe declarar «5.000 activas» midiendo 4.500.
* Las incidencias tienen coordenadas válidas dentro del área urbana de Málaga.
* Existe al menos una incidencia de cada estado y de cada severidad para poder validar los iconos y la agrupación.
* El usuario de prueba tiene acceso al mapa y al panel de operador.

Si el volumen de Docker no está disponible, la medición queda bloqueada hasta restaurar o resembrar el dataset. No se debe medir con un conjunto menor y presentarlo como línea base de 5.000.

## 4. Métricas que se registran

| Vista | Métrica | Instrumento |
| :--- | :--- | :--- |
| `/map` | Tiempo hasta interactivo (TTI) | Traza de rendimiento de Chrome DevTools |
| `/map` | Largest Contentful Paint (LCP) | Chrome DevTools / API PerformanceObserver |
| `/map` | Tareas largas (> 50 ms) y bloqueo del hilo principal | Traza de rendimiento |
| `/map` | Nodos del DOM tras la carga | DevTools → Elements / Memory |
| `/map` | Memoria JS en reposo | DevTools → Memory |
| `/map` | Fotogramas por segundo en desplazamiento y zoom de 5 s | Traza de rendimiento |
| `/map` | Tamaño y número de peticiones del listado de incidencias | DevTools → Network |
| `/map` | Peticiones periódicas de notificaciones con la pestaña oculta | DevTools → Network |
| `/map` | Tamaño del paquete inicial descargado | Salida de `npm run build` |
| `/operator-panel` | TTI | Traza de rendimiento |
| `/operator-panel` | Tiempo de renderizado de la tabla | Traza de rendimiento |
| `/operator-panel` | Nodos del DOM de la tabla | DevTools → Elements |
| `/operator-panel` | Memoria JS en reposo | DevTools → Memory |
| `/operator-panel` | Latencia percibida al aplicar un filtro | Traza de rendimiento (marca de usuario) |
| `/operator-panel` | Peticiones y tamaño descargado al cargar y filtrar | DevTools → Network |
| API | p50, p90, p95 y payload del listado y de la búsqueda | k6 o medición equivalente documentada |

## 5. Procedimiento

1. Levantar PostgreSQL con el dataset de 5.000 incidencias y arrancar el backend.
2. Verificar el conteo de incidencias activas y las coordenadas.
3. Compilar el frontend y servirlo en modo producción.
4. Iniciar sesión con el usuario de prueba.
5. Abrir `/map` y registrar una traza de 20 segundos que incluya la carga, un desplazamiento y un zoom.
6. Repetir la carga en frío tres veces y anotar la mediana.
7. Abrir `/operator-panel`, registrar la carga de la tabla y aplicar un filtro; repetir tres veces.
8. Ejecutar la vista móvil a 390 × 844 y comprobar que no hay desbordamiento horizontal.
9. Con la pestaña del mapa oculta durante 10 minutos, comprobar si se emiten peticiones de notificaciones.
10. Registrar todas las cifras en la sección 7 y adjuntar las trazas o capturas relevantes.

## 6. Estado actual: cifras «antes»

### 6.1. Backend (medición de esta ejecución)

Medición local del 2026-11-26 contra el backend en `http://localhost:8080` y una base PostgreSQL aislada (`urban_pulse_baseline`) sembrada con `database/init/diagramUrbanPulse.sql`: **5.000 incidencias totales, 4.500 activas y 0 sin coordenadas**. Autenticación con `admin@urbanpulse.es`; tres repeticiones por operación con la JVM en caliente.

> **Reproducibilidad:** la base de medición se creó dentro del contenedor `db_postgres` (`CREATE DATABASE urban_pulse_baseline`) y se sembró ejecutando el script de inicialización sobre ella. No se modificó ningún fichero versionado ni la base de desarrollo original. El backend se arrancó con `SPRING_DATASOURCE_URL` apuntando a esa base y se detuvo al terminar.

| Operación | Mediana | Repeticiones | Payload | Elementos |
| :--- | ---: | :--- | ---: | ---: |
| `GET /api/v2/incidents` (listado completo) | 9.851 ms | 11.540 / 9.851 / 9.611 ms | 4.771.646 bytes (~4,55 MiB) | 4.500 |
| `GET /api/v2/incidents/search?category=ALUMBRADO` | 1.368 ms | 1.521 / 1.368 / 1.276 ms | 659.660 bytes (~644 KiB) | 626 |
| `GET /api/v2/incidents/{id}` (detalle) | 33 ms | 72 / 33 / 27 ms | 757 bytes | 1 |
| `GET /api/v2/incidents/map` | — | 400 | — | Ruta inexistente: cae en `/{id}` con UUID inválido |

**Fuga de dato personal confirmada:** la respuesta del listado contiene `citizenEmail` 4.500 veces, una por incidencia activa.

**N+1 confirmado:** tras reiniciar los contadores de PostgreSQL (`pg_stat_reset()`) y ejecutar **una sola** petición al listado, `pg_stat_user_tables` registra **4.502 `seq_scan` sobre `assignments`** para 4.500 incidencias activas. El mapeo de cada incidencia consulta `TareasRepository` de forma individual. El objetivo de la Spec 013 es un número constante de consultas (1–2).

Estas cifras sustituyen a las de `/api/v1` para la comparación de esta entrega. Se mantienen las de `docs/rendimiento/experimento-1-baseline.md` como referencia histórica:

Tomadas de `docs/rendimiento/experimento-1-baseline.md` con 5.000 incidencias, 10 usuarios virtuales y 2 minutos. Son una referencia, no una medición de esta ejecución:

| Operación | p50 | p95 | Payload por petición |
| :--- | ---: | ---: | ---: |
| `GET /api/v1/incidents` (listado completo) | 5.315 ms | 5.717 ms | ~3,7 MB |
| `GET /api/v1/incidents/search` (búsqueda sin límite) | 781,57 ms | 899,08 ms | ~509 KB |
| `GET /api/v1/incidents/{id}` (detalle) | 8,49 ms | 23,74 ms | ~1 KB |

Estas cifras confirman el diagnóstico de la auditoría: el coste del listado completo es de un orden de magnitud superior al del detalle y está dominado por el tamaño del payload y la serialización de 5.000 registros.

### 6.2. Frontend — compilación y cobertura (medido)

Ejecutado el 2026-11-26 en `frontend/` con Node.js y Vite 8. Los artefactos `dist/` y `coverage/` están ignorados por Git.

| Métrica | Antes (medido) | Objetivo |
| :--- | ---: | ---: |
| JS de entrada principal | 1.063,46 kB (329,77 kB gzip) | ≤ 600 kB y sin `jspdf` en el chunk inicial |
| CSS de entrada | 125,15 kB (23,81 kB gzip) | Sin aumento > 5 % |
| `jspdf` en el chunk inicial | Sí (122 referencias en `index-*.js`) | No; cargar bajo demanda |
| Chunks dinámicos | `html2canvas` 199,48 kB, `index.es` 151,39 kB, `purify.es` 29,07 kB | Mantenerlos fuera de la ruta del mapa |
| Tests | 37 archivos / 303 tests en verde | 100 % en verde |
| `npm run lint` | 0 errores y 0 avisos | Sin errores ni avisos |
| Cobertura de sentencias | 81,68 % | Sin regresión > 1 p.p. |
| Cobertura de ramas | 71,91 % | Sin regresión > 1 p.p. |
| Cobertura de funciones | 76,70 % | Sin regresión > 1 p.p. |
| Cobertura de líneas | 82,86 % | Sin regresión > 1 p.p. |
| Cobertura de `IncidentsMap.jsx` | 0 % (sin test directo) | ≥ 70 % |

### 6.3. Frontend — métricas de navegador (pendientes)

| Vista | Métrica | Antes | Objetivo |
| :--- | :--- | ---: | ---: |
| `/map` | TTI (ms) | Pendiente | ≤ 3.000 ms |
| `/map` | LCP (ms) | Pendiente | ≤ 2.500 ms |
| `/map` | Nodos del DOM | Pendiente | ≤ 1.000 con el mapa alejado |
| `/map` | Memoria JS (MB) | Pendiente | ≤ 150 MB |
| `/map` | FPS en pan/zoom | Pendiente | 60 fps sin tareas > 100 ms |
| `/map` | Peticiones con pestaña oculta (10 min) | Pendiente | 0 |
| `/operator-panel` | TTI (ms) | Pendiente | ≤ 3.000 ms |
| `/operator-panel` | Nodos del DOM de la tabla | Pendiente | ≤ 500 por página |
| `/operator-panel` | Memoria JS (MB) | Pendiente | ≤ 100 MB |
| `/operator-panel` | Latencia de filtro (ms) | Pendiente | ≤ 500 ms |
| `/operator-panel` | Payload al cargar (KB) | Pendiente | ≤ 50 KB por página |

**Estado del bloqueo:** Docker Desktop, PostgreSQL y el backend ya están disponibles, y la base aislada `urban_pulse_baseline` contiene 5.000 incidencias (4.500 activas). Las métricas de navegador siguen pendientes porque requieren una traza manual de Chrome DevTools o una herramienta de automatización que no está instalada; instalar dependencias queda fuera de la Fase 0. El procedimiento exacto está en la sección 5 y no se rellenan cifras estimadas para no contaminar la comparación.

### 6.4. Accesibilidad y sistema visual — inspección estática (medido)

Inspección del código el 2026-11-26, sin herramientas externas. Es la línea base de la Spec 014.

| Métrica | Antes (medido) | Objetivo |
| :--- | ---: | ---: |
| Idioma del documento | `lang="en"` (`frontend/index.html:2`) | `lang="es"` |
| Título del documento | `frontend` (`frontend/index.html:8`) | Título descriptivo + `meta description` |
| `outline: none` | 18 apariciones en 14 ficheros CSS | 0 sin sustituto visible |
| `:focus-visible` global | No existe | Regla global de 2 px, contraste ≥ 3:1 |
| `prefers-reduced-motion` | 0 apariciones | Cobertura de animaciones y transiciones |
| `prefers-color-scheme` | 11 de 27 ficheros CSS | Todos los ficheros con superficies y texto |
| Media queries en `Navbar.css` | 0 | Menú colapsable < 768 px |
| `aria-current="page"` | 1 aparición (`AnalyticsPage`), 0 en `Navbar` | Enlace activo en todas las rutas |
| Modales con `div` + `role="dialog"` | 7 componentes | `<dialog>` nativo en todos |
| `App.css` | Existe y no se importa en ningún punto | Eliminado |
| Auditoría WCAG automática | No ejecutada (sin herramienta instalada) | Lighthouse ≥ 95 o axe sin incidencias críticas/graves |

## 7. Umbrales de aceptación «después»

Los umbrales se fijan a partir de la línea base medida y se confirmarán tras la implementación. La comparación debe cumplir como mínimo:

* El payload del listado del mapa baja de 4,55 MiB (4.771.646 bytes medidos) a menos de 1 MB sin comprimir para 4.500 incidencias activas.
* La respuesta ligera del mapa no contiene el correo del ciudadano ni las colecciones no necesarias.
* El número de nodos del DOM atribuibles a marcadores y agrupaciones no supera 1.000 con el mapa alejado.
* El desplazamiento y el zoom se mantienen a 60 fps y sin tareas largas superiores a 100 ms.
* La tabla del panel de operador carga 10 filas por página y no descarga el catálogo completo.
* Con la pestaña oculta durante 10 minutos no se emite ninguna petición periódica de notificaciones.
* El paquete inicial del mapa no incluye el generador de PDF.
* La comparación antes/después se registra en este documento y en el `plan.md` de cada spec.

## 8. Limitaciones

* La medición del frontend en `npm run preview` no reproduce exactamente una red móvil real ni un dispositivo de gama baja; el factor 4× de CPU es una aproximación documentada.
* Las cifras de backend de la sección 6.1 se midieron en Windows, en local y sin concurrencia; las de `experimento-1-baseline.md` corresponden a `/api/v1`, Linux/CachyOS y 10 usuarios virtuales. No son directamente comparables entre sí.
* La base aislada contiene 4.500 incidencias activas de 5.000 totales; las cifras se etiquetan con ese valor y no como «5.000 activas».
* La agrupación de marcadores (ADR-11) y la paginación (ADR-12) se medirán por separado y también de forma combinada, porque afectan a ejes distintos: DOM frente a payload.
* Este documento no incluye cambios de código de producto; es un protocolo de medición y un registro de resultados.
