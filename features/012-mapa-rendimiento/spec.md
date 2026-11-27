# Spec 012 — Rendimiento del mapa de incidencias

**Estado:** Propuesta (pendiente de aprobación)

**Origen:** Issue #60 ([Perf]: Mejora visual y de optimización del Frontend) y auditoría `docs/issue-refactor-frontend.md` (hallazgos C1, C2, C5 y C6).

## Contexto y objetivo

El mapa de incidencias es la vista de mayor valor operativo y la que peor escala. Con el conjunto sembrado de 5.000 incidencias (4.500 activas) se degrada por tres vías simultáneas: se recrean los iconos de todos los marcadores en cada renderizado, se mantiene un número muy elevado de nodos en el DOM y cualquier cambio de estado ajeno al mapa (un aviso temporal, el sondeo de notificaciones o el foco) obliga a repintar el árbol completo. El objetivo es que el mapa siga siendo interactivo y fluido a esa escala sin modificar el contrato de lectura del backend, que se aborda en la Spec 013.

## Usuarios / actores

- **Ciudadano autenticado:** consulta el mapa para localizar incidencias cercanas.
- **Operador de triaje:** usa el mapa para priorizar y abrir fichas de incidencias.
- **Técnico:** localiza las incidencias de su ámbito de trabajo.
- **Analista:** revisa la distribución territorial de incidencias.

## Historias de usuario

- **H1:** Como ciudadano, quiero desplazarme y hacer zoom sobre un mapa con miles de incidencias sin que la interfaz se congele, para poder localizar la que me interesa.
- **H2:** Como operador, quiero ver agrupaciones cuando hay muchas incidencias y el detalle individual al acercarme, para priorizar el trabajo sin perder la visión global.
- **H3:** Como usuario del mapa, quiero que acciones ajenas a la cartografía (avisos, notificaciones, foco) no bloqueen ni ralenticen mi navegación.

## Requisitos funcionales (criterios de aceptación en EARS)

### Reutilización de los recursos de representación (H1, H3)

- RF-1: EL SISTEMA reutilizará una única instancia de icono por estado de incidencia en todos los marcadores y en todos los renderizados sucesivos.
- RF-2: CUANDO el conjunto de incidencias recibido no cambie, EL SISTEMA no recalculará la colección de incidencias representables.
- RF-3: CUANDO cambie un estado que no modifique los datos ni las interacciones del mapa, EL SISTEMA no volverá a renderizar el mapa ni sus marcadores.
- RF-4: CUANDO el usuario seleccione una incidencia desde el mapa, EL SISTEMA mostrará su ficha completa reutilizando el detalle ya existente.

### Popup ligero (H1)

- RF-5: MIENTRAS un popup de marcador esté visible, EL SISTEMA mostrará únicamente estado, título y fecha de creación, además de la acción «Ver detalle».
- RF-6: CUANDO el usuario pulse «Ver detalle», EL SISTEMA abrirá la ficha completa de la incidencia y cerrará el popup.

### Agrupación de marcadores (H1, H2)

- RF-7: CUANDO el número de incidencias representables supere 500, EL SISTEMA agrupará visualmente los marcadores geográficamente próximos.
- RF-8: EL SISTEMA mostrará en cada agrupación el número de incidencias que contiene.
- RF-9: CUANDO el usuario amplíe el mapa hasta el nivel de detalle individual, EL SISTEMA desagrupará los marcadores y mostrará cada incidencia por separado.
- RF-10: CUANDO el usuario pulse una agrupación, EL SISTEMA ampliará el mapa al área que contiene sus incidencias.
- RF-11: DONDE una agrupación contenga incidencias de severidades distintas, EL SISTEMA indicará la severidad predominante sin ocultar el contador.

### Carga inicial y sondeo (H3)

- RF-12: CUANDO se cargue la ruta del mapa, EL SISTEMA no descargará ni ejecutará el código del generador de informes.
- RF-13: MIENTRAS la pestaña del navegador esté oculta, EL SISTEMA suspenderá las consultas periódicas de notificaciones.
- RF-14: CUANDO la pestaña vuelva a estar visible, EL SISTEMA reanudará las consultas periódicas de notificaciones.

## Requisitos no funcionales

- NFR-1: Con 4.500 incidencias activas (5.000 totales en el dataset sembrado), las interacciones de desplazamiento y zoom mantendrán 60 fotogramas por segundo en un equipo de referencia `[NECESITA ACLARACIÓN: definir equipo y navegador de referencia]`.
- NFR-2: Ninguna interacción de desplazamiento, zoom o apertura de popup bloqueará el hilo principal durante más de 100 ms.
- NFR-3: Con 5.000 incidencias y el mapa alejado, los nodos del DOM atribuibles a marcadores y agrupaciones no superarán 1.000.
- NFR-4: El paquete inicial de la ruta del mapa no incluirá las librerías de generación de PDF; se verificará mediante análisis del artefacto de compilación.
- NFR-5: Con la pestaña oculta durante 10 minutos, no se emitirá ninguna petición periódica de notificaciones.
- NFR-6: La suma de los contadores de las agrupaciones visibles y de los marcadores individuales coincidirá con el total de incidencias representables.

## Casos límite

- SI una incidencia no tiene coordenadas numéricas válidas, ENTONCES EL SISTEMA la omitirá del mapa sin mostrar error.
- SI no hay incidencias representables, ENTONCES EL SISTEMA mostrará el aviso de «sin incidencias» ya existente.
- SI falla la carga de incidencias, ENTONCES EL SISTEMA mostrará el error y permitirá reintentar.
- SI el usuario tiene abierto el popup de una incidencia que deja de estar disponible, ENTONCES EL SISTEMA cerrará el popup sin bloquear el mapa.
- SI una agrupación contiene una sola incidencia, ENTONCES EL SISTEMA mostrará el marcador individual de esa incidencia.
- SI el usuario cambia de pestaña mientras se está ejecutando una consulta periódica, ENTONCES EL SISTEMA no programará la siguiente mientras la pestaña siga oculta.

## Fuera de alcance

- Reducción del payload, endpoint ligero de mapa y paginación de listados (Spec 013).
- Cambios en el contrato REST de lectura del backend; se abordan en la Spec 013 y el ADR-12.
- Paginación y virtualización del panel de operador (Spec 013).
- Sistema de tokens, modo oscuro, foco visible, navegación móvil y migración de modales (Spec 014).
- Cambios en el contrato de escritura de incidencias.
- Sustitución de Leaflet u OpenStreetMap; el ADR-04 sigue vigente.
- Rediseño visual completo del mapa más allá del popup ligero.
- Medición de latencia de red o de base de datos; se registra en la línea base de `docs/rendimiento/`.

## Criterios de aceptación

_Condiciones verificables que deben cumplirse para dar la funcionalidad por terminada._

- [ ] RF-1 verificado con una prueba que confirma que dos llamadas consecutivas para el mismo estado devuelven la misma instancia de icono.
- [ ] RF-2 y RF-3 verificados con el perfilador de React: un cambio de aviso, foco o notificación no re-renderiza el mapa.
- [ ] RF-5 y RF-6 verificados con pruebas del popup ligero y de la apertura de la ficha completa.
- [ ] RF-7 a RF-11 verificados con 5.000 incidencias: agrupación visible, contador correcto y desagregación al acercarse.
- [ ] RF-12 verificado con el análisis del artefacto de compilación: el generador de PDF no está en el paquete inicial del mapa.
- [ ] RF-13 y RF-14 verificados con la pestaña oculta durante 10 minutos y su posterior reactivación.
- [ ] NFR-1 a NFR-6 medidos y registrados en `docs/rendimiento/experimento-4-baseline-frontend.md`.
- [ ] `npm run lint` y `npm test` en verde, con pruebas nuevas para las ramas añadidas.

## Dudas abiertas

- `[NECESITA ACLARACIÓN]` Umbral exacto de activación de la agrupación (propuesto: 500 incidencias representables), radio de agrupación y nivel de zoom de desagregación. Deben calibrarse con la densidad urbana de Málaga.
- `[NECESITA ACLARACIÓN]` Equipo y navegador de referencia para el umbral de 60 fps y de 100 ms por interacción.
- `[NECESITA ACLARACIÓN]` ¿El umbral de 1.000 nodos del DOM es aceptable o debe ajustarse tras la primera medición?
- `[NECESITA ACLARACIÓN]` ¿La suspensión del sondeo de notificaciones aplica también a la campana cuando el usuario la tiene abierta, o solo al sondeo periódico?
