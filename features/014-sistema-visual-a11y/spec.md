# Spec 014 — Sistema visual, navegación responsive y accesibilidad

**Estado:** Propuesta (pendiente de aprobación)

**Origen:** Issue #60 ([Perf]: Mejora visual y de optimización del Frontend) y auditoría `docs/issue-refactor-frontend.md` (hallazgos C7, C8 y área 4).

## Contexto y objetivo

La interfaz conserva restos de la plantilla inicial (idioma inglés, título genérico, estilos no utilizados y un color de acento ajeno a la paleta real), carece de un sistema de color común y de modo oscuro completo, elimina el indicador de foco en varios controles, no adapta el menú a pantallas pequeñas y reimplementa los modales con `div` y foco atrapado a mano. Esto produce fallos de accesibilidad WCAG 2.1 A/AA y una experiencia degradada en móvil, modo oscuro y navegación por teclado. El objetivo es unificar el sistema visual, hacer la navegación usable en móvil y llevar las vistas principales a un nivel de accesibilidad verificable, sin rediseñar la identidad de la aplicación.

## Usuarios / actores

- **Ciudadano:** usa la aplicación desde móvil y puede navegar con teclado o lector de pantalla.
- **Operador y técnico:** trabajan muchas horas en el panel y necesitan foco visible y contraste correcto.
- **Analista:** consulta vistas densas en escritorio y en modo oscuro.
- **Usuarios con preferencias del sistema:** modo oscuro, movimiento reducido y alto contraste.
- **Equipo de desarrollo:** necesita un sistema de color mantenible y no duplicado.

## Historias de usuario

- **H1:** Como usuario de lector de pantalla, quiero que la página declare su idioma real y que los controles tengan semántica correcta, para entender lo que se anuncia.
- **H2:** Como usuario de teclado, quiero ver siempre dónde está el foco y poder cerrar los modales con Escape, para navegar sin ratón.
- **H3:** Como usuario móvil, quiero un menú que no desborde y que me permita acceder a todas las secciones, para usar la aplicación desde el teléfono.
- **H4:** Como usuario con el sistema en modo oscuro, quiero que todos los textos y controles sean legibles, para no forzar la vista ni cambiar de tema.
- **H5:** Como usuario con movimiento reducido configurado, quiero que la interfaz no anime elementos innecesariamente, para evitar molestias.

## Requisitos funcionales (criterios de aceptación en EARS)

### Idioma, metadatos y limpieza de la plantilla (H1)

- RF-1: EL SISTEMA declarará el español como idioma del documento.
- RF-2: EL SISTEMA mostrará un título descriptivo de la aplicación y una descripción para motores de búsqueda y marcadores.
- RF-3: EL SISTEMA no aplicará estilos de plantilla no utilizados ni restricciones globales de ancho, alineación o borde que obliguen a cada vista a corregirlos.
- RF-4: EL SISTEMA no mostrará controles ni secciones de la plantilla original que no formen parte de la aplicación.

### Color y contraste (H4)

- RF-5: EL SISTEMA usará los mismos valores de color para el mismo significado en todas las vistas, sin duplicar la paleta con valores literales dispersos.
- RF-6: MIENTRAS el sistema operativo esté configurado en modo oscuro, EL SISTEMA adaptará fondos, textos, bordes y controles en todas las vistas, incluidos el menú, los listados, el panel de operador, los filtros, las notificaciones y los comentarios.
- RF-7: EL SISTEMA garantizará un contraste mínimo de 4,5:1 para texto normal y de 3:1 para texto grande y componentes de interfaz, tanto en modo claro como oscuro.
- RF-8: EL SISTEMA conservará el contraste en los estados normal, hover, foco, activo y deshabilitado.

### Foco y teclado (H2)

- RF-9: CUANDO un usuario navegue con teclado, EL SISTEMA mostrará un indicador de foco visible de al menos 2 píxeles y con contraste mínimo de 3:1.
- RF-10: EL SISTEMA no eliminará el indicador de foco de ningún control interactivo sin ofrecer un sustituto visible.

### Navegación (H3)

- RF-11: CUANDO el ancho de la ventana esté entre 320 y 767 píxeles, EL SISTEMA ofrecerá un menú de navegación colapsable que no produzca desbordamiento horizontal.
- RF-12: CUANDO el menú colapsable se abra o se cierre, EL SISTEMA expondrá su estado expandido o contraído a las tecnologías de asistencia.
- RF-13: EL SISTEMA marcará el enlace de la sección activa como página actual para las tecnologías de asistencia.
- RF-14: EL SISTEMA distinguirá la sección activa por al menos dos medios visuales, no únicamente por color.

### Modales (H2)

- RF-15: CUANDO un modal esté abierto, EL SISTEMA mantendrá el foco dentro de él e impedirá la interacción con el contenido de fondo.
- RF-16: CUANDO el usuario pulse Escape, EL SISTEMA cerrará el modal y devolverá el foco al elemento que lo abrió.
- RF-17: CUANDO un modal se cierre, EL SISTEMA restaurará el foco sin desplazar la página.

### Semántica de filtros (H1)

- RF-18: EL SISTEMA agrupará los filtros de incidencias en una región con nombre accesible y significado semántico.
- RF-19: EL SISTEMA no anunciará dos veces la misma etiqueta en un control de filtro.
- RF-20: CUANDO una búsqueda no devuelva resultados, EL SISTEMA anunciará el estado vacío a las tecnologías de asistencia.

### Movimiento y renderizado (H5)

- RF-21: MIENTRAS la preferencia de movimiento reducido esté activa, EL SISTEMA desactivará las animaciones y transiciones no esenciales.
- RF-22: EL SISTEMA aplazará el renderizado de las secciones bajo el pliegue en Inicio y Analítica sin cambiar su apariencia final.

## Requisitos no funcionales

- NFR-1: Las vistas principales cumplirán WCAG 2.1 nivel AA en idioma, foco visible, contraste, nombre, rol y valor.
- NFR-2: A 320 píxeles de ancho, ninguna vista principal presentará desplazamiento horizontal de página.
- NFR-3: Con zoom de navegador al 200 %, las vistas principales seguirán siendo operables sin pérdida de contenido.
- NFR-4: La auditoría automática de accesibilidad de las cinco rutas principales no reportará incidencias críticas ni graves `[NECESITA ACLARACIÓN: definir herramienta y umbral, por ejemplo Lighthouse ≥ 95]`.
- NFR-5: No habrá regresiones visuales en modo claro ni oscuro a 1440, 768 y 390 píxeles de ancho.
- NFR-6: Los cambios de color no alterarán la identidad visual actual más allá de unificar la paleta y completar el modo oscuro.

## Casos límite

- SI el usuario tiene activado el modo oscuro y el modo de alto contraste, ENTONCES EL SISTEMA mantendrá textos y controles legibles.
- SI el usuario navega solo con teclado y abre un modal, ENTONCES EL SISTEMA no permitirá que el foco escape al fondo.
- SI se abre un modal desde otro modal, ENTONCES EL SISTEMA cerrará o apilará el anterior sin dejar el foco perdido.
- SI el usuario reduce el ancho hasta 320 píxeles con el menú abierto, ENTONCES EL SISTEMA mantendrá el menú usable y sin desbordamiento.
- SI el usuario tiene movimiento reducido y se muestra un indicador de carga, ENTONCES EL SISTEMA conservará el estado de carga sin animación.
- SI un control nativo se muestra en modo oscuro, ENTONCES EL SISTEMA evitará combinaciones de fondo y texto ilegibles.
- SI una sección bajo el pliegue se aplaza, ENTONCES EL SISTEMA la mostrará igualmente al hacer scroll o al imprimir.

## Fuera de alcance

- Rediseño de marca, logotipo, tipografía corporativa o nuevos componentes visuales no existentes.
- Sustitución completa de los emojis por un set de iconos propio; solo se exige que los decorativos queden ocultos a tecnologías de asistencia.
- Internacionalización a idiomas distintos del español.
- Cambios en backend, contratos de API o base de datos.
- Rendimiento del DOM, agrupación de marcadores y carga inicial del mapa (Spec 012).
- Paginación y contrato de listados (Spec 013).
- Modo oscuro de vistas que ya lo tengan resuelto, salvo la unificación de tokens.
- Aplicación móvil nativa.

## Criterios de aceptación

_Condiciones verificables que deben cumplirse para dar la funcionalidad por terminada._

- [ ] RF-1 y RF-2 verificados en el documento servido: idioma español, título y descripción descriptivos.
- [ ] RF-3 y RF-4 verificados con la eliminación de estilos y elementos de plantilla no utilizados.
- [ ] RF-5 a RF-8 verificados con una revisión de contraste en modo claro y oscuro de las cinco rutas principales.
- [ ] RF-9 y RF-10 verificados recorriendo con teclado todos los controles interactivos.
- [ ] RF-11 a RF-14 verificados a 320, 390 y 768 píxeles, con menú colapsable y enlace activo anunciado.
- [ ] RF-15 a RF-17 verificados en todos los modales: Escape, foco atrapado, fondo inerte y retorno de foco.
- [ ] RF-18 a RF-20 verificados con lector de pantalla o inspección del árbol de accesibilidad.
- [ ] RF-21 y RF-22 verificados con `prefers-reduced-motion` activo y con el aplazamiento de secciones.
- [ ] NFR-1 a NFR-6 verificados y registrados; sin regresiones visuales en las capturas de referencia.
- [ ] `npm run lint` y `npm test` en verde, incluidas pruebas de accesibilidad de los componentes modificados.

## Dudas abiertas

- `[NECESITA ACLARACIÓN]` ¿Qué herramienta y umbral se usan como puerta de accesibilidad (Lighthouse, axe, revisión manual) y a partir de qué puntuación se considera aceptada?
- `[NECESITA ACLARACIÓN]` ¿Se adopta un único breakpoint compartido en 768 píxeles o se mantienen los breakpoints actuales de 640 y 768?
- `[NECESITA ACLARACIÓN]` ¿Se pospone definitivamente la sustitución de emojis por iconos propios o se incluye en esta iteración?
- `[NECESITA ACLARACIÓN]` ¿La limpieza del color de acento violeta implica cambiar también el marcador temporal de ubicación seleccionada, que hoy lo usa?
- `[NECESITA ACLARACIÓN]` ¿El aplazamiento de renderizado debe aplicarse también a las tablas largas del panel de operador o solo a Inicio y Analítica?
