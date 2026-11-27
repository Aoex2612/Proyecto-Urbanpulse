# Spec 013 — Lectura paginada y respuesta ligera de incidencias

**Estado:** Propuesta (pendiente de aprobación)

**Origen:** Issue #60 ([Perf]: Mejora visual y de optimización del Frontend) y auditoría `docs/issue-refactor-frontend.md` (hallazgos C3, C4 y riesgos R1–R4).

## Contexto y objetivo

Los listados de incidencias se sirven completos y sin límite. La medición del 2026-11-26 contra `/api/v2` con 4.500 incidencias activas registra una respuesta de **4.771.646 bytes (~4,55 MiB) y una mediana de 9.851 ms** en el listado, y **659.660 bytes (~644 KiB) y 1.368 ms** en una búsqueda por categoría con 626 resultados. Esa respuesta incluye datos que el mapa no necesita y expone el correo electrónico del ciudadano 4.500 veces, mientras que el panel de operador descarga el conjunto completo y lo filtra en el navegador. Esta especificación define una lectura ligera para el mapa y una paginación aditiva para los listados, manteniendo la compatibilidad de los clientes existentes y eliminando los datos personales de la respuesta cartográfica.

## Usuarios / actores

- **Ciudadano autenticado:** consulta el mapa y necesita que cargue rápido sin que se expongan sus datos personales.
- **Operador y técnico:** trabajan con el panel de operador y necesitan navegar por páginas y filtrar sin descargar todo el catálogo.
- **Analista:** consulta listados filtrados y necesita totales fiables.
- **Consumidores existentes del API:** requieren que su forma de respuesta actual no se rompa.
- **Equipo de privacidad y seguridad:** requiere que el correo del ciudadano no viaje en la vista ciudadana.

## Historias de usuario

- **H1:** Como ciudadano, quiero que el mapa cargue solo la información imprescindible y sin mis datos personales, para que la vista sea rápida y segura.
- **H2:** Como operador, quiero navegar el panel por páginas y filtrar en el servidor, para trabajar con miles de incidencias sin que la interfaz se degrade.
- **H3:** Como consumidor del API, quiero que los listados actuales sigan funcionando igual si no indico paginación, para no romper integraciones ni pruebas existentes.

## Requisitos funcionales (criterios de aceptación en EARS)

### Respuesta ligera para el mapa (H1)

- RF-1: CUANDO el mapa solicite incidencias, EL SISTEMA devolverá únicamente los datos necesarios para representarlas: identificador, coordenadas, estado, prioridad, categoría, fecha de creación y un título corto.
- RF-2: EL SISTEMA no incluirá el correo electrónico del ciudadano, la descripción, los activos urbanos, el contexto urbano ni las asignaciones activas en la respuesta destinada al mapa.
- RF-3: SI una incidencia activa carece de coordenadas válidas, ENTONCES EL SISTEMA la omitirá de la respuesta del mapa.
- RF-4: EL SISTEMA mantendrá disponible la ficha completa de cada incidencia mediante la consulta de detalle existente.
- RF-5: CUANDO se solicite la respuesta ligera del mapa, EL SISTEMA no resolverá datos de activos, contexto urbano, asignaciones ni correo del ciudadano.

### Paginación aditiva (H2, H3)

- RF-6: CUANDO un cliente solicite un listado sin indicar paginación, EL SISTEMA devolverá la misma forma y el mismo contenido que antes de este cambio.
- RF-7: CUANDO un cliente indique número de página y tamaño, EL SISTEMA devolverá el contenido de esa página junto con el total de elementos, el total de páginas, la página devuelta y el tamaño aplicado.
- RF-8: SI el tamaño solicitado no está comprendido entre 1 y 100, ENTONCES EL SISTEMA rechazará la petición con un error de validación comprensible y no aplicará un valor silencioso.
- RF-9: SI la página solicitada está fuera del rango disponible, ENTONCES EL SISTEMA devolverá una página vacía con los metadatos correctos.
- RF-10: CUANDO un cliente combine criterios de búsqueda y paginación, EL SISTEMA aplicará ambos y devolverá el total de elementos que cumplen los criterios.
- RF-11: CUANDO un cliente realice una búsqueda sin indicar paginación, EL SISTEMA devolverá la misma forma y el mismo contenido que antes de este cambio.

### Panel de operador (H2)

- RF-12: CUANDO el operador abra el panel, EL SISTEMA mostrará 10 incidencias por página.
- RF-13: CUANDO el operador cambie de página, EL SISTEMA solicitará únicamente esa página al servidor y no recargará el conjunto completo.
- RF-14: CUANDO el operador aplique o modifique filtros, EL SISTEMA reiniciará la navegación en la primera página y consultará al servidor con los criterios aplicados.
- RF-15: MIENTRAS se cargue una página, EL SISTEMA mostrará y anunciará el estado de carga a las tecnologías de asistencia.
- RF-16: EL SISTEMA mostrará el total de resultados y la página actual sobre el total de páginas.
- RF-17: DONDE el panel permita mostrar más de 50 filas por página, EL SISTEMA mantendrá el desplazamiento fluido mediante representación virtual de filas.

### Eficiencia de consulta (H1, H2)

- RF-18: CUANDO el sistema recupere N incidencias activas para un listado, EL SISTEMA ejecutará un número de consultas a la base de datos independiente de N.

## Requisitos no funcionales

- NFR-1: Para 4.500 incidencias activas (5.000 totales), la respuesta ligera del mapa no superará 1 MB sin comprimir (aproximadamente 200 bytes por incidencia), frente a los 4,55 MiB medidos en el listado completo.
- NFR-2: En entorno local con 4.500 incidencias activas, la respuesta ligera del mapa tendrá un percentil 95 inferior a 1.000 ms, frente a la mediana de 9.851 ms medida en el listado completo `[NECESITA ACLARACIÓN: validar el umbral tras la primera medición]`.
- NFR-3: La respuesta ligera del mapa no contendrá datos personales; se verificará con una prueba automatizada que compruebe la ausencia del correo del ciudadano.
- NFR-4: Sin parámetros de paginación, los listados conservarán su contrato actual; se verificará con pruebas de contrato.
- NFR-5: Cada página de 10 elementos se resolverá con un número constante de consultas, con independencia del tamaño de la página.
- NFR-6: La paginación mantendrá el mismo orden de resultados entre páginas para un mismo conjunto de filtros y datos.

## Casos límite

- SI un listado filtrado no tiene resultados, ENTONCES EL SISTEMA devolverá una página vacía con total de elementos cero y el panel mostrará su estado vacío.
- SI el cliente solicita una página posterior a la última disponible, ENTONCES EL SISTEMA devolverá contenido vacío y metadatos coherentes.
- SI el cliente solicita tamaño cero, negativo o superior a 100, ENTONCES EL SISTEMA responderá con error de validación y no con un error interno.
- SI una incidencia se elimina o cambia de estado entre dos páginas, ENTONCES EL SISTEMA devolverá los metadatos de la consulta actual sin fallar.
- SI la base de datos no está disponible, ENTONCES EL SISTEMA devolverá un error controlado y el cliente mostrará un mensaje de reintento.
- SI el usuario no tiene permisos para un listado, ENTONCES EL SISTEMA mantendrá la respuesta de acceso denegado existente.
- SI la búsqueda no indica paginación, ENTONCES EL SISTEMA conservará la respuesta completa actual para no romper consumidores.

## Fuera de alcance

- Cambiar por defecto la forma de los endpoints actuales; la ruptura de contrato se pospone a una versión mayor.
- Operaciones de escritura, actualización de estado, adjuntos, comentarios, notificaciones y analítica.
- Rendimiento del DOM y agrupación de marcadores del mapa (Spec 012).
- Sistema visual, modo oscuro, foco, navegación móvil y accesibilidad (Spec 014).
- Caché distribuida, CDN, GraphQL, BFF o replicación de lectura.
- Migración o resiembra del conjunto de datos de 5.000 incidencias.
- Rediseño de la tabla del panel de operador más allá de la paginación y el indicador de carga.

## Criterios de aceptación

_Condiciones verificables que deben cumplirse para dar la funcionalidad por terminada._

- [ ] RF-1 y RF-2 verificados con una prueba que comprueba los campos devueltos y la ausencia del correo del ciudadano.
- [ ] RF-3 verificado con incidencias sin coordenadas válidas.
- [ ] RF-6 verificado con pruebas de contrato: la respuesta sin parámetros es idéntica a la anterior.
- [ ] RF-7 a RF-11 verificados con pruebas de paginación, página fuera de rango, tamaños inválidos y contrato retrocompatible sin parámetros.
- [ ] RF-12 a RF-17 verificados en el panel de operador con 5.000 incidencias.
- [ ] RF-18 verificado con el registro de consultas SQL: el número de consultas no crece con el número de incidencias.
- [ ] NFR-1 a NFR-6 medidos y registrados en `docs/rendimiento/experimento-4-baseline-frontend.md`.
- [ ] `mvn clean test`, `npm run lint` y `npm test` en verde, incluidas las pruebas de integración con PostgreSQL cuando la base de datos esté disponible.

## Dudas abiertas

- `[NECESITA ACLARACIÓN]` La búsqueda sin paginación se mantiene sin límite en esta entrega para preservar la retrocompatibilidad estricta (decisión propuesta en el ADR-12). ¿Se acepta ese riesgo o se pospone la acotación a una versión mayor?
- `[NECESITA ACLARACIÓN]` ¿La paginación debe ser base 0, alineada con el `PageResponse` actual de Spring, o base 1 de cara al usuario? Se propone base 0 en el API y presentación base 1 en la interfaz.
- `[NECESITA ACLARACIÓN]` ¿El título corto del mapa debe truncarse a un número fijo de caracteres o devolverse completo?
- `[NECESITA ACLARACIÓN]` ¿Se mantiene el endpoint ligero de mapa para roles internos con los mismos campos, o se necesita algún campo adicional (por ejemplo, departamento asignado)?
